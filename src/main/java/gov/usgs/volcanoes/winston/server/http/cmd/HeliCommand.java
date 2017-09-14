/**
 * I waive copyright and related rights in the this work worldwide through the CC0 1.0 Universal
 * public domain dedication. https://creativecommons.org/publicdomain/zero/1.0/legalcode
 */

package gov.usgs.volcanoes.winston.server.http.cmd;

import java.util.Map;
import java.util.TimeZone;

import gov.usgs.plot.HelicorderSettings;
import gov.usgs.plot.PlotException;
import gov.usgs.plot.data.HelicorderData;
import gov.usgs.volcanoes.core.data.Scnl;
import gov.usgs.volcanoes.core.util.StringUtils;
import gov.usgs.volcanoes.core.util.UtilException;
import gov.usgs.volcanoes.winston.db.Data;
import gov.usgs.volcanoes.winston.db.WinstonDatabase;
import gov.usgs.volcanoes.winston.server.MalformedCommandException;
import gov.usgs.volcanoes.winston.server.http.HttpBaseCommand;
import gov.usgs.volcanoes.winston.server.http.HttpConstants;
import gov.usgs.volcanoes.winston.server.http.UnsupportedMethodException;
import gov.usgs.volcanoes.winston.server.wws.WinstonConsumer;
import io.netty.buffer.Unpooled;
import io.netty.channel.ChannelHandlerContext;
import io.netty.handler.codec.http.DefaultFullHttpResponse;
import io.netty.handler.codec.http.FullHttpRequest;
import io.netty.handler.codec.http.FullHttpResponse;
import io.netty.handler.codec.http.HttpHeaders;
import io.netty.handler.codec.http.HttpResponseStatus;

/**
 * Return the wave server menu. Similar to earthworm getmenu command.
 *
 * @author Tom Parker
 *
 */
public final class HeliCommand extends HttpBaseCommand {

  private static final int MAX_HOURS = 144;
  private static final int MIN_HOURS = 1;
  private static final double MAX_TC = 21600;

  /**
   * Constructor.
   */
  public HeliCommand() {
    super();
  }


  public void doCommand(ChannelHandlerContext ctx, FullHttpRequest request)
      throws UtilException, MalformedCommandException {

    Map<String, String> params;
    try {
      params = getUnaryParams(request);
    } catch (UnsupportedMethodException e) {
      throw new MalformedCommandException();
    }

    final HelicorderSettings settings = validateParams(params);
    final Scnl scnl = Scnl.parse(settings.channel);
    final double startTime = settings.startTime;
    final double endTime = settings.endTime;

    HelicorderData heliData = null;
    try {
      heliData = databasePool.doCommand(new WinstonConsumer<HelicorderData>() {

        public HelicorderData execute(WinstonDatabase winston) throws UtilException {
          return new Data(winston).getHelicorderData(scnl, startTime, endTime, 0);
        }
      });
    } catch (Exception e) {
      throw new UtilException(e.getMessage());
    }

    if (heliData == null || heliData.rows() <= 0) {
      throw new UtilException("Error: could not get helicorder data, check channel (code).");
    }

    byte[] png = null;
    try {
      png = settings.createPlot(heliData).getPNGBytes();
    } catch (PlotException e) {
      throw new UtilException(e.getLocalizedMessage());
    }

    FullHttpResponse response = new DefaultFullHttpResponse(request.getProtocolVersion(),
        HttpResponseStatus.OK, Unpooled.copiedBuffer(png));
    response.headers().set(HttpHeaders.Names.CONTENT_LENGTH, png.length);
    response.headers().set(HttpHeaders.Names.CONTENT_TYPE, "image/png");

    int httpRefreshInterval = configFile.getInt("wws.httpRefreshInterval", -1);
    if (httpRefreshInterval > 0)
      response.headers().set("Refresh", httpRefreshInterval + "; url=" + request.getUri());

    if (HttpHeaders.isKeepAlive(request)) {
      response.headers().set(HttpHeaders.Names.CONNECTION, HttpHeaders.Values.KEEP_ALIVE);
    }
    ctx.write(response);
  }


  private HelicorderSettings validateParams(Map<String, String> params)
      throws MalformedCommandException {
    new HelicorderSettings();
    HelicorderSettings settings = new HelicorderSettings();
    StringBuffer error = new StringBuffer();
    settings.channel = params.get("code");
    if (settings.channel == null) {
      error.append("Error: you must specify a channel (code).");
    } else {
      settings.channel = settings.channel.replace('_', '$');
    }

    final String tz = StringUtils.stringToString(params.get("tz"), HttpConstants.TIME_ZONE);
    settings.timeZone = TimeZone.getTimeZone(tz);

    settings.endTime = getEndTime(params.get("t2"));
    if (Double.isNaN(settings.endTime))
      error.append("Error: could not parse end time (t2). Should be ")
          .append(HttpConstants.INPUT_DATE_FORMAT).append('.');

    settings.startTime = getStartTime(params.get("t1"), settings.endTime, HttpConstants.ONE_HOUR_S);
    if (Double.isNaN(settings.startTime))
      error.append("Error: cannot parse start time. Should be ")
          .append(HttpConstants.INPUT_DATE_FORMAT).append(" or -HH. I received ")
          .append(params.get("t1"));

    if (settings.endTime - settings.startTime > MAX_HOURS * HttpConstants.ONE_HOUR_S)
      error.append("Error: Plot must not be more that ").append(MAX_HOURS).append(" hours long");
    else if (settings.endTime - settings.startTime < MIN_HOURS * HttpConstants.ONE_HOUR_S)
      error.append("Error: Plot cannot be less than ").append(MIN_HOURS).append(" hour long");

    settings.timeChunk = StringUtils.stringToDouble(params.get("tc"), HttpConstants.HELI_TIME_CHUNK)
        * HttpConstants.ONE_MINUTE_S;
    if (settings.timeChunk <= 0 || settings.timeChunk > MAX_TC)
      error.append("Error: time chunk (tc) must be greater than 0 and less than ").append(MAX_TC)
          .append(".");

    final int width = StringUtils.stringToInt(params.get("w"), HttpConstants.HELI_WIDTH);
    final int height = StringUtils.stringToInt(params.get("h"), HttpConstants.HELI_HEIGHT);
    settings.setSizeFromPlotSize(width, height);

    // if (settings.height * settings.width <= 0
    // || settings.height * settings.width > wws.httpMaxSize())
    // error = error + "Error: product of width (w) and height (h) must be between 1 and "
    // + wws.httpMaxSize() + ".";

    settings.showClip = StringUtils.stringToBoolean(params.get("sc"), HttpConstants.HELI_SHOW_CLIP);
    settings.forceCenter =
        StringUtils.stringToBoolean(params.get("fc"), HttpConstants.HELI_FORCE_CENTER);
    settings.barRange = StringUtils.stringToInt(params.get("br"), -1);
    settings.clipValue = StringUtils.stringToInt(params.get("cv"), -1);

    settings.largeChannelDisplay =
        StringUtils.stringToBoolean(params.get("lb"), HttpConstants.HELI_LABEL);

    settings.minimumAxis = StringUtils.stringToBoolean(params.get("min"));
    if (settings.minimumAxis)
      settings.setMinimumSizes();

    if (error.length() > 0) {
      throw new MalformedCommandException(error.toString());
    }

    return settings;
  }
}
