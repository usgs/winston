/**
 * I waive copyright and related rights in the this work worldwide through the CC0 1.0 Universal
 * public domain dedication. https://creativecommons.org/publicdomain/zero/1.0/legalcode
 */

package gov.usgs.volcanoes.winston.server.http.cmd;

import java.io.IOException;
import java.io.StringWriter;
import java.io.Writer;
import java.nio.charset.Charset;
import java.text.DecimalFormat;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import freemarker.template.Template;
import freemarker.template.TemplateException;
import gov.usgs.volcanoes.core.time.J2kSec;
import gov.usgs.volcanoes.core.util.UtilException;
import gov.usgs.volcanoes.winston.Channel;
import gov.usgs.volcanoes.winston.db.Channels;
import gov.usgs.volcanoes.winston.db.WinstonDatabase;
import gov.usgs.volcanoes.winston.server.ConnectionStatistics;
import gov.usgs.volcanoes.winston.server.http.HttpBaseCommand;
import gov.usgs.volcanoes.winston.server.http.HttpTemplateConfiguration;
import gov.usgs.volcanoes.winston.server.wws.WinstonConsumer;
import io.netty.buffer.Unpooled;
import io.netty.channel.ChannelHandlerContext;
import io.netty.handler.codec.http.DefaultFullHttpResponse;
import io.netty.handler.codec.http.FullHttpRequest;
import io.netty.handler.codec.http.FullHttpResponse;
import io.netty.handler.codec.http.HttpHeaders;
import io.netty.handler.codec.http.HttpResponseStatus;
import io.netty.util.AttributeKey;

/**
 * Return the wave server menu. Similar to earthworm getmenu command.
 *
 * @author Tom Parker
 *
 */
public final class StatusCommand extends HttpBaseCommand {

  private static final Logger LOGGER = LoggerFactory.getLogger(StatusCommand.class);

  private static final AttributeKey<ConnectionStatistics> connectionStatsKey;

  static {
    connectionStatsKey = AttributeKey.valueOf("connectionStatistics");
  }

  /**
   * Constructor.
   */
  public StatusCommand() {
    super();
  }


  public void doCommand(ChannelHandlerContext ctx, FullHttpRequest request) throws UtilException {
    final DecimalFormat formatter = new DecimalFormat("#.##");
    final double now = J2kSec.fromEpoch(System.currentTimeMillis());

    // get and sort menu
    List<Channel> sts;
    try {
      sts = databasePool.doCommand(new WinstonConsumer<List<Channel>>() {

        public List<Channel> execute(WinstonDatabase winston) throws UtilException {
          return new Channels(winston).getChannelsByLastInsert();
        }

      });
    } catch (Exception e) {
      throw new UtilException(e.getMessage());
    }

    final double medianDataAge = now - sts.get(sts.size() / 2).getMaxTime();

    final Map<String, Integer> oneMinChannels = new HashMap<String, Integer>();
    final Map<String, Integer> fiveMinChannels = new HashMap<String, Integer>();
    final Map<String, Integer> oneHourChannels = new HashMap<String, Integer>();
    final Map<String, Integer> oneDayChannels = new HashMap<String, Integer>();
    final Map<String, Integer> oneMonthChannels = new HashMap<String, Integer>();
    final Map<String, Integer> ancientChannels = new HashMap<String, Integer>();

    for (final Channel chan : sts) {
      final double age = now - chan.getMaxTime();
      final String code = chan.getCode().replace('$', '_');
      if (age < 60)
        oneMinChannels.put(code, (int) age);
      else if (age <= 60 * 5)
        fiveMinChannels.put(code, (int) age);
      else if (age <= 60 * 60)
        oneHourChannels.put(code, (int) age);
      else if (age <= 60 * 60 * 24)
        oneDayChannels.put(code, (int) age);
      else if (age <= 60 * 60 * 24 * 7 * 4)
        oneMonthChannels.put(code, (int) age);
      else
        ancientChannels.put(code, (int) age);
    }

    Map<String, Object> root = new HashMap<String, Object>();
    root.put("oneMinChannels", oneMinChannels);
    root.put("fiveMinChannels", fiveMinChannels);
    root.put("oneHourChannels", oneHourChannels);
    root.put("oneDayChannels", oneDayChannels);
    root.put("oneMonthChannels", oneMonthChannels);
    root.put("ancientChannels", ancientChannels);

    root.put("channelCount", sts.size());
    root.put("medianDataAge", formatter.format(medianDataAge));

    ConnectionStatistics connectionStatistics = ctx.channel().attr(connectionStatsKey).get();
    root.put("connectionCount", connectionStatistics.getOpen());

    final Channel chan = sts.get(0);
    root.put("mostRecentChan", chan.getCode().replace('$', '_'));
    root.put("mostRecentTime", formatter.format(now - chan.getMaxTime()));

    try {
      HttpTemplateConfiguration cfg = HttpTemplateConfiguration.getInstance();
      Template template = cfg.getTemplate("www/status.ftl");
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
}
