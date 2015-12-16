package gov.usgs.volcanoes.winston.server.httpCmd;

import java.nio.channels.SocketChannel;
import java.text.DateFormat;
import java.text.DecimalFormat;
import java.text.NumberFormat;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.TimeZone;

import gov.usgs.net.HttpRequest;
import gov.usgs.net.HttpResponse;
import gov.usgs.net.NetTools;
import gov.usgs.volcanoes.core.time.J2kSec;
import gov.usgs.volcanoes.core.util.StringUtils;
import gov.usgs.volcanoes.winston.db.Data;
import gov.usgs.volcanoes.winston.db.WaveServerEmulator;
import gov.usgs.volcanoes.winston.db.WinstonDatabase;
import gov.usgs.volcanoes.winston.legacyServer.WWS;
import io.netty.channel.ChannelHandlerContext;
import io.netty.handler.codec.http.FullHttpRequest;
import io.netty.handler.codec.http.HttpMethod;
import io.netty.handler.codec.http.QueryStringDecoder;

/**
 * All possible HTTP commands are expected to extend this class. When a new class is created, it
 * must be added to HttpGetCommand
 *
 * @author Tom Parker
 *
 */
public abstract class HttpBaseCommand {
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
   * Text used as anchor to navigate usage page
   * 
   * @return anchor text
   */
  abstract public String getAnchor();

  /**
   * Command title as displayed on usage page
   * 
   * @return command title
   */
  abstract public String getTitle();

  /**
   * Usage text to be included on usagpage. Embeded HTML is okay.
   * 
   * @return usage text
   */
  abstract public String getUsage(HttpRequest req);


  /**
   * Command as a http file
   * 
   * @return command, including leading /
   */
  abstract public String getCommand();


  /**
   * Do the work. Return response to the browser.
   */
  public abstract void doCommand(ChannelHandlerContext ctx, FullHttpRequest req);

  protected Map<String, List<String>> getParams(FullHttpRequest request) throws UnsupportedMethodException {
    Map<String, List<String>> params = null;
    HttpMethod method = request.getMethod();
    
    if (method == HttpMethod.GET) {
      QueryStringDecoder queryStringDecoder = new QueryStringDecoder(request.getUri());
      params = queryStringDecoder.parameters();
//    } else if (method == HttpMethod.POST) {
      // TODO: support post
      // HttpPostRequestDecoder queryStringDecoder = new HttpPostRequestDecoder(request);
      // Map<String, List<String>> params = queryStringDecoder.;
    } else {
      throw new UnsupportedMethodException("Unsupported HTTP method " + method);
    }
    
    return params;
  }

  protected Map<String, String> getUnaryParams(FullHttpRequest request) throws UnsupportedMethodException {
    Map<String, String> params = new HashMap<String, String>();
    HttpMethod method = request.getMethod();
    
    if (method == HttpMethod.GET) {
      QueryStringDecoder queryStringDecoder = new QueryStringDecoder(request.getUri());
      for (String param : queryStringDecoder.parameters().keySet()) {
        params.put(param, queryStringDecoder.parameters().get(param).get(0));
      }
//    } else if (method == HttpMethod.POST) {
      // TODO: support post
      // HttpPostRequestDecoder queryStringDecoder = new HttpPostRequestDecoder(request);
      // Map<String, List<String>> params = queryStringDecoder.;
    } else {
      throw new UnsupportedMethodException("Unsupported HTTP method " + method);
    }
    
    return params;
  }

  /**
   * 
   * @param t1 start time String
   * @param endTime end time
   * @param mult number of seconds per interval used for relative times. 60 for minutes, 60*60 for
   *        hours, 60*60*24 for days
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
   * @param t time
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
