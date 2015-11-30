package gov.usgs.winston.db;

import gov.usgs.earthworm.message.TraceBuf;
import gov.usgs.util.Util;
import gov.usgs.util.UtilException;
import gov.usgs.winston.Channel;

import java.sql.ResultSet;
import java.text.DateFormat;
import java.text.DecimalFormat;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.List;
import java.util.StringTokenizer;
import java.util.TimeZone;
import java.util.logging.Level;

/**
 * A class for manipulating data from Winston for use in the WWS.
 * 
 * TODO: refactor.  This is getting pretty messy.
 * 
 * TODO: either implement or eliminate embargo and span.
 *
 * @author Dan Cervelli
 */
public class WaveServerEmulator
{
	protected final static int ONE_HOUR = 60 * 60;
	protected final static int ONE_DAY = 24 * ONE_HOUR;
	
	private WinstonDatabase winston;
	private Channels channels;
	private Data data;
	private DateFormat dateFormat;
	private DecimalFormat decimalFormat;

	public WaveServerEmulator(WinstonDatabase w)
	{
		winston = w;
		channels = new Channels(w);
		data = new Data(w);
		dateFormat = new SimpleDateFormat("yyyy_MM_dd");
		dateFormat.setTimeZone(TimeZone.getTimeZone("GMT"));
		decimalFormat = (DecimalFormat)DecimalFormat.getInstance();
		decimalFormat.setMaximumFractionDigits(200);
		decimalFormat.setGroupingUsed(false);
	}

	public int getChannelID(String s, String c, String n, String l)
	{
		String loc = "";
		if (l != null && !l.equals("--"))
			loc = "$" + l;
		String trueCode = s + "$" + c + "$" + n + loc;
		int id = channels.getChannelID(trueCode);
		return id;
	}
	
	public int getChannelID(String s, String c, String n)
	{
		return getChannelID(s, c, n, null);
	}

	
	/**
	 * TODO: implement embargo
	 * TODO: implement span
	 * TODO: make more efficient
	 * TODO: return correct dataType
	 * @param embargo
	 * @param span
	 * @return menu
	 */
	public List<String> getWaveServerMenu(boolean scnl, double embargo, double span) {
		return getWaveServerMenu(scnl, embargo, span, 0);
	}

	public List<String> getWaveServerMenu(boolean scnl, double embargo, double span, double maxDays)
	{
		if (!winston.checkConnect())
			return null;

		List<Channel> sts = channels.getChannels();
		if (sts == null)
			return null;

		List<String> list = new ArrayList<String>(sts.size());
		for (Channel st : sts)
		{
			String[] ss = st.getCode().split("\\$");
			double[] ts = { st.getMinTime(), st.getMaxTime() };


			if (maxDays > 0)
				ts[0] = Math.max(ts[0], Util.nowJ2K() - (maxDays * ONE_DAY));
			
			if (ts != null && ts[0] < ts[1])
			{
				
				if (!scnl && ss.length == 3)
					list.add(" " + st.getSID() + " " + ss[0] + " " + ss[1] + " " + ss[2] + " " + decimalFormat.format(Util.j2KToEW(ts[0])) + " " + decimalFormat.format(Util.j2KToEW(ts[1])) + " s4 ");
				else if (scnl)
				{
					String loc = (ss.length == 4 ? ss[3] : "--");
					String line = " " + st.getSID() + " " + ss[0] + " " + ss[1] + " " + ss[2] + " " + loc + " " + decimalFormat.format(Util.j2KToEW(ts[0])) + " " + decimalFormat.format(Util.j2KToEW(ts[1])) + " s4 ";
					list.add(line);
				}
			}
		}
		return list;
	}

	public String getWaveServerMenuItem(int p, double embargo, double span)
	{
		if (!winston.checkConnect())
			return null;

		try
		{
			String result = null;
			winston.useRootDatabase();
			ResultSet rs = winston.getStatement().executeQuery("SELECT code FROM channels WHERE sid=" + p);
			if (rs.next())
			{
				String code = rs.getString(1);
				double[] ts = data.getTimeSpan(code);

				/*
				 if (embargo != 0)
				 {
				 double now = CurrentTime.nowJ2K();
				 double emnow = now - embargo;
				 maxt = Math.min(emnow, maxt);
				 }
				 if (span != 0)
				 mint = Math.max(mint, maxt - span);
				 */
				StringTokenizer st = new StringTokenizer(code, "$");
				String sta = st.nextToken();
				String ch = st.nextToken();
				String nw = st.nextToken();

				result = " " + p + " " + sta + " " + ch + " " + nw + " " + decimalFormat.format(Util.j2KToEW(ts[0])) + " " + decimalFormat.format(Util.j2KToEW(ts[1])) + " s4 ";
			}
			rs.close();
			return result;
		}
		catch (Exception e)
		{
			winston.getLogger().log(Level.SEVERE, "Could not get wave server menu item.", Util.getLineNumber(this, e));
		}
		return null;
	}

	public String getWaveServerMenuItem(String s, String c, String n, String l, double embargo, double span)
	{
		String loc = "";
		if (l != null && !l.equals("--"))
			loc = "$" + l;
		String trueCode = s + "$" + c + "$" + n + loc;
		int id = channels.getChannelID(trueCode);
		if (id == -1)
			return null;
		else
			return getWaveServerMenuItem(id, embargo, span);
	}

	public Object[] getWaveServerRaw(String s, String c, String n, double t1, double t2)
	{
		return getWaveServerRaw(s, c, n, null, t1, t2);
	}
	
	// TODO: fix. Returning Object[] is not the right design
	public Object[] getWaveServerRaw(String s, String c, String n, String l, double t1, double t2)
	{
		String lc = "";
		if (l != null && !l.equals("--"))
			lc = "$" + l;
		String code = s + "$" + c + "$" + n + lc;
		if (!winston.checkConnect() || !winston.useDatabase(code))
			return null;
		List<byte[]> bufs = null;
		try{
			bufs = data.getTraceBufBytes(code, t1, t2, 0);
		} catch (UtilException e){
		}
		if (bufs == null || bufs.size() == 0)
			return null;

		try
		{
			int sid = channels.getChannelID(code);
			TraceBuf tb0 = new TraceBuf(bufs.get(0));
			TraceBuf tbN = new TraceBuf(bufs.get(bufs.size() - 1));
			int total = 0;
			for (byte[] buf : bufs)
				total += buf.length;
			
			String lr = "";
			if (l != null)
				lr = " " + l;
			String hdr = sid + " " + s + " " + c + " " + n + lr + " F " + tb0.dataType() + " " + tb0.getStartTime() + " " + tbN.getEndTime() + " " + total;
			return new Object[] { hdr, new Integer(total), bufs };
		}
		catch (Exception e)
		{
			winston.getLogger().log(Level.SEVERE, "Could not get raw wave.", e);
		}
		return null;
	}
}