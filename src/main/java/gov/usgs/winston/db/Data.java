package gov.usgs.winston.db;

import gov.usgs.earthworm.message.TraceBuf;
import gov.usgs.math.DownsamplingType;
import gov.usgs.plot.data.HelicorderData;
import gov.usgs.plot.data.RSAMData;
import gov.usgs.plot.data.Wave;
import gov.usgs.util.Time;
import gov.usgs.util.Util;
import gov.usgs.util.UtilException;

import java.io.IOException;
import java.sql.Blob;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.List;
import java.util.TimeZone;
import java.util.logging.Level;

/**
 * A class to handle data acquistion from a Winston database.
 * 
 * @author Dan Cervelli
 */
public class Data {
	private static final int ONE_DAY = 60 * 60 * 24;
	private final WinstonDatabase winston;
	private final Channels channels;
	private final DateFormat dateFormat;
	private String vdxName;

	/**
	 * Constructor
	 * 
	 * @param w
	 *            WinstonDatabase
	 */
	public Data(WinstonDatabase w) {
		winston = w;
		vdxName = "";
		channels = new Channels(w);
		dateFormat = new SimpleDateFormat("yyyy_MM_dd");
		dateFormat.setTimeZone(TimeZone.getTimeZone("GMT"));
	}

	/**
	 * Get timespan for specific channel
	 * 
	 * @param code
	 *            channel code
	 * @return Array of start and end times
	 */
	public double[] getTimeSpan(String code) {
		if (!winston.checkConnect())
			return null;
		try {
			ResultSet rs = winston.getStatement().executeQuery(
					"SELECT st, et FROM `" + winston.databasePrefix + "_ROOT`.channels WHERE code='" + code + "'");
			rs.next();
			double[] d = new double[] { rs.getDouble(1), rs.getDouble(2) };
			rs.close();
			return d;
		} catch (Exception e) {
			winston.getLogger().log(Level.SEVERE, "Could not get time span for channel: " + code, e);// Util.getLineNumber(this,
																										// e));
		}
		return null;
	}

	/**
	 * Get timespan for specific channel
	 * 
	 * @param sid
	 *            channel id
	 * @return Array of start and end times
	 */
	public double[] getTimeSpan(int sid) {
		String code = channels.getChannelCode(sid);
		return getTimeSpan(code);
	}

	/**
	 * Get days between times t1 & t2
	 * 
	 * @param t1
	 *            start of timespan
	 * @param t2
	 *            end of timespan
	 * @return List of strings representing each day between t1 & t2
	 */
	private List<String> daysBetween(double t1, double t2) {
		ArrayList<String> result = new ArrayList<String>();
		double ct = t1;
		while (ct < t2 + ONE_DAY) {
			result.add(Time.format(WinstonDatabase.WINSTON_TABLE_DATE_FORMAT, ct));
			ct += ONE_DAY;
		}
		return result;
	}

	/**
	 * Finds data gaps in a given channel between two times. Returns null
	 * on a Winston error. Returns a single item list with the given time span
	 * if the channel doesn't exist or if no data exist in the interval.
	 * 
	 * @param code
	 * @param t1
	 * @param t2
	 * @return List of data gaps (each gap by a start and end time)
	 */
	public List<double[]> findGaps(String code, double t1, double t2) {
		if (!winston.checkConnect())
			return null;

		List<double[]> gaps = new ArrayList<double[]>();
		if (!winston.useDatabase(code)) {
			// database didn't exist so the whole thing must be a gap
			gaps.add(new double[] { t1, t2 });
			return gaps;
		}

		try {
			SimpleDateFormat dateFormat = new SimpleDateFormat("yyyy_MM_dd");

			List<String> days = daysBetween(t1, t2);

			dateFormat.setTimeZone(TimeZone.getTimeZone("GMT"));
			List<double[]> bufs = new ArrayList<double[]>(2 * ONE_DAY);
			for (String day : days) {
				double tst = Time.parse(WinstonDatabase.WINSTON_TABLE_DATE_FORMAT, day);
				double tet = tst + ONE_DAY;
				String table = code + "$$" + day;
				if (!winston.tableExists(code, table))
					continue;

				if (tet < t1)
					continue;
				if (tst > t2)
					continue;

				ResultSet rs = winston.getStatement()
						.executeQuery("SELECT st, et FROM `" + table + "` ORDER BY st ASC");
				while (rs.next()) {
					double start = rs.getDouble(1);
					double end = rs.getDouble(2);
					if (end < t1)
						continue;
					if (start > t2)
						continue;
					bufs.add(new double[] { start, end });
				}
				rs.close();
			}

			if (bufs == null || bufs.size() == 0) {
				// there were no tracebufs in the time range, the whole span is a gap.
				gaps.add(new double[] { t1, t2 });
				return gaps;
			}

			double epsilon = 0.01;
			if (bufs.get(0)[0] > t1) {
				gaps.add(new double[] { t1, bufs.get(0)[0] });
			}
			double last = bufs.get(0)[1];
			for (int i = 1; i < bufs.size(); i++) {
				double[] buf = bufs.get(i);
				if (buf[0] - last > epsilon) {
					double start = last;
					double end = buf[0];
					if (end < t1)
						continue;
					if (start > t2)
						continue;
					if (start < t1)
						start = t1;
					if (end > t2)
						end = t2;

					gaps.add(new double[] { start, end });
				}

				last = buf[1];
			}
			if (bufs.get(bufs.size() - 1)[1] < t2) {
				gaps.add(new double[] { bufs.get(bufs.size() - 1)[1], t2 });
			}

			return gaps;
		} catch (Exception e) {
			e.printStackTrace();
		}
		return null;
	}

	/**
	 * Return wave data for timespan t1..t2 for channel w/ id sid; cap result at
	 * maxrows size. Currently returns larger than asked for.
	 * 
	 * @param sid
	 *            id of channel
	 * @param t1
	 *            start time
	 * @param t2
	 *            end time
	 * @param maxrows
	 *            cap on size of result
	 * @return wave
	 * @throws UtilException
	 */
	public Wave getWave(int sid, double t1, double t2, int maxrows) throws UtilException {
		String code = channels.getChannelCode(sid);
		return getWave(code, t1, t2, maxrows);
	}

	/**
	 * Yield tracebuf from a ResultSet as an array of buytes
	 */
	private byte[] getTraceBufBytes(ResultSet rs) throws SQLException, IOException {
		Blob b = rs.getBlob("tracebuf");
		byte[] bs = b.getBytes(1, (int) b.length());
		bs = Util.decompress(bs, 512);
		return bs;
	}

	/**
	 * Modified 2006-08-14 to not check for consistent sample rates. That duty
	 * is passed to TraceBuf.traceBufsToWave(). This should fix the HVO bug
	 * that trig2disk can not get tracebufs with varying sample rates.
	 * 
	 * @param code
	 * @param t1
	 * @param t2
	 * @param maxrows
	 * @return trace buf data
	 * @throws UtilException
	 */
	public List<byte[]> getTraceBufBytes(String code, double t1, double t2, int maxrows) throws UtilException {
		int numSamplesCounter = 0;
		if (!winston.checkConnect() || !winston.useDatabase(code))
			return null;
		try {
			double ct = t1;

			ArrayList<byte[]> bufs = new ArrayList<byte[]>((int) Math.ceil(t2 - t1) + 1);
			String endDate = dateFormat.format(Util.j2KToDate(t2));
			boolean done = false;
			ResultSet rs = null;

			// MySQL only uses one key for optimization so selecting tracebufs
			// that lie between st and et is prohibitively slow. By selecting
			// the latest 1 tracebuf where the st is less than the desired time
			// we can quickly find the tracebuf that may overlap into the
			// desired interval. Unfortunately, this causes a problem for the
			// edge case of selecting data right at the UTC day boundary. To
			// fix this the query must be run against the day of the st of the
			// desired interval plus the day before.
			String[] initialDates = new String[] { dateFormat.format(Util.j2KToDate(t1 - ONE_DAY)),
					dateFormat.format(Util.j2KToDate(t1)) };

			for (String date : initialDates) {
				String sql = "SELECT st, et, sr, datatype, tracebuf FROM `" + code + "$$" + date
						+ "` WHERE st<? ORDER BY st DESC LIMIT 1";
				PreparedStatement ps = winston.getPreparedStatement(sql);
				try {
					ps.setDouble(1, t1);
					rs = ps.executeQuery();
					if (rs.next()) {
						if (t1 >= rs.getDouble(1) && t1 <= rs.getDouble(2)) {
							byte[] buf = getTraceBufBytes(rs);
							numSamplesCounter += getNumSamples(rs.getDouble(1), rs.getDouble(2), rs.getDouble(3));
							if (maxrows > 0 && numSamplesCounter > maxrows) {
								throw new UtilException("Max rows (" + maxrows + " rows) "
										+ (vdxName.length() > 0 ? ("for data source " + vdxName + " ") : "")
										+ "exceeded.");
							}
							bufs.add(buf);
						}
					}
					rs.close();
				} catch (SQLException e) {
					winston.getLogger().log(Level.FINEST, "No table found for " + date + ", " + t1 + "->" + t2,
							Util.getLineNumber(this, e));
				}
			}

			// got the first tracebuf, now lets get the rest.
			while (!done) {
				String date = dateFormat.format(Util.j2KToDate(ct));
				if (date.equals(endDate))
					done = true;

				String sql = "SELECT st, et, sr, datatype, tracebuf FROM `" + code + "$$" + date + "` WHERE st>=" + t1
						+ " AND st<=" + t2 + " ORDER BY st ASC";

				try {
					rs = winston.getStatement().executeQuery(sql);
				} catch (SQLException e) {
					// table not found
					winston.getLogger().log(Level.FINEST, "No table found for " + code + "$$" + date);
					ct += ONE_DAY;
					continue;
				}
				while (rs.next()) {

					byte[] buf = getTraceBufBytes(rs);
					numSamplesCounter += getNumSamples(rs.getDouble(1), rs.getDouble(2), rs.getDouble(3));
					if (maxrows > 0 && numSamplesCounter > maxrows)
						throw new UtilException("Max rows (" + maxrows + " rows) "
								+ (vdxName.length() > 0 ? ("for data source " + vdxName + " ") : "") + "exceeded.");
					bufs.add(buf);
				}
				rs.close();
				ct += ONE_DAY;
			}
			return bufs;
		} catch (SQLException e) {
			winston.getLogger().log(Level.FINEST, "Could not get TraceBuf bytes for " + code + ", " + t1 + "->" + t2,
					Util.getLineNumber(this, e));
		} catch (IOException e) {
			winston.getLogger().log(Level.FINEST, "Could not get TraceBuf bytes for " + code + ", " + t1 + "->" + t2,
					Util.getLineNumber(this, e));
		}
		return null;

	}

	/**
	 * Return wave meta data for timespan t1..t2 for channel w/ code; cap result at
	 * maxrows size.
	 * 
	 * @param code
	 *            channel
	 * @param t1
	 *            start time
	 * @param t2
	 *            end time
	 * @param maxrows
	 *            cap on size of result
	 * @return wave
	 */

	private int getNumSamples(double st, double et, double sr) {
		return new Double(sr * (et - st)).intValue();
	}

	public List<TraceBuf> getTraceBufs(String code, double t1, double t2, int maxrows) throws UtilException {
		List<byte[]> rawBufs = getTraceBufBytes(code, t1, t2, maxrows);
		if (rawBufs == null || rawBufs.size() == 0)
			return null;

		try {
			List<TraceBuf> traceBufs = new ArrayList<TraceBuf>(rawBufs.size());
			for (byte[] buf : rawBufs)
				traceBufs.add(new TraceBuf(buf));

			return traceBufs;
		} catch (Exception e) {
			winston.getLogger().log(Level.SEVERE, "Could not get TraceBufs for " + code + ", " + t1 + "->" + t2,
					Util.getLineNumber(this, e));
		}
		return null;
	}

	/**
	 * Return wave data for timespan t1..t2 for channel w/ code; cap result at
	 * maxrows size. Currently returns larger than asked for.
	 * 
	 * @param code
	 *            channel
	 * @param t1
	 *            start time
	 * @param t2
	 *            end time
	 * @return wave
	 */
	public Wave getWave(String code, double t1, double t2, int maxrows) throws UtilException {
		if (!winston.checkConnect() || !winston.useDatabase(code))
			return null;

		List<TraceBuf> bufs = getTraceBufs(code, t1, t2, maxrows);
		if (bufs == null || bufs.size() == 0)
			return null;
		else {
			Wave wave = TraceBuf.traceBufToWave(bufs);
			wave.convertToJ2K();
			return wave;
		}
	}

	public HelicorderData getHelicorderData(String code, double t1, double t2, int maxrows) throws UtilException {
		if (!winston.checkConnect() || !winston.useDatabase(code))
			return null;
		try {
			double ct = t1;
			// this 'fixes' problems when a start time of 0000 UTC is asked for
			// and that data are actually stored in the previous day. Some
			// issues remain.
			String endDate = dateFormat.format(Util.j2KToDate(t2));
			boolean done = false;
			ArrayList<double[]> list = new ArrayList<double[]>((int) (t2 - t1) + 2);
			while (!done) {
				String date = dateFormat.format(Util.j2KToDate(ct));
				if (date.equals(endDate))
					done = true;
				ct += ONE_DAY;
				String table = code + "$$H" + date;
				ResultSet rs = null;
				String sql = "SELECT j2ksec, smin, smax, rcnt FROM `" + table
						+ "` WHERE j2ksec>=? AND j2ksec<=? ORDER BY j2ksec ASC";

				if (maxrows != 0) {
					sql += " LIMIT " + (maxrows + 1);
					
					// Check the row count for the query before running the entire thing. Can save significant time
					// for sufficiently large queries that exceed the maxrows parameter
					PreparedStatement s = winston.getPreparedStatement("SELECT COUNT(*) FROM (SELECT 1 " + 
																		sql.substring(sql.indexOf("FROM")) + ") as T");
					try {
						s.setDouble(1, t1);
						s.setDouble(2, t2);
						rs = s.executeQuery();
					} catch (Exception e) {
						// table not found
						continue;
					}
					if (rs.next() && rs.getInt(1) > maxrows)
						throw new UtilException("Max rows (" + maxrows + " rows) "
								+ (vdxName.length() > 0 ? ("for data source " + vdxName + " ") : "") + "exceeded.");
				}
				
				PreparedStatement select = winston.getPreparedStatement(sql);
				try {
					select.setDouble(1, t1);
					select.setDouble(2, t2);
					rs = select.executeQuery();
				} catch (Exception e) {
					// table not found
					continue;
				}

				while (rs.next()) {
					double[] d = new double[] { rs.getDouble(1), rs.getDouble(2), rs.getDouble(3) }; // , rs.getInt(4)
																										// };
					list.add(d);
				}
				rs.close();
			}
			return new HelicorderData(list);
		} catch (SQLException e) {
			winston.getLogger().log(Level.SEVERE, "Could not get helicorder for " + code + ", " + t1 + "->" + t2,
					Util.getLineNumber(this, e));
		}
		return null;
	}

	public RSAMData getRSAMData(String code, double t1, double t2, int maxrows, DownsamplingType ds, int dsInt)
			throws UtilException {
		if (!winston.checkConnect() || !winston.useDatabase(code))
			return null;

		try {
			double ct = t1;
			// this 'fixes' problems when a start time of 0000 UTC is asked for
			// and that data are actually stored in the previous day. Some
			// issues remain.
			String endDate = dateFormat.format(Util.j2KToDate(t2));
			boolean done = false;
			int numSamplesCounter = 0;
			ArrayList<double[]> list = new ArrayList<double[]>((int) (t2 - t1) + 2);
			while (!done) {
				String date = dateFormat.format(Util.j2KToDate(ct));
				if (date.equals(endDate))
					done = true;
				ct += ONE_DAY;
				String table = code + "$$H" + date;
				ResultSet rs = null;
				String sql = "SELECT j2ksec, rsam" + " FROM `" + table + "` WHERE j2ksec>=" + t1 + " AND j2ksec<=" + t2
						+ " AND rcnt>0" + " ORDER BY j2ksec";
				try {
					sql = getDownsamplingSQL(sql, t1, ds, dsInt);
				} catch (UtilException e) {
					throw new UtilException("Can't downsample dataset: " + e.getMessage());
				}
				if (maxrows != 0) {
					sql += " LIMIT " + (maxrows + 1);
					
					// If the dataset has a maxrows paramater, check that the number of requested rows doesn't
					// exceed that number prior to running the full query. This can save a decent amount of time
					// for large queries. Note that this only applies for non-downsampled queries. This is done for
					// two reasons: 1) If the user is downsampling, they already know they're dealing with a lot of data
					// and 2) the way MySQL handles the multiple nested queries that would result makes it slower than
					// just doing the full query to begin with.
					if (ds.equals(DownsamplingType.NONE)) {
						try {
							rs = winston.getStatement().executeQuery("SELECT COUNT(*) FROM (SELECT 1 " + 
																	  sql.substring(sql.indexOf("FROM")) + ") as T");
						} catch (Exception e) {
							// table not found
							continue;
						}
						if (rs.next() && rs.getInt(1) > maxrows) {
							throw new UtilException("Max rows (" + maxrows + " rows) "
									+ (vdxName.length() > 0 ? ("for data source " + vdxName + " ") : "") + "exceeded.");
						}
					}
				}
				
				try {
					rs = winston.getStatement().executeQuery(sql);
				} catch (Exception e) {
					// table not found
					continue;
				}
				
				// Check for the amount of data returned in a downsampled query. Non-downsampled queries are checked above.
				if (!ds.equals(DownsamplingType.NONE) && maxrows != 0) {
					numSamplesCounter += getResultSetSize(rs);
					if (numSamplesCounter > maxrows) {
						throw new UtilException("Max rows (" + maxrows + " rows) "
								+ (vdxName.length() > 0 ? ("for data source " + vdxName + " ") : "") + "exceeded.");
					}
				}
				while (rs.next()) {
					double[] d = new double[] { rs.getDouble(1), rs.getDouble(2) };
					list.add(d);
				}
				rs.close();
			}
			return new RSAMData(list);
		} catch (SQLException e) {
			winston.getLogger().log(Level.SEVERE, "Could not get RSAM for " + code + ", " + t1 + "->" + t2,
					Util.getLineNumber(this, e));
		}
		return null;
	}

	/**
	 * Version of SQLDataSource.getDownsamplingSQL which doesn't use prepared statements, so as it's not supported by
	 * Winston.
	 */
	private static String getDownsamplingSQL(String sql, double startTime, DownsamplingType ds, int dsInt)
			throws UtilException {
		if (!ds.equals(DownsamplingType.NONE) && dsInt <= 1)
			throw new UtilException("Downsampling interval should be more than 1");
		if (ds.equals(DownsamplingType.NONE))
			return sql;
		else if (ds.equals(DownsamplingType.DECIMATE))
			return "SELECT * FROM(SELECT fullQuery.*, @row := @row+1 AS rownum FROM (" + sql
					+ ") fullQuery, (SELECT @row:=0) r) ranked WHERE rownum % " + dsInt + " = 1";
		else if (ds.equals(DownsamplingType.MEAN)) {
			String sql_select_clause = sql.substring(6, sql.toUpperCase().indexOf("FROM") - 1);
			String sql_from_where_clause = sql.substring(sql.toUpperCase().indexOf("FROM") - 1, sql.toUpperCase()
					.lastIndexOf("ORDER BY") - 1);
			String[] columns = sql_select_clause.split(",");
			String avg_sql = "SELECT ";
			for (String column : columns) {
				avg_sql += "AVG(" + column.trim() + "), ";
			}
			avg_sql += "((j2ksec-" + startTime + ") DIV " + dsInt + ") intNum ";
			avg_sql += sql_from_where_clause;
			avg_sql += " GROUP BY intNum";
			return avg_sql;
		} else
			throw new UtilException("Unknown downsampling type: " + ds);
	}

	public void setVdxName(String name) {
		this.vdxName = name;
	}
	
	public static int getResultSetSize(ResultSet rs) throws SQLException {
		int size =0;
		int currentRow = rs.getRow();
		if (rs != null){  
		   rs.beforeFirst();  
		   rs.last();  
		   size = rs.getRow();
		   if(currentRow==0){
			   rs.beforeFirst();   
		   } else {
			   rs.absolute(currentRow);
		   }
		}
		return size;   
	}


}
