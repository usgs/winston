/**
 * I waive copyright and related rights in the this work worldwide through the CC0 1.0 Universal
 * public domain dedication. https://creativecommons.org/publicdomain/zero/1.0/legalcode
 */

package gov.usgs.volcanoes.winston.server.http.cmd;

import java.awt.Color;
import java.nio.charset.Charset;
import java.util.Map;
import java.util.TimeZone;

import gov.usgs.volcanoes.core.data.RSAMData;
import gov.usgs.volcanoes.core.data.Scnl;
import gov.usgs.volcanoes.core.legacy.plot.Plot;
import gov.usgs.volcanoes.core.legacy.plot.PlotException;
import gov.usgs.volcanoes.core.legacy.plot.decorate.DefaultFrameDecorator;
import gov.usgs.volcanoes.core.legacy.plot.decorate.DefaultFrameDecorator.Location;
import gov.usgs.volcanoes.core.legacy.plot.render.MatrixRenderer;
import gov.usgs.volcanoes.core.math.DownsamplingType;
import gov.usgs.volcanoes.core.time.J2kSec;
import gov.usgs.volcanoes.core.util.StringUtils;
import gov.usgs.volcanoes.core.util.UtilException;
import gov.usgs.volcanoes.winston.db.Data;
import gov.usgs.volcanoes.winston.db.WinstonDatabase;
import gov.usgs.volcanoes.winston.server.MalformedCommandException;
import gov.usgs.volcanoes.winston.server.WinstonConsumer;
import gov.usgs.volcanoes.winston.server.http.HttpBaseCommand;
import gov.usgs.volcanoes.winston.server.http.HttpConstants;
import gov.usgs.volcanoes.winston.server.http.UnsupportedMethodException;
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
public final class RsamCommand extends HttpBaseCommand {

  private TimeZone timeZone;
  private int timeZoneOffset;
  private Double endTime;
  private Double startTime;
  private int width;
  private int height;
  private boolean detrend;
  private int rsamPeriod;
  private boolean despike;
  private double despikePeriod;
  private boolean runningMedian;
  private double runningMedianPeriod;
  private double plotMax;
  private double plotMin;
  private boolean outputData;
  private Scnl scnl;
  private RSAMData rsamData;
  private FullHttpRequest request;

  /**
   * Constructor.
   */
  public RsamCommand() {
    super();
  }


  public void doCommand(ChannelHandlerContext ctx, FullHttpRequest request)
      throws UtilException, MalformedCommandException {
    this.request = request;

    Map<String, String> params;
    try {
      params = getUnaryParams(request);
    } catch (UnsupportedMethodException e) {
      throw new MalformedCommandException();
    }


    String errorString = validateParams(params);
    if (errorString.length() > 0) {
      throw new MalformedCommandException(errorString);
    }
    getData();
    if (outputData)
      ctx.write(sendData());
    else
      ctx.write(sendPlot());

  }


  private void getData() throws UtilException {
    rsamData = null;
    try {

      final DownsamplingType dst;
      final int dsInt;
      if (rsamPeriod < 2) {
        dst = DownsamplingType.NONE;
        dsInt = 0;
      } else {
        dst = DownsamplingType.MEAN;
        dsInt = rsamPeriod;
      }

      try {
        rsamData = databasePool.doCommand(new WinstonConsumer<RSAMData>() {

          public RSAMData execute(WinstonDatabase winston) throws UtilException {
            return new Data(winston).getRSAMData(scnl, startTime, endTime, 0, dst, dsInt);
          }
        });
      } catch (Exception e) {
        throw new UtilException(e.getMessage());
      }

      if (rsamData == null)
        throw new UtilException("No RSAM data for " + scnl + " " + startTime + "->" + endTime);
      
      if (rsamData.rows() > 0) {
        rsamData.adjustTime(timeZoneOffset);

        if (despike)
          rsamData.despike(1, despikePeriod);

        if (detrend)
          rsamData.detrend(1);

        if (runningMedian)
          rsamData.set2median(1, runningMedianPeriod);
      } else {
        throw new UtilException(
            "Error: could not get RSAM data, check channel (code). Empty result.");

      }

    } catch (final UtilException e) {
      throw new UtilException(
          "Error: could not get RSAM data, check channel (code). e = " + e.toString());
    }
  }

  private String validateParams(Map<String, String> arguments) throws MalformedCommandException {
    StringBuffer errorString = new StringBuffer();

    String code = arguments.get("code");
    if (code == null)
      errorString.append("Error: you must specify a channel (code).<br>");
    else {
      try {
        scnl = Scnl.parse(code, "_");
      } catch (UtilException e) {
        throw new MalformedCommandException(String.format("Cannot parse code. (%s)", code));
      }
    }

    timeZone = TimeZone.getTimeZone(StringUtils.stringToString(arguments.get("tz"), "UTC"));

    endTime = getEndTime(arguments.get("t2"));
    if (endTime.isNaN())
      errorString.append("Error: could not parse end time (t2). Should be ")
          .append(HttpConstants.INPUT_DATE_FORMAT).append(".<br>");

    startTime = getStartTime(arguments.get("t1"), endTime, HttpConstants.ONE_DAY_S);
    if (startTime.isNaN())
      errorString.append("Error: could not parse start time (t1). Should be ")
          .append(HttpConstants.INPUT_DATE_FORMAT).append(" or -HH.M<br>");

    timeZoneOffset = timeZone.getOffset(J2kSec.asEpoch(endTime));

    width = StringUtils.stringToInt(arguments.get("w"), HttpConstants.RSAM_WIDTH);
    height = StringUtils.stringToInt(arguments.get("h"), HttpConstants.RSAM_HEIGHT);

    // if (height * width <= 0 || height * width > wws.httpMaxSize())
    // errorString += "Error: product of width (w) and height (h) must be between 1 and "
    // + wws.httpMaxSize() + ".<br>";

    detrend = StringUtils.stringToBoolean(arguments.get("dt"), HttpConstants.RSAM_DETREND);

    rsamPeriod = StringUtils.stringToInt(arguments.get("rsamP"), HttpConstants.RSAM_RSAM_PERIOD);

    despike = StringUtils.stringToBoolean(arguments.get("ds"), HttpConstants.RSAM_DOWN_SAMPLE);
    despikePeriod = StringUtils.stringToDouble(arguments.get("dsp"), 0);

    runningMedian = StringUtils.stringToBoolean(arguments.get("rm"), false);
    runningMedianPeriod = StringUtils.stringToDouble(arguments.get("rmp"), 300);

    plotMax = StringUtils.stringToDouble(arguments.get("max"), Double.MAX_VALUE);
    plotMin = StringUtils.stringToDouble(arguments.get("min"), Double.MIN_VALUE);

    outputData = StringUtils.stringToBoolean(arguments.get("csv"), HttpConstants.RSAM_CSV);

    if (errorString.length() != 0) {
      throw new MalformedCommandException();
    }

    return errorString.toString();
  }

  private FullHttpResponse sendPlot() throws UtilException {
    // if (wws.httpRefreshInterval() > 0)
    // response.setHeader("Refresh:", wws.httpRefreshInterval() + "; url=" + request.getResource());

    byte[] png;

    final Plot plot = new Plot();
    plot.setSize(width, height);
    plot.setBackgroundColor(new Color(0.97f, 0.97f, 0.97f));

    final MatrixRenderer mr = new MatrixRenderer(rsamData.getData(), false);
    final double max = Math.min(plotMax, rsamData.max(1) + rsamData.max(1) * .1);
    final double min = Math.max(plotMin, rsamData.min(1) - rsamData.max(1) * .1);

    mr.setExtents(startTime + timeZoneOffset, endTime + timeZoneOffset, min, max);
    mr.setLocation(70, 35, width - 140, height - 70);
    mr.createDefaultAxis();
    mr.setXAxisToTime(8, true, true);

    final String tzText =
        timeZone.getDisplayName(timeZone.inDaylightTime(J2kSec.asDate(endTime)), TimeZone.SHORT);
    final String bottomText =
        "(" + J2kSec.format(HttpConstants.DISPLAY_DATE_FORMAT, startTime + timeZoneOffset) + " to "
            + J2kSec.format(HttpConstants.DISPLAY_DATE_FORMAT, endTime + timeZoneOffset) + " "
            + tzText + ")";

    mr.getAxis().setBottomLabelAsText(bottomText);
    mr.getAxis().setLeftLabelAsText("RSAM");
    DefaultFrameDecorator.addLabel(mr, scnl.toString(" "), Location.LEFT);
    mr.createDefaultLineRenderers(Color.blue);
    // mr.setExtents(startTime, endTime, gdm.min(1), gdm.max(1));
    plot.addRenderer(mr);
    try {
      png = plot.getPNGBytes();
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

    return response;
  }

  private FullHttpResponse sendData() {
    final String html = rsamData.toCSV();
    final String fileName = scnl.toString("_") + "-RSAM.csv";

    FullHttpResponse response = new DefaultFullHttpResponse(request.getProtocolVersion(),
        HttpResponseStatus.OK, Unpooled.copiedBuffer(html, Charset.forName("UTF-8")));
    response.headers().set(HttpHeaders.Names.CONTENT_LENGTH, html.length());
    response.headers().set(HttpHeaders.Names.CONTENT_TYPE, "text/csv; charset=utf-8");
    response.headers().set("content-Disposition", "attachment; filename='" + fileName + "'");

    if (HttpHeaders.isKeepAlive(request)) {
      response.headers().set(HttpHeaders.Names.CONNECTION, HttpHeaders.Values.KEEP_ALIVE);
    }
    return response;
  }

}
