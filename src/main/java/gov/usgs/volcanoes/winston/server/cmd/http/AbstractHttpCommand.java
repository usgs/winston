package gov.usgs.volcanoes.winston.server.cmd.http;

import java.nio.channels.SocketChannel;
import java.text.DateFormat;
import java.text.DecimalFormat;
import java.text.NumberFormat;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Map;
import java.util.TimeZone;

import gov.usgs.net.HttpRequest;
import gov.usgs.net.HttpResponse;
import gov.usgs.net.NetTools;
import gov.usgs.volcanoes.core.time.J2kSec;
import gov.usgs.volcanoes.core.util.StringUtils;
import gov.usgs.volcanoes.winston.db.Data;
import gov.usgs.volcanoes.winston.db.WaveServerEmulator;
import gov.usgs.volcanoes.winston.db.WinstonDatabase;
import gov.usgs.volcanoes.winston.server.WWS;

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
  protected AbstractHttpCommand(final NetTools nt, final WinstonDatabase db, final WWS wws) {
    netTools = nt;
    winston = db;
    this.wws = wws;
    maxDays = wws.getMaxDays();
    emulator = new WaveServerEmulator(db);
    data = new Data(db);
    decimalFormat = (DecimalFormat) NumberFormat.getInstance();
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
  public void respond(final String cmd, final SocketChannel c, final HttpRequest request) {
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
  public void writeSimpleHTML(final String msg) {
    final String html = "<html><body>" + msg + "</body></html>";
    final HttpResponse response = new HttpResponse("text/html");
    response.setLength(html.length());
    netTools.writeString(response.getHeaderString(), socketChannel);
    netTools.writeString(html, socketChannel);
  }

  /**
   * Return a string as a Text page
   * 
   * @param msg
   */
  public void writeSimpleText(final String msg) {

    final HttpResponse response = new HttpResponse("text/plain");
    response.setLength(msg.length());
    netTools.writeString(response.getHeaderString(), socketChannel);
    netTools.writeString(msg, socketChannel);
  }

  /**
   * 
   * @param t1
   *          start time String
   * @param endTime
   *          end time
   * @param mult
   *          number of seconds per interval used for relative times. 60 for
   *          minutes, 60*60 for hours, 60*60*24 for days
   * @return
   * @throws ParseException
   */
  protected Double getStartTime(final String t1, final Double endTime, final long mult) {
    Double startTime = Double.NaN;

    if (t1 == null || t1.substring(0, 1).equals("-")) {
      final double hrs = StringUtils.stringToDouble(t1, -12);
      startTime = endTime + hrs * mult;
    } else {
      final DateFormat dateFormat = new SimpleDateFormat(INPUT_DATE_FORMAT);
      dateFormat.setTimeZone(TimeZone.getTimeZone("GMT"));
      Date bt;
      try {
        bt = dateFormat.parse(t1);
        startTime = J2kSec.fromDate(bt);
      } catch (final ParseException e) {
        startTime = Double.NaN;
      }
    }

    return timeOrMaxDays(startTime);
  }

  /*
   * convert time back to UTC
   */
  protected Double getStartTime(final String t1, final Double endTime, final long mult,
      final TimeZone tz) {
    final double startTime = getStartTime(t1, endTime, mult);
    return timeOrMaxDays(startTime - (tz.getOffset(J2kSec.asEpoch(endTime))));
  }

  /**
   * parse end time
   * 
   * @param t2
   * @return
   */
  protected Double getEndTime(final String t2) {
    Double endTime = Double.NaN;
    if (t2 == null || t2.equalsIgnoreCase("now"))
      endTime = J2kSec.now();
    else {
      final DateFormat dateFormat = new SimpleDateFormat(INPUT_DATE_FORMAT);
      dateFormat.setTimeZone(TimeZone.getTimeZone("GMT"));
      Date bt;
      try {
        bt = dateFormat.parse(t2);
        endTime = J2kSec.fromDate(bt) - 1;
      } catch (final ParseException e) {
        endTime = Double.NaN;
      }
    }

    return timeOrMaxDays(endTime);
  }

  /**
   * convert back to UTC
   * 
   */
  protected Double getEndTime(final String t2, final TimeZone tz) {
    final Double endTime = getEndTime(t2);
    return timeOrMaxDays(endTime - (tz.getOffset(J2kSec.asEpoch(endTime))));
  }

  /**
   * Apply maxDays to time
   * 
   * @param t
   *          time
   * @return greater of t or now less maxDays
   */
  protected double timeOrMaxDays(final double t) {
    if (maxDays == 0)
      return t;
    else
      return Math.max(t, J2kSec.now() - (maxDays * ONE_DAY));
  }

  /**
   * Convert a boolean value to an integer as passed in arguments.
   * 
   * @param in
   * @return
   */
  protected int boolToInt(final boolean in) {
    return in == true ? 1 : 0;
  }
}
