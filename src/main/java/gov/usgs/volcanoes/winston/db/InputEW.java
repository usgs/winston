package gov.usgs.volcanoes.winston.db;

import gov.usgs.earthworm.message.TraceBuf;
import gov.usgs.util.CurrentTime;
import gov.usgs.util.Time;
import gov.usgs.util.Util;

import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.SortedMap;
import java.util.SortedSet;
import java.util.TimeZone;
import java.util.TreeMap;
import java.util.TreeSet;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.zip.Deflater;

import org.apache.commons.collections.map.LRUMap;
import org.apache.commons.collections.set.MapBackedSet;

/**
 * TraceBuf input functions for use by ImportEW.
 * 
 * 
 * @author Dan Cervelli
 * @author Joshua Doubleday
 */
public class InputEW {
	public static class InputResult {
		public enum Code {
			NO_CODE, ERROR_INPUT, ERROR_NULL_TRACEBUF, ERROR_DUPLICATE, ERROR_UNKNOWN, ERROR_DATABASE, ERROR_NO_WINSTON, ERROR_CHANNEL, ERROR_TIME_SPAN, ERROR_HELICORDER, SUCCESS, SUCCESS_CREATED_TABLE, SUCCESS_HELICORDER, SUCCESS_TIME_SPAN
		}

		public Code code;
		public TraceBuf traceBuf;
		public double failedHeliJ2K;

		public InputResult(Code c, TraceBuf tb) {
			code = c;
			traceBuf = tb;
		}
	}

	private static class HeliFields {
	    public static final int J2KSEC = 0;
	    public static final int SMIN = 1;
	    public static final int SMAX = 2;
	    public static final int RCNT = 3;
	    public static final int RSAM = 4;
	    public static final int MEAN = 5;
	    public static final int MU = 6; // DC offset
	    public static final int WEIGHTED_RSAM = 7; // DC offset removed
	}
	
	private WinstonDatabase winston;
	private DateFormat dateFormat;

	private Logger logger;

	/*
	 * These two static fields are for optimization purposes. They are static
	 * and synchronized so multiple instances of this class can modify them
	 * simultaneously.
	 */
	private static Set<String> checkTableCache;
	private static Map<String, double[]> channelTimeSpans;

	// TODO: make this synchronized too?
	private Map<String, SortedMap<Double, double[]>> channelHelicorderRows;
	private Map<String, Integer> channelSid;

	private int maxRows = 300;
	private int numRowsToDelete = 60;
	private boolean enableValarmView = false;

	/**
	 * Constructs a new Input2.
	 * 
	 * @param w
	 */
	@SuppressWarnings("unchecked")
	public InputEW(WinstonDatabase w) {
		setWinston(w);
		checkTableCache = MapBackedSet.decorate(Collections
				.synchronizedMap(new LRUMap(w.cacheCap, true)));

		channelTimeSpans = Collections
				.synchronizedMap(new HashMap<String, double[]>());
		channelHelicorderRows = new HashMap<String, SortedMap<Double, double[]>>();
		channelSid = new HashMap<String, Integer>();
		dateFormat = new SimpleDateFormat("yyyy_MM_dd");
		dateFormat.setTimeZone(TimeZone.getTimeZone("GMT"));
	}

	/**
	 * Set the winston database for this inputter.
	 * 
	 * @param db
	 *            the winston database;
	 */
	public void setWinston(WinstonDatabase db) {
		winston = db;
		logger = winston.getLogger();
	}

	public void setRowParameters(int mr, int nd) {
		maxRows = mr;
		numRowsToDelete = nd;
	}

	private List<String> getDayTables(String code) {
		ArrayList<String> list = new ArrayList<String>(10);
		try {
			ResultSet rs = winston.getStatement().executeQuery("SHOW TABLES");
			while (rs.next())
				list.add(rs.getString(1));
			rs.close();

			Collections.sort(list);

			ArrayList<String> dayList = new ArrayList<String>(list.size() / 2);
			for (String table : list) {
				String day = table.substring(table.indexOf("$$") + 2);
				if (day.length() == 10 && day.charAt(4) == '_'
						&& day.charAt(7) == '_'
						&& Character.isDigit(day.charAt(0))
						&& Character.isDigit(day.charAt(9)))
					dayList.add(table);
			}
			return dayList;
		} catch (Exception e) {
			logger.log(Level.SEVERE,
					"Could not get list of tables for channel: " + code, e);
		}
		return null;
	}

	public void purgeTables(String channel, int days) {
		if (days <= 0)
			return;

		if (!winston.checkConnect())
			return;

		if (!winston.useDatabase(channel))
			return;

		List<String> list = getDayTables(channel);
		if (list == null)
			return;

		Date now = CurrentTime.getInstance().nowDate();
		Date then = new Date(now.getTime() - ((long) days * 86400000L));
		String thenString = Time.format(
				WinstonDatabase.WINSTON_TABLE_DATE_FORMAT, then);

		winston.getLogger().info(
				"Purging '" + channel + "' tables before: " + thenString);

		boolean deleted = false;
		boolean setTime = false;
		for (String table : list) {
			String ss[] = table.split("\\$\\$");

			if (thenString.compareTo(ss[1]) > 0) {
				try {
					checkTableCache.remove(table);
					winston.getStatement()
							.execute("DROP TABLE `" + table + "`");
					winston.getStatement().execute(
							"DROP TABLE `" + ss[0] + "$$H" + ss[1] + "`");

					deleted = true;
					winston.getLogger().info("Deleted table: " + table);
				} catch (Exception e) {
					winston.getLogger().severe(
							"Could not drop old table: " + channel
									+ ".  Are permissions set properly?");
				}
			} else {
				if (deleted) {
					try {
						String nextLowestTable = table;
						ResultSet rs = winston.getStatement()
								.executeQuery(
										"SELECT MIN(st) FROM `"
												+ nextLowestTable + "`");
						rs.next();
						double t1 = rs.getDouble(1);
						setTimeSpan(channel, t1, Double.NaN);
						rs.close();
						setTime = true;
					} catch (Exception e) {
						winston.getLogger().severe(
								"Could not update span after dropping table: "
										+ channel);
					}
				}
				break;
			}
		}
		if (deleted && !setTime) {
			// must have deleted all of the tables, just delete the channel
			// entirely
			winston.getLogger()
					.info("Permanently deleting channel: " + channel);
			new Admin(winston).deleteChannel(channel);
		}
	}

	/**
	 * Creates a day table.
	 * 
	 * TODO: fix view out of order data bug
	 * 
	 * @param code
	 *            the code of the new table
	 * @param date
	 *            the date of the new table
	 * @return success/failure indication
	 */
	private boolean createDayTable(String code, String date) {
		try {
			double prevDayJ2k = Util.dateToJ2K(dateFormat.parse(date))
					- (24 * 3600);
			String prevDate = dateFormat.format(Util.j2KToDate(prevDayJ2k));

			String waveTable = code + "$$" + date;
			String heliTable = code + "$$H" + date;
			String waveTableLast = code + "$$" + prevDate;
			String heliTableLast = code + "$$H" + prevDate;
			String waveTableall = code + "$$" + "past2days";
			String heliTableall = code + "$$H" + "past2days";

			winston.getStatement()
					.execute(
							"CREATE TABLE `"
									+ waveTable
									+ "` (st DOUBLE PRIMARY KEY, et DOUBLE, sr DOUBLE, "
									+ "datatype CHAR(3), tracebuf BLOB) " + winston.tableEngine);
			
System.out.println(                         "CREATE TABLE `"
                                    + waveTable
                                    + "` (st DOUBLE PRIMARY KEY, et DOUBLE, sr DOUBLE, "
                                    + "datatype CHAR(3), tracebuf BLOB) " + winston.tableEngine);

			winston.getStatement()
					.execute(
							"CREATE TABLE `"
									+ heliTable
									+ "` (j2ksec DOUBLE PRIMARY KEY, smin INT, smax INT, "
									+ "rcnt INT, rsam DOUBLE) " + winston.tableEngine);

			if (enableValarmView) {
				// if there is data from the previous day, we want to union it
				// into our
				// view, otherwise, setup views into current days data
				winston.getLogger().log(Level.INFO,
						"Creating VIEWs for VAlarm: " + heliTableall);

				String sql = "CREATE or REPLACE VIEW `" + waveTableall
						+ "` AS SELECT * FROM `" + waveTable + "`";
				if (tableExists(waveTableLast))
					sql += " UNION ALL select * from `" + waveTableLast + "`";
				winston.getStatement().execute(sql);

				sql = "CREATE or REPLACE VIEW `" + heliTableall
						+ "` AS SELECT * FROM `" + heliTable + "`";
				if (tableExists(heliTableLast))
					sql += " UNION ALL select * from `" + heliTableLast + "`";
				winston.getStatement().execute(sql);
			}

			return true;
		} catch (Exception ex) {
			winston.getLogger().log(Level.SEVERE,
					"Could not create day table: '" + code + "$" + date + "'.",
					ex);
		}
		return false;
	}

	/**
	 * Checks if a table exists.
	 * 
	 * @param code
	 *            the code to check
	 * @param date
	 *            the date to check
	 * @return indicator of table existence
	 */
	private boolean tableExists(String code, String date) {
		return tableExists(code + "$$" + date);
	}

	/*
	 * Checks if a table exists.
	 * 
	 * @param code the code to check
	 * 
	 * @param date the date to check
	 * 
	 * @return indicator of table existence
	 */
	private boolean tableExists(String table) {

		if (checkTableCache.contains(table))
			return true;

		try {
			ResultSet rs = winston.getStatement().executeQuery(
					"SHOW TABLES LIKE '" + table + "'");
			boolean result = rs.next();
			if (result) {
				checkTableCache.add(table);
				rs.close();
			}
			return result;
		} catch (Exception e) {
		}
		return false;
	}

	/**
	 * Updates the time span of a channel to include a given start and end time.
	 * 
	 * TODO: preparedStatements
	 * 
	 * @param channel
	 *            the channel
	 * @param st
	 *            the start time
	 * @param et
	 *            the end time
	 */
	private void setTimeSpan(String channel, double st, double et)
			throws SQLException {
		double[] d = channelTimeSpans.get(channel);
		if (!Double.isNaN(st)) {
			if (d != null)
				d[0] = st;
			winston.getStatement().execute(
					"UPDATE `" + winston.databasePrefix
							+ "_ROOT`.channels SET st=" + st + " WHERE code='"
							+ channel + "'");
		}
		if (!Double.isNaN(et)) {
			if (d != null)
				d[1] = et;
			winston.getStatement().execute(
					"UPDATE `" + winston.databasePrefix
							+ "_ROOT`.channels SET et=" + et + " WHERE code='"
							+ channel + "'");
		}
	}

	/**
	 * Gets the current time span of the channel. Supercedes the version in
	 * Data.java for optimization reasons.
	 * 
	 * @param channel
	 * @return
	 */
	private double[] getTimeSpan(String channel) {
		double[] d = channelTimeSpans.get(channel);
		if (d != null)
			return d;

		try {
			ResultSet rs = winston.getStatement().executeQuery(
					"SELECT st, et FROM `" + winston.databasePrefix
							+ "_ROOT`.channels WHERE code='" + channel + "'");
			d = new double[] { Double.NaN, Double.NaN };
			if (rs.next()) {
				d[0] = rs.getDouble(1);
				d[1] = rs.getDouble(2);
			}
			rs.close();
			channelTimeSpans.put(channel, d);
			return d;
		} catch (Exception e) {
			winston.getLogger().log(Level.SEVERE,
					"Could not get time span for channel: " + channel, e);// Util.getLineNumber(this,
																			// e));
		}
		return null;
	}

	/**
	 * Gets a helicorder row. This function MUST be called before
	 * updateHelicorderRow because it is responsible for creating the blank row
	 * if no existing data can be found.
	 * 
	 * @param table
	 * @param j2ksec
	 * @return
	 */
	private double[] getHelicorderRow(String channel, double j2ksec,
			boolean useDB) {
		SortedMap<Double, double[]> rows = channelHelicorderRows.get(channel);
		if (rows == null) {
			rows = new TreeMap<Double, double[]>();
			channelHelicorderRows.put(channel, rows);
		}
		double[] d = rows.get(j2ksec);
		if (d != null)
			return d;

		String date = dateFormat.format(Util.j2KToDate(j2ksec));
		String table = channel + "$$H" + date;

		if (useDB) {
			try {
				ResultSet rs = winston.getStatement().executeQuery(
						"SELECT j2ksec, smin, smax, rcnt, rsam FROM `" + table
								+ "` WHERE j2ksec=" + j2ksec);
				if (rs.next())
					d = new double[] { rs.getDouble(1), rs.getDouble(2),
							rs.getDouble(3), rs.getDouble(4), rs.getDouble(5),
							0, 0, 0 };
				rs.close();
			} catch (Exception e) {
				logger.warning("Could not get helicorder row: "
						+ e.getMessage());
			}
		}

		if (d == null)
			d = new double[] { j2ksec, Integer.MAX_VALUE, Integer.MIN_VALUE, 0,
					0, 0, 0, 0 };

		rows.put(j2ksec, d);
		if (rows.size() > maxRows) {
			for (int i = 0; i < numRowsToDelete; i++)
				rows.remove(rows.firstKey());
		}
		return d;
	}

	private double getRSAMMu(String channel, double j2ksec, int delta,
			int duration) {
		SortedMap<Double, double[]> rows = channelHelicorderRows.get(channel);
		if (rows == null)
			return Double.NaN;

		delta = -delta;
		double sampleSum = 0;
		double sampleCount = 0;
		for (int k = delta - duration; k < delta; k++) {
			double[] hr = rows.get(j2ksec + k);
			if (hr != null) {
				sampleSum += hr[HeliFields.MEAN] * hr[HeliFields.RCNT];
				sampleCount += hr[HeliFields.RCNT];
			}
		}

		if (sampleCount != 0)
			return sampleSum / sampleCount;
		else
			return 0;
	}

	private PreparedStatement getInputStatement(String table, TraceBuf tb) {
		try {
			PreparedStatement insert = winston
					.getPreparedStatement("INSERT INTO `" + table
							+ "` VALUES (?,?,?,?,?);");

			if (insert == null)
				logger.severe("Just got a null ps");
			else if (tb == null)
				logger.severe("null tb");
			insert.setDouble(1, tb.getStartTimeJ2K());
			insert.setDouble(2, tb.getEndTimeJ2K());
			insert.setDouble(3, tb.samplingRate());
			insert.setString(4, tb.dataType());
			byte[] compressed = Util.compress(tb.bytes, Deflater.BEST_SPEED, 0,
					tb.bytes.length - 1);
			insert.setBytes(5, compressed);
			return insert;
		} catch (Exception e) {
			logger.log(Level.SEVERE, "Could not create prepared statement: "
					+ tb, e);
		}
		return null;
	}

	/**
	 * Updates a helicorder row.
	 * 
	 * @param channel
	 * @param date
	 * @param tb
	 * @throws SQLException
	 */
	private void updateHelicorderData(Set<Double> modifiedRows, String channel,
			String date, TraceBuf tb, boolean computeRsam, int delta,
			int duration, boolean useDB) // throws SQLException
	{
		// TODO: optimize/simplify
		double fst = Math.floor(tb.getStartTimeJ2K());
		double cet = Math.ceil(tb.getEndTimeJ2K());
		double[][] heliList = new double[((int) Math.round(cet - fst)) + 1][];
		int j = 0;
		for (int i = (int) Math.round(fst); i <= (int) Math.round(cet); i++) {
			modifiedRows.add((double) i);
			heliList[j] = getHelicorderRow(channel, i, useDB);
			if (computeRsam) {
				double mu = getRSAMMu(channel, i, delta, duration);
				heliList[j][HeliFields.MU] = mu;
			}
			j++;
		}
		double st = tb.getStartTimeJ2K();
		double dt = 1 / tb.samplingRate();

		for (int i = 0; i < tb.numSamples(); i++) {
			j = (int) (Math.floor(st) - fst);
			double[] d = heliList[j];
			int sample = tb.samples()[i];
			d[HeliFields.SMIN] = Math.min(d[HeliFields.SMIN], sample);
			d[HeliFields.SMAX] = Math.max(d[HeliFields.SMAX], sample);
			if (computeRsam) {
				// original RSAM
				d[HeliFields.RSAM] = (d[HeliFields.RSAM] * d[HeliFields.RCNT] + Math.abs(sample)) / (d[HeliFields.RCNT] + 1);
				// average of samples
				d[HeliFields.MEAN] = (d[HeliFields.MEAN] * d[HeliFields.RCNT] + sample) / (d[HeliFields.RCNT] + 1);
				d[HeliFields.WEIGHTED_RSAM] = (d[HeliFields.WEIGHTED_RSAM] * d[HeliFields.RCNT] + Math.abs(sample - d[HeliFields.MU])) / (d[HeliFields.RCNT] + 1);
				d[HeliFields.RCNT]++;
			}
			st += dt;
		}
	}

	/**
	 * Writes helicorder data to the database. In case of failure, returns the
	 * j2k that failed. Returns NaN on success.
	 * 
	 * @param channel
	 * @param modifiedRows
	 * @return
	 */
	private double writeHelicorderData(String channel, Set<Double> modifiedRows) // throws
																					// SQLException
	{
		for (double j2k : modifiedRows) {
			String date = dateFormat.format(Util.j2KToDate(j2k));
			String table = channel + "$$H" + date;

			double[] row = getHelicorderRow(channel, j2k, false);
			String sql = String
					.format("INSERT INTO `%s` (j2ksec, smin, smax, rcnt, rsam) "
							+ "VALUES (%f,%d,%d,%d,%f) ON DUPLICATE KEY UPDATE "
							+ "smin=VALUES(smin), smax=VALUES(smax), rcnt=VALUES(rcnt), rsam=VALUES(rsam)",
							table, j2k, (int) row[HeliFields.SMIN], (int) row[HeliFields.SMAX],
							(int) row[HeliFields.RCNT], row[HeliFields.WEIGHTED_RSAM]);
			try {
				winston.getStatement().execute(sql);
			} catch (SQLException ex) {
				logger.warning("Could not write helicorder row: "
						+ ex.getMessage());
				logger.warning("SQL: " + sql);
				return j2k;
			}
		}

		return Double.NaN;
	}

	public boolean rederive(List<TraceBuf> tbs, boolean computeRsam, int delta,
			int duration) {
		if (tbs == null || tbs.size() == 0)
			return false;

		if (!winston.checkConnect())
			return false;

		String channel = tbs.get(0).toWinstonString();

		if (!winston.useDatabase(channel))
			return false;

		SortedSet<Double> modifiedHeliRows = new TreeSet<Double>();
		Iterator<TraceBuf> it = tbs.iterator();
		while (it.hasNext()) {
			TraceBuf tb = it.next();
			if (tb == null)
				continue;

			if (!tb.toWinstonString().equals(channel))
				continue;

			double ts = tb.getStartTimeJ2K();
			String date = dateFormat.format(Util.j2KToDate(ts));

			updateHelicorderData(modifiedHeliRows, channel, date, tb,
					computeRsam, delta, duration, false);
		}

		return Double.isNaN(writeHelicorderData(channel, modifiedHeliRows));
	}

	private List<InputResult> getError(InputResult.Code code) {
		ArrayList<InputResult> list = new ArrayList<InputResult>(1);
		list.add(new InputEW.InputResult(code, null));
		return list;
	}

	/**
	 * Inserts multiple TraceBufs for a single channel into the database. This
	 * uses optimizations that require that all of the TraceBufs be from the
	 * same channel. Using this function with different channels will produce
	 * unstable results.
	 * 
	 * This function returns either null if the input is null or of size 0, if
	 * the Winston connection can not be established, if the station time span
	 * can be calculated (bad database), or the channel's database can not be
	 * used (again, bad database). Otherwise it returns a list of InputResults
	 * for each TraceBuf and one that specifies the result of the time span
	 * update.
	 * 
	 * @param tbs
	 *            the list of TraceBufs to insert
	 * @return the result or null (see above)
	 */
	public List<InputResult> inputTraceBufs(List<TraceBuf> tbs,
			boolean computeRsam, int delta, int duration) {
		if (tbs == null || tbs.size() == 0)
			return getError(InputResult.Code.ERROR_INPUT);

		if (!winston.checkConnect())
			return getError(InputResult.Code.ERROR_NO_WINSTON);

		String channel = tbs.get(0).toWinstonString();
		double[] span = getTimeSpan(channel);
		if (span == null)
			return getError(InputResult.Code.ERROR_TIME_SPAN);

		double stBefore = span[0];

		if (!winston.useDatabase(channel))
			return getError(InputResult.Code.ERROR_DATABASE);

		ArrayList<InputResult> results = new ArrayList<InputResult>(
				tbs.size() + 1);

		SortedSet<Double> modifiedHeliRows = new TreeSet<Double>();

		Iterator<TraceBuf> it = tbs.iterator();
		while (it.hasNext()) {
			boolean tableCreated = false;
			TraceBuf tb = it.next();
			InputResult result = new InputResult(InputResult.Code.NO_CODE, tb);
			if (tb == null)
				result.code = InputResult.Code.ERROR_NULL_TRACEBUF;

			if (!tb.toWinstonString().equals(channel))
				result.code = InputResult.Code.ERROR_CHANNEL;

			if (result.code != InputResult.Code.NO_CODE)
				continue;

			double ts = tb.getStartTimeJ2K();
			String date = dateFormat.format(Util.j2KToDate(ts));
			String endDate = dateFormat.format(Util.j2KToDate(tb
					.getEndTimeJ2K() + 1));
			String table = channel + "$$" + date;

			try {
				if (!tableExists(channel, date)) {
					createDayTable(channel, date);
					tableCreated = true;
				}
				if (!tableExists(channel, endDate)) {
					createDayTable(channel, endDate);
					tableCreated = true;
				}

				PreparedStatement insert = getInputStatement(table, tb);

				try {
					insert.executeUpdate();
				} catch (SQLException ex) {
					if (ex.getMessage().startsWith("Duplicate entry"))
						result.code = InputResult.Code.ERROR_DUPLICATE;
					else
						throw ex;
				}

				span[0] = Math.min(span[0], tb.getStartTimeJ2K());
				span[1] = Math.max(span[1], tb.getEndTimeJ2K());
		
				if (tb.samplingRate() > 2 && result.code != InputResult.Code.ERROR_DUPLICATE)
					updateHelicorderData(modifiedHeliRows, channel, date, tb,
							computeRsam, delta, duration, true);
			} catch (SQLException ex) {
				result.code = InputResult.Code.ERROR_DATABASE;
				logger.log(Level.SEVERE, "Could not insert trace buf: ", ex);
			}

			if (result.code == InputResult.Code.NO_CODE) {
				if (tableCreated)
					result.code = InputResult.Code.SUCCESS_CREATED_TABLE;
				else
					result.code = InputResult.Code.SUCCESS;
			}

			results.add(result);
			tableCreated = false;
		}

		InputResult heliResult = new InputResult(
				InputResult.Code.SUCCESS_HELICORDER, null);
		double failed = writeHelicorderData(channel, modifiedHeliRows);
		if (!Double.isNaN(failed)) {
			heliResult.code = InputResult.Code.ERROR_HELICORDER;
			heliResult.failedHeliJ2K = failed;
		}

		results.add(heliResult);

		InputResult spanResult = new InputResult(
				InputResult.Code.ERROR_TIME_SPAN, null);
		try {
			if (span[0] == stBefore)
				setTimeSpan(channel, Double.NaN, span[1]);
			else
				setTimeSpan(channel, span[0], span[1]);
			spanResult.code = InputResult.Code.SUCCESS_TIME_SPAN;
		} catch (SQLException ex) {
			logger.log(Level.SEVERE, "Could not set time span for channel: "
					+ channel, ex);
		}
		results.add(spanResult);

		return results;
	}

	private int getSid(String c) throws Exception {
		winston.useRootDatabase();
		if (!channelSid.containsKey(c)) {
			ResultSet rs = winston.getStatement().executeQuery(
					"SELECT sid FROM `" + winston.databasePrefix
							+ "_ROOT`.channels WHERE code='" + c + "'");
			if (rs.next()) {
				int sid = rs.getInt(1);
				channelSid.put(c, sid);
			}
		}

		return channelSid.get(c);
	}

	/**
	 * Inserts multiple metadata updates
	 * 
	 * This function returns either null if the input is null or of size 0, if
	 * the Winston connection can not be established, or the channelmetadata
	 * database can not be used (again, bad database). Otherwise it returns a
	 * list of InputResults for each entry.
	 * 
	 * @param channel
	 * @param entries
	 * @return the result or null (see above)
	 */
	public void inputMetadata(String channel, Map<String, String> m) {

		if (!winston.checkConnect() || !winston.useRootDatabase())
			logger.severe("Can't update metadata: Can't connect to Winston");
		else {
			try {
				int sid = getSid(channel);

				PreparedStatement ps = winston
						.getPreparedStatement("REPLACE INTO channelmetadata (sid, name, value) VALUES (?,?,?);");
				Iterator<String> it = m.keySet().iterator();
				while (it.hasNext()) {
					String name = it.next();
					ps.setInt(1, sid);
					ps.setString(2, name);
					ps.setString(3, m.get(name));

					ps.executeUpdate();
					logger.severe(String.format(
							"Metadata updated for %s: %s=%s", channel, name,
							m.get(name)));
				}

				m.clear();
			} catch (Exception e) {
				logger.severe("Can't update metadata: " + e.getMessage());
				e.printStackTrace();
			}
		}
	}

	public void setEnableValarmView(boolean enableValarmView) {
		this.enableValarmView = enableValarmView;

	}

}
