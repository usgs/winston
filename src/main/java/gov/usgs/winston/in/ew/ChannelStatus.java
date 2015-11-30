package gov.usgs.winston.in.ew;

import gov.usgs.earthworm.message.TraceBuf;
import gov.usgs.util.CurrentTime;
import gov.usgs.util.Util;
import gov.usgs.winston.db.InputEW;

import java.text.SimpleDateFormat;
import java.util.Comparator;
import java.util.TimeZone;

/**
 * 
 * $Log: not supported by cvs2svn $
 * @author Dan Cervelli
 */
public class ChannelStatus
{
	public enum SortOrder
	{
		CHANNEL,
		LAST_INSERT_TIME,
		MIN_TIME,
		MAX_TIME,
		FAILURES,
		SUCCESSES;
		
		public static SortOrder parse(char c)
		{
			switch (Character.toUpperCase(c))
			{
				default:
				case 'C':
					return CHANNEL;
				case 'L':
					return LAST_INSERT_TIME;
				case 'M':
					return MIN_TIME;
				case 'X':
					return MAX_TIME;
				case 'S':
					return SUCCESSES;
				case 'F':
					return FAILURES;
			}
		}
	}
	
	private String channel;
	private int successes = 0;
	private int failures = 0;
	private double lastTime;
	private double lastBufTime = 0;
	private double minBufTime = 1E300;
	private double maxBufTime = -1E300;
	private SimpleDateFormat dateFormat;

	public ChannelStatus(String c)
	{
		dateFormat = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss.SSS");
		dateFormat.setTimeZone(TimeZone.getTimeZone("GMT"));
		channel = c;
	}

	public void process(TraceBuf tb, InputEW.InputResult.Code code)
	{
		if (code == InputEW.InputResult.Code.SUCCESS || 
				code == InputEW.InputResult.Code.SUCCESS_CREATED_TABLE)
		{
			successes++;
			lastTime = CurrentTime.getInstance().nowJ2K();
		}
		else
			failures++;

		if (tb != null)
		{
			lastBufTime = tb.getStartTimeJ2K();
			minBufTime = Math.min(lastBufTime, minBufTime);
			maxBufTime = Math.max(lastBufTime, maxBufTime);
		}
	}

	public double timeSinceLast()
	{
		double now = CurrentTime.getInstance().nowJ2K();
		return now - lastTime;
	}

	public static String getHeaderString()
	{
		return "[C]hannel     [L]ast ins   [M]in time               Ma[x] time               [S]uccess/[F]ailure";
	}
	
	public String toString()
	{
		return String.format(
				"%-13s %-12s %-24s %-24s %-12s", 
				channel, 
				Util.timeDifferenceToString(timeSinceLast()),
				dateFormat.format(Util.j2KToDate(minBufTime)),
				dateFormat.format(Util.j2KToDate(maxBufTime)),
				successes + "/" + failures
				);
	}
	
	public static Comparator<ChannelStatus> getComparator(
			final SortOrder order, final boolean desc)
	{
		return new Comparator<ChannelStatus>()
		{
			public int compare(ChannelStatus cs1, ChannelStatus cs2)
			{
				int cmp = 0;
				switch (order)
				{
					default:
					case CHANNEL:
						cmp = cs1.channel.compareTo(cs2.channel);
						break;
					case LAST_INSERT_TIME:
						cmp = Double.compare(cs2.lastTime, cs1.lastTime);
						break;
					case SUCCESSES:
						cmp = cs1.successes - cs2.successes;
						break;
					case FAILURES:
						cmp = cs1.failures - cs2.failures;
						break;
					case MAX_TIME:
						cmp = Double.compare(cs1.maxBufTime, cs2.maxBufTime);
						break;
					case MIN_TIME:
						cmp = Double.compare(cs1.minBufTime, cs2.minBufTime);
						break;
				}
				if (desc)
					cmp = -cmp;
				
				return cmp;
			}
		};
	}
}
