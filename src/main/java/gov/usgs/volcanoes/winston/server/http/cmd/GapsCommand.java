/**
 * I waive copyright and related rights in the this work worldwide through the CC0 1.0 Universal
 * public domain dedication. https://creativecommons.org/publicdomain/zero/1.0/legalcode
 */

package gov.usgs.volcanoes.winston.server.http.cmd;

import java.io.IOException;
import java.io.StringWriter;
import java.io.Writer;
import java.nio.charset.Charset;
import java.text.SimpleDateFormat;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.TimeZone;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import freemarker.template.Template;
import freemarker.template.TemplateException;
import gov.usgs.volcanoes.core.data.Scnl;
import gov.usgs.volcanoes.core.time.J2kSec;
import gov.usgs.volcanoes.core.time.Time;
import gov.usgs.volcanoes.core.util.StringUtils;
import gov.usgs.volcanoes.core.util.UtilException;
import gov.usgs.volcanoes.winston.db.Data;
import gov.usgs.volcanoes.winston.db.WinstonDatabase;
import gov.usgs.volcanoes.winston.server.WinstonConsumer;
import gov.usgs.volcanoes.winston.server.http.HttpBaseCommand;
import gov.usgs.volcanoes.winston.server.http.HttpConstants;
import gov.usgs.volcanoes.winston.server.http.HttpTemplateConfiguration;
import gov.usgs.volcanoes.winston.server.http.UnsupportedMethodException;
import io.netty.buffer.Unpooled;
import io.netty.channel.ChannelHandlerContext;
import io.netty.handler.codec.http.DefaultFullHttpResponse;
import io.netty.handler.codec.http.DefaultHttpResponse;
import io.netty.handler.codec.http.FullHttpRequest;
import io.netty.handler.codec.http.FullHttpResponse;
import io.netty.handler.codec.http.HttpHeaders;
import io.netty.handler.codec.http.HttpResponse;
import io.netty.handler.codec.http.HttpResponseStatus;

/**
 * Return the wave server menu. Similar to earthworm getmenu command.
 *
 * @author Tom Parker
 *
 */
public final class GapsCommand extends HttpBaseCommand {

  private static final Logger LOGGER = LoggerFactory.getLogger(GapsCommand.class);

  private Scnl scnl;
  private Double endTime;
  private Double startTime;
  private String timeZone;
  private TimeZone zone;
  private Double minGapDuration;
  private int writeComputer;

  private String startTimeS;
  private String endTimeE;
  private double totalTime;

  private List<double[]> gaps;
  private int gapCount;

  private double gapLength;
  private double totalGapLength;

  private ChannelHandlerContext ctx;
  private FullHttpRequest request;

  /**
   * Constructor.
   */
  public GapsCommand() {
    super();
  }


  public void doCommand(ChannelHandlerContext ctx, FullHttpRequest request) throws UtilException {
    this.ctx = ctx;
    this.request = request;

    StringBuffer error = new StringBuffer();

    Map<String, String> params;
    try {
      params = getUnaryParams(request);
    } catch (UnsupportedMethodException e) {
      // TODO return 501 error
      e.printStackTrace();
      return;
    }

    // Station
    String code = params.get("code");
    if (code == null) {
      error.append("Error: you must specify a channel (code).");
    } else {
      scnl = Scnl.parse(code, "_");
    }

    // End Time
    endTime = getEndTime(params.get("t2"));
    if (endTime.isNaN()) {
      error.append("Error: could not parse end time (t2). Should be ")
          .append(HttpConstants.INPUT_DATE_FORMAT).append(".");
    }

    // Start Time
    startTime = getStartTime(params.get("t1"), endTime, HttpConstants.ONE_HOUR_S);
    if (startTime.isNaN()) {
      error.append("Error: could not parse start time (t1). Should be ")
          .append(HttpConstants.INPUT_DATE_FORMAT).append(" or -HH.");
    }


    // TimeZone
    timeZone = StringUtils.stringToString(params.get("tz"), "UTC");
    zone = TimeZone.getTimeZone(timeZone);

    // Minimum Gap Duration
    minGapDuration = StringUtils.stringToDouble(params.get("mgd"), 5);

    // Write Computer
    writeComputer = StringUtils.stringToInt(params.get("wc"), 0);

    if (error.length() > 0) {
      HttpResponse response =
          new DefaultHttpResponse(request.getProtocolVersion(), HttpResponseStatus.BAD_REQUEST);
      response.headers().set(HttpHeaders.Names.CONTENT_TYPE, "text/html; charset=UTF-8");

      boolean keepAlive = HttpHeaders.isKeepAlive(request);

      if (keepAlive) {
        response.headers().set(HttpHeaders.Names.CONTENT_LENGTH, error.length());
        response.headers().set(HttpHeaders.Names.CONNECTION, HttpHeaders.Values.KEEP_ALIVE);
      }
      ctx.writeAndFlush(error);
    } else {
      SimpleDateFormat dateF = new SimpleDateFormat(Time.STANDARD_TIME_FORMAT);
      dateF.setTimeZone(zone);
      // Finds Gaps
      // get and sort menu
      try {
        gaps = databasePool.doCommand(new WinstonConsumer<List<double[]>>() {

          public List<double[]> execute(WinstonDatabase winston) throws UtilException {
            return new Data(winston).findGaps(scnl, startTime, endTime);
          }
        });
      } catch (Exception e) {
        throw new UtilException(e.getMessage());
      }

      startTimeS = dateF.format(J2kSec.asEpoch(startTime));
      endTimeE = dateF.format(J2kSec.asEpoch(startTime));
      totalTime = endTime - startTime;

      if (writeComputer == 1) {
        writeComputer();
      } else {
        writeHuman();
      }
    }
  }

  // Write for Humans
  private void writeHuman() {
    Map<String, Object> root = new HashMap<String, Object>();
    root.put("startTime", J2kSec.asEpoch(startTime));
    root.put("endTime", J2kSec.asEpoch(endTime));
    root.put("timeZone", HttpConstants.TIME_ZONE);
    root.put("channel", scnl);
    root.put("minimumGapDuration", minGapDuration);
    root.put("totalTime", totalTime);

    double totalGap = 0;
    for (double[] gap : gaps) {
      totalGap += gap[1] - gap[0];
    }
    root.put("totalGap", totalGap);

    for (double[] gap : gaps) {
      gap[0] = J2kSec.asEpoch(gap[0]);
      gap[1] = J2kSec.asEpoch(gap[1]);
    }
    root.put("gaps", gaps);

    try {
      HttpTemplateConfiguration cfg = HttpTemplateConfiguration.getInstance();
      Template template = cfg.getTemplate("www/gaps.ftl");
      Writer sw = new StringWriter();
      template.process(root, sw);
      String html = sw.toString();
      sw.close();

      FullHttpResponse response = new DefaultFullHttpResponse(request.getProtocolVersion(),
          HttpResponseStatus.OK, Unpooled.copiedBuffer(html, Charset.forName("UTF-8")));
      response.headers().set(HttpHeaders.Names.CONTENT_LENGTH, html.length());
      response.headers().set(HttpHeaders.Names.CONTENT_TYPE, "text/html; charset=UTF-8");

      if (HttpHeaders.isKeepAlive(request)) {
        response.headers().set(HttpHeaders.Names.CONNECTION, HttpHeaders.Values.KEEP_ALIVE);
      }
      ctx.writeAndFlush(response);
    } catch (IOException e) {
      LOGGER.error(e.getLocalizedMessage());
    } catch (TemplateException e) {
      LOGGER.error(e.getLocalizedMessage());
    }
  }

  // Write for Computers
  private void writeComputer() {

    final StringBuilder header = new StringBuilder();
    final StringBuilder output = new StringBuilder();
    output.append("# Gap Start\t\tGap End\t\t\tDuration\n");

    // Write Computer Data Gaps
    gapCount = 0;
    totalGapLength = 0;

    for (final double[] gap : gaps) {
      gapLength = gap[1] - gap[0];
      if (gapLength < minGapDuration) {
        continue;
      } else {
        output.append(gap[0] + "\t" + gap[1] + "\t" + gapLength + "\n");
        gapCount++;
        totalGapLength = totalGapLength + gapLength;
      }
    }

    // Write Computer Winston Gaps
    header.append("# Start Time: " + startTimeS + " (" + startTime + ")\n# End Time: " + endTimeE
        + " (" + endTime + ")\n# Total Time: " + totalTime + "\n# Time Zone: " + timeZone
        + "\n# Station: " + scnl + "\n# Minumum Gap Duration: " + minGapDuration + "\n# GapCount: "
        + gapCount + "\n");

    header.append(output);

    FullHttpResponse response = new DefaultFullHttpResponse(request.getProtocolVersion(),
        HttpResponseStatus.OK, Unpooled.copiedBuffer(header, Charset.forName("UTF-8")));
    response.headers().set(HttpHeaders.Names.CONTENT_LENGTH, header.length());
    response.headers().set(HttpHeaders.Names.CONTENT_TYPE, "text/html; charset=UTF-8");

    if (HttpHeaders.isKeepAlive(request)) {
      response.headers().set(HttpHeaders.Names.CONNECTION, HttpHeaders.Values.KEEP_ALIVE);
    }
    ctx.writeAndFlush(response);
  }
}
