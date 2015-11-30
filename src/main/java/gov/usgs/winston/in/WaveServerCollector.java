package gov.usgs.winston.in;

import gov.usgs.earthworm.Menu;
import gov.usgs.earthworm.MenuItem;
import gov.usgs.earthworm.SCN;
import gov.usgs.earthworm.WaveServer;
import gov.usgs.earthworm.message.TraceBuf;
import gov.usgs.util.CurrentTime;
import gov.usgs.util.Util;
import gov.usgs.winston.db.Channels;
import gov.usgs.winston.db.Data;
import gov.usgs.winston.db.Input;
import gov.usgs.winston.db.WinstonDatabase;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

/**
 *
 * @author Dan Cervelli
 */
@Deprecated
public class WaveServerCollector extends Thread
{
	public static final int COLLECT = 1;
	public static final int FILL_GAPS = 2;
	private int mode;
	private WinstonDatabase winston;
	private Channels winstonStations;
	private Input input;
	private Data data;
	private int interval;	// seconds
	private int maxSize;	// seconds
	private int delay;
	
	private List<SCN> channels;
	private String name;
	private WaveServer waveServer;
	
	public WaveServerCollector(String n, WinstonDatabase w, WaveServer ws, int i, int m, int d, int md)
	{
		name = n;
		winston = w;
		input = new Input(winston);
		winstonStations = new Channels(winston);
		data = new Data(winston);
		interval = i;
		maxSize = m;	
		delay = d;
		mode = md;
		waveServer = ws;
		channels = new ArrayList<SCN>();
	}

	public void startCollecting()
	{
		start();		
	}
	
	public void stopCollecting()
	{
		
	}
	
	public void addStation(SCN ci)
	{
		channels.add(ci);	
	}
	
	public void fillGap(SCN scn, double t1, double t2)
	{
		double ct = t1;
		waveServer.connect();
		while (ct < t2)
		{
			long ts = System.currentTimeMillis();
			List<TraceBuf> tbs = null;
			if (t2 - ct > maxSize)
			{
				tbs = waveServer.getTraceBufs(scn.station, scn.channel, scn.network, Util.j2KToEW(ct - 5), Util.j2KToEW(ct + maxSize + 5));
				ct += maxSize;
			}
			else
			{
				tbs = waveServer.getTraceBufs(scn.station, scn.channel, scn.network, Util.j2KToEW(ct - 5), Util.j2KToEW(t2 + 5));
				ct = t2;
			}
			if (tbs != null && tbs.size() > 0)
			{
				for (Object o : tbs)
				{
					TraceBuf tb = (TraceBuf)o;
					tb.createBytes();
					input.inputTraceBuf(tb, false);
				}
			}
			long te = System.currentTimeMillis();
			System.out.println("Chunk: " + ((double)(te - ts) / 1000) + "s");
		}
		waveServer.close();
	}
	
	public void fillGaps()
	{
		Menu menu = waveServer.getMenu();
		Iterator<SCN> it = channels.iterator();
		while (it.hasNext())
		{
			SCN scn = (SCN)it.next();
			String code = scn.toString().replace('_', '$');
			System.out.println("[" + name + "/" + code + "]: ");
			
			double now = CurrentTime.getInstance().nowEW();
			
			MenuItem mi = menu.getItem(scn);
			if (mi == null)
				continue;
			
			List<double[]> gaps = data.findGaps(code, Util.ewToJ2K(mi.startTime), now);
			double[] span = data.getTimeSpan(code);
			double fdt = span[0];
			if (fdt > Util.ewToJ2K(mi.startTime))
			{
				gaps.add(new double[] { Util.ewToJ2K(mi.startTime), fdt });
			}
			if (gaps != null)
			{
				Iterator<double[]> it2 = gaps.iterator();
				while (it2.hasNext())
				{
					double[] gap = (double[])it2.next();
					System.out.println((gap[1] - gap[0]) + "s, " + gap[0] + " -> " + gap[1]);
					fillGap(scn, gap[0] - 5, gap[1] + 5);
				}
			}
		}
	}
	
	public void collect()
	{
		Iterator<SCN> it = channels.iterator();
		while (it.hasNext())
		{
			long ts = System.currentTimeMillis();
			SCN scn = (SCN)it.next();
			String code = scn.toString().replace('_', '$');
			System.out.print("[" + name + "/" + code + "]: ");
			
			boolean stationOk = true;
			if (!winstonStations.channelExists(code))
			{
				System.out.print("creating new station in Winston; ");
				stationOk = winstonStations.createChannel(code);
			}
			if (!stationOk)
				continue;
			
			double[] span = data.getTimeSpan(code);
			double now = CurrentTime.getInstance().nowEW();
			double last = Util.j2KToEW(span[1]);
			if (last == -1E300)
				last = now - 10 * 60;
			
			List<TraceBuf> tbs = waveServer.getTraceBufs(scn.station, scn.channel, scn.network, last, now);
			if (tbs == null || tbs.size() == 0)
			{
				System.out.print("wave server returned no data; ");	
			}
			else
			{
				for (Object o : tbs)
				{
					TraceBuf tb = (TraceBuf)o;
					tb.createBytes();
					input.inputTraceBuf(tb, false);
				}
			}
			
			long te = System.currentTimeMillis();
			
			System.out.println("done. " + ((double)(te - ts) / 1000) + "s");
		}
	}
	
	public void run()
	{
		try { Thread.sleep(delay * 1000); } catch (Exception e) {}
		if (mode == COLLECT)
		{
			while (true)
			{
				try
				{
					collect();
					Thread.sleep(interval * 1000);
				}
				catch (Exception e)
				{
					e.printStackTrace();	
				}
			}
		}
		else
		{
			fillGaps();
		}
	}
}
