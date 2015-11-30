package gov.usgs.volcanoes.winston.in.ew;

import gov.usgs.earthworm.Menu;
import gov.usgs.earthworm.WaveServer;
import gov.usgs.earthworm.message.TraceBuf;
import gov.usgs.util.CodeTimer;
import gov.usgs.util.Log;
import gov.usgs.util.Time;
import gov.usgs.util.Util;
import gov.usgs.winston.db.Channels;
import gov.usgs.winston.db.InputEW;
import gov.usgs.winston.db.WinstonDatabase;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.logging.Logger;

/**
 * If a span is added to the job it is an explicit guarantee that there is
 * no existing data in that span.  Therefore incoming TraceBufs only need to be
 * between the given span to avoid overlappers.
 *
 * @author Dan Cervelli
 */
public class ImportWSJob
{
	private WinstonDatabase winston;
	private WaveServer waveServer;
	
	private String channel;
	private List<double[]> spans;
	
	private Logger logger;
	
	private Menu menu;
	
	private Channels channels;
	private InputEW input;
	
	private double chunkSize;
	private int chunkDelay;
	
	private boolean rsamEnable = true;
	private int rsamDelta = 10;
	private int rsamDuration = 60;
	
	private boolean quit = false;
	
	private boolean requestSCNL = false;
	
	private ImportWS importWS;
	
	public ImportWSJob(WinstonDatabase w, WaveServer ws, ImportWS is)
	{
		importWS = is;
		winston = w;
		waveServer = ws;
		spans = new ArrayList<double[]>(10);
		channels = new Channels(winston);
		input = new InputEW(winston);
		menu = importWS.getMenu();
		requestSCNL = importWS.getRequestSCNL();
		logger = Log.getLogger("gov.usgs.winston");
	}
	
	public void setRSAMParameters(boolean en, int rd, int rl)
	{
		rsamEnable = en;
		rsamDelta = rd;
		rsamDuration = rl;
	}
	
	public void setChannel(String c)
	{
		channel = c;
	}
	
	public String getChannel()
	{
		return channel;
	}
	
	public void addSpan(double t1, double t2)
	{
		spans.add(new double[] {t1, t2});
	}
	
	public void setChunkSize(double sec)
	{
		chunkSize = sec;
	}

	public void setChunkDelay(int ms)
	{
		chunkDelay = ms;
	}
	
	public void quit()
	{
		quit = true;
	}
	
	private void getData(double[] span)
	{
		try
		{
			String[] ss = channel.split("\\$");
			String loc = null;
			if (ss.length == 4)
				loc = ss[3];
			
			if (requestSCNL && loc == null)
			    loc = "--";
			
			double t1 = span[0];
			double t2 = span[1];
			
			logger.info(String.format("%s: downloading gap: [%s -> %s, %s]",
					channel, 
					Time.toDateString(span[0]),
					Time.toDateString(span[1]),
					Util.timeDifferenceToString(span[1] - span[0])));
			
			input.setRowParameters((int)chunkSize + 65, 60);
			
			double ct = t1 - chunkSize;
			List<TraceBuf> tbs = null;
			CodeTimer timer = new CodeTimer("chunk");
			int total = 0;
			double totalInsTime = 0;
			double totalDlTime = 0;
			while (ct < t2)
			{
				if (quit)
					break;
				ct += chunkSize;
				double ret = Math.min(ct + chunkSize + 5, t2 + 5);
				CodeTimer netTimer = new CodeTimer("net");
				tbs = waveServer.getTraceBufs(ss[0], ss[1], ss[2], loc, Util.j2KToEW(ct - 5), Util.j2KToEW(ret));
				netTimer.stop();
				totalDlTime += netTimer.getTotalTimeMillis();
				if (tbs != null && tbs.size() > 0)
				{
					Iterator<TraceBuf> it = tbs.iterator();
					double minTime = 1E300;
					double maxTime = -1E300;
					while (it.hasNext())
					{
						TraceBuf tb = it.next();
						minTime = Math.min(tb.getStartTimeJ2K(), minTime);
						maxTime = Math.max(tb.getEndTimeJ2K(), maxTime);
						if (tb.getEndTimeJ2K() < t1 || tb.getStartTimeJ2K() > t2)
						{
							// these are totally outside range so can be dropped quietly.
							it.remove();
							continue;
						}
						if (tb.getStartTimeJ2K() - t1 < -0.0001 || tb.getEndTimeJ2K() - t2 > 0.0001)
						{
							it.remove();
							logger.finest(String.format("Overlapping TraceBuf skipped.", tb.getStartTimeJ2K() - t1, tb.getEndTimeJ2K() - t2));
							continue;
						}
						tb.createBytes();
					}
					if (tbs.size() == 0)
						continue;
					try
					{
						if (chunkDelay > 0)
						{
							logger.finest(String.format("%s: delaying for %dms...", channel, chunkDelay));
							Thread.sleep(chunkDelay);
						}
					}
					catch (Exception e) { e.printStackTrace(); }
					CodeTimer inputTimer = new CodeTimer("input");
					List<InputEW.InputResult> results = input.inputTraceBufs(tbs, rsamEnable, rsamDelta, rsamDuration);
					inputTimer.stop();
					totalInsTime += inputTimer.getTotalTimeMillis();
					logger.fine(String.format("%s: %d tb (%.0f/%.0fms), [%s -> %s, %s]",
							channel,
							tbs.size(),
							netTimer.getRunTimeMillis(), 
							inputTimer.getRunTimeMillis(),
							Time.toDateString(minTime),
							Time.toDateString(maxTime),
							Util.timeDifferenceToString(maxTime - minTime)
							));
					
					// TODO: clean this up, unify with ImportEW
					if (results.size() == 1)
					{
	//					 TODO: handle errors
						InputEW.InputResult result = results.get(0);
						logger.warning("Error: " + result.code);
					}
					else
					{
						for (int i = 0; i < results.size() - 2; i++)
						{
							InputEW.InputResult result = results.get(i);
							TraceBuf tb = result.traceBuf;
							switch (result.code)
							{
								case SUCCESS_CREATED_TABLE:
									logger.fine(String.format("%s: day table created (%s)", channel, Time.format("yyyy-MM-dd", tb.getEndTimeJ2K() + 1)));
								case SUCCESS:
									total++;
									logger.finest("Insert: " + tb.toString());
									break;
								case ERROR_DATABASE:
									//fixing
									logger.warning("Database error: " + tb.toString());
									break;
								case ERROR_UNKNOWN:
									logger.warning("Unknown insert error: " + tb.toString());
									break;
								case ERROR_CHANNEL:
								case ERROR_NULL_TRACEBUF:
									// these errors should never occur
									logger.warning("Bad channel/null TraceBuf.");
									break;
								case ERROR_DUPLICATE:
									logger.finer("Duplicate TraceBuf: " + tb.toString());
									break;
								case NO_CODE:
									// this should never occur
									logger.warning("No error/success code: " + tb.toString());
									break;
								case ERROR_HELICORDER:
									break;
								case ERROR_INPUT:
									break;
								case ERROR_NO_WINSTON:
									break;
								case ERROR_TIME_SPAN:
									break;
								case SUCCESS_HELICORDER:
									break;
								case SUCCESS_TIME_SPAN:
									break;
								default:
									break;
							}	
						}
					}
				}
			}
			timer.stop();
			importWS.addStats(total, totalDlTime, totalInsTime);
			logger.info(String.format("%s: gap %s, %d tbs inserted in %.3fms (%.3fms/tb)", 
					channel, 
					(quit ? "interrupted" : "finished"), 
					total,
					timer.getTotalTimeMillis(),
					(total == 0 ? 0 : timer.getTotalTimeMillis() / total)));
		}
		catch (Throwable t)
		{
			t.printStackTrace();
		}
	}
	
	private void getAllData()
	{
		for (double[] span : spans)
		{
			if (!quit)
				getData(span);
		}
	}
	
	public double getSpansDuration()
	{
		double duration = 0.0;
		for (double[] span : spans)
			duration += span[1] - span[0];
		return duration;
	}
	
	public void go()
	{
		for (double[] span : spans)
		{
			logger.fine(String.format("%s: gap: [%s -> %s, %s]", 
					channel, Time.toDateString(span[0]), Time.toDateString(span[1]), Util.timeDifferenceToString(span[1] - span[0])));
		}
		
		logger.info(String.format("%s: starting job, total gaps: %d for a total duration of %s, Chunk: %.1fs, Delay: %dms", 
				channel, spans.size(), Util.timeDifferenceToString(getSpansDuration()), chunkSize, chunkDelay));
		
		if (!menu.channelExists(channel))
		{
			logger.severe("Channel does not exist on source WaveServer.");
			return;
		}
		
		if (!channels.channelExists(channel))
		{
			logger.info("Creating new channel '" + channel + "' in Winston database.");
			channels.createChannel(channel);
		}
		
		waveServer.connect();
		getAllData();
		waveServer.close();
		spans.clear();
	}
}
