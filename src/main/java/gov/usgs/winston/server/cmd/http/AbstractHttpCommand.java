package gov.usgs.winston.server.cmd.http;

import gov.usgs.net.HttpRequest;
import gov.usgs.net.HttpResponse;
import gov.usgs.net.NetTools;
import gov.usgs.util.CurrentTime;
import gov.usgs.util.Util;
import gov.usgs.winston.db.Data;
import gov.usgs.winston.db.WaveServerEmulator;
import gov.usgs.winston.db.WinstonDatabase;
import gov.usgs.winston.server.WWS;

import java.nio.channels.SocketChannel;
import java.text.DateFormat;
import java.text.DecimalFormat;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Map;
import java.util.TimeZone;

/**
 * All possible HTTP commands are expected to extend this class. When a new
 * class is created, it must be added to HttpGetCommand
 * 
 * @author Tom Parker
 * 
 */
public abstract class AbstractHttpCommand {
	protected static final String INPUT_DATE_FORMAT = "yyyyMMddHHmm";
	protected static final String DISPLAY_DATE_FORMAT = "yyyy-MM-dd HH:mm";
	protected final static int ONE_MINUTE = 60;
	protected final static int ONE_HOUR = 60 * ONE_MINUTE;
	protected final static int ONE_DAY = 24 * ONE_HOUR;
    protected static final String DEFAULT_TZ = "UTC";


	protected SocketChannel socketChannel;
	protected NetTools netTools;
	protected WinstonDatabase winston;
	protected WWS wws;
	protected Data data;
	protected WaveServerEmulator emulator;
	protected int maxDays;
	protected HttpRequest request;
	protected String cmd;
	protected Map<String, String> arguments;
	protected DecimalFormat decimalFormat;


	/**
	 * Command as a http file
	 * 
	 * @return command, including leading /
	 */
	abstract public String getCommand();


	/**
	 * Do the work. Return response to the browser.
	 */
	abstract protected void sendResponse();

	/**
	 * Class constructor.
	 * 
	 * @param nt
	 * @param db
	 * @param wws
	 */
	protected AbstractHttpCommand(NetTools nt, WinstonDatabase db, WWS wws) {
		netTools = nt;
		winston = db;
		this.wws = wws;
		maxDays = wws.getMaxDays();
		emulator = new WaveServerEmulator(db);
		data = new Data(db);
		decimalFormat = (DecimalFormat)DecimalFormat.getInstance();
		decimalFormat.setMaximumFractionDigits(3);
		decimalFormat.setGroupingUsed(false);
	}

	/**
	 * Initiate response. Set variable and pass control to the command.
	 * 
	 * @param cmd
	 * @param c
	 * @param request
	 */
	public void respond(String cmd, SocketChannel c, HttpRequest request) {
		this.socketChannel = c;
		this.request = request;
		this.cmd = cmd;
		arguments = request.getArguments();
		sendResponse();
	}

	/**
	 * Return a string as a HTML page
	 * 
	 * @param msg
	 */
	public void writeSimpleHTML(String msg) {
		String html = "<html><body>" + msg + "</body></html>";
		HttpResponse response = new HttpResponse("text/html");
		response.setLength(html.length());
		netTools.writeString(response.getHeaderString(), socketChannel);
		netTools.writeString(html, socketChannel);
	}

	/**
	 * Return a string as a Text page
	 * 
	 * @param msg
	 */
	public void writeSimpleText(String msg) {

		HttpResponse response = new HttpResponse("text/plain");
		response.setLength(msg.length());
		netTools.writeString(response.getHeaderString(), socketChannel);
		netTools.writeString(msg, socketChannel);
	}

	/**
	 * 
	 * @param t1
	 *            start time String
	 * @param endTime
	 *            end time
	 * @param mult
	 *            number of seconds per interval used for relative times. 60 for
	 *            minutes, 60*60 for hours, 60*60*24 for days
	 * @return
	 * @throws ParseException
	 */
	protected Double getStartTime(String t1, Double endTime, long mult) {
		Double startTime = Double.NaN;

		if (t1 == null || t1.substring(0, 1).equals("-")) {
			double hrs = Util.stringToDouble(t1, -12);
			startTime = endTime + hrs * mult;
		} else {
			DateFormat dateFormat = new SimpleDateFormat(INPUT_DATE_FORMAT);
			dateFormat.setTimeZone(TimeZone.getTimeZone("GMT"));
			Date bt;
			try {
				bt = dateFormat.parse(t1);
				startTime = Util.dateToJ2K(bt);
			} catch (ParseException e) {
				startTime = Double.NaN;
			}
		}

		return timeOrMaxDays(startTime);
	}

	/*
	 * convert time back to UTC
	 */
	protected Double getStartTime(String t1, Double endTime, long mult, TimeZone tz) {
		double startTime = getStartTime(t1, endTime, mult);
		return timeOrMaxDays(startTime - (tz.getOffset((long)Util.j2KToEW(endTime)*1000)/1000));
	}
	
	/**
	 * parse end time
	 * 
	 * @param t2
	 * @return
	 */
	protected Double getEndTime(String t2) {
		Double endTime = Double.NaN;
		if (t2 == null || t2.equalsIgnoreCase("now"))
			endTime = CurrentTime.getInstance().nowJ2K();
		else {
			DateFormat dateFormat = new SimpleDateFormat(INPUT_DATE_FORMAT);
			dateFormat.setTimeZone(TimeZone.getTimeZone("GMT"));
			Date bt;
			try {
				bt = dateFormat.parse(t2);
				endTime = Util.dateToJ2K(bt) - 1;
			} catch (ParseException e) {
				endTime = Double.NaN;
			}
		}

		return timeOrMaxDays(endTime);
	}

	/**
	 * convert back to UTC
	 * 
	 */
	protected Double getEndTime(String t2, TimeZone tz) {
		Double endTime = getEndTime(t2);
		return timeOrMaxDays(endTime - (tz.getOffset((long)Util.j2KToEW(endTime)*1000)/1000));
	}
	/**
	 * Apply maxDays to time
	 * 
	 * @param t
	 *            time
	 * @return greater of t or now less maxDays
	 */
	protected double timeOrMaxDays(double t) {
		if (maxDays == 0)
			return t;
		else
			return Math.max(t, Util.nowJ2K() - (maxDays * ONE_DAY));
	}

	/**
	 * Convert a boolean value to an integer as passed in arguments.
	 * 
	 * @param in
	 * @return
	 */
	protected int boolToInt(boolean in) {
		return in == true ? 1 : 0;
	}
}
