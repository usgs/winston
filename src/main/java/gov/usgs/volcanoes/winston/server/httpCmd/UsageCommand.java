/**
 * I waive copyright and related rights in the this work worldwide through the CC0 1.0 Universal
 * public domain dedication. https://creativecommons.org/publicdomain/zero/1.0/legalcode
 */

package gov.usgs.volcanoes.winston.server.httpCmd;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.io.RandomAccessFile;
import java.io.StringWriter;
import java.io.Writer;
import java.nio.channels.SocketChannel;
import java.text.SimpleDateFormat;
import java.util.Arrays;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

import freemarker.cache.ClassTemplateLoader;
import freemarker.template.Configuration;
import freemarker.template.DefaultObjectWrapper;
import freemarker.template.Template;
import freemarker.template.TemplateException;
import freemarker.template.TemplateExceptionHandler;

import java.util.TimeZone;

import gov.usgs.net.HttpRequest;
import gov.usgs.net.NetTools;
import gov.usgs.volcanoes.core.time.Ew;
import gov.usgs.volcanoes.core.time.Time;
import gov.usgs.volcanoes.core.util.StringUtils;
import gov.usgs.volcanoes.core.util.UtilException;
import gov.usgs.volcanoes.winston.Channel;
import gov.usgs.volcanoes.winston.Version;
import gov.usgs.volcanoes.winston.db.Channels;
import gov.usgs.volcanoes.winston.db.WinstonDatabase;
import gov.usgs.volcanoes.winston.server.wwsCmd.MenuCommand;
import gov.usgs.volcanoes.winston.server.wwsCmd.WinstonConsumer;
import gov.usgs.volcanoes.winston.server.ConnectionStatistics;
import gov.usgs.volcanoes.winston.server.MalformedCommandException;
import io.netty.channel.ChannelFuture;
import io.netty.channel.ChannelFutureListener;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.DefaultFileRegion;
import io.netty.handler.codec.http.DefaultFullHttpResponse;
import io.netty.handler.codec.http.DefaultHttpResponse;
import io.netty.handler.codec.http.FullHttpRequest;
import io.netty.handler.codec.http.FullHttpResponse;
import io.netty.handler.codec.http.HttpHeaders;
import io.netty.handler.codec.http.HttpMethod;
import io.netty.handler.codec.http.HttpResponse;
import io.netty.handler.codec.http.HttpResponseStatus;
import io.netty.handler.codec.http.LastHttpContent;
import io.netty.handler.codec.http.QueryStringDecoder;
import io.netty.handler.codec.http.multipart.HttpPostRequestDecoder;
import io.netty.handler.ssl.SslHandler;
import io.netty.handler.stream.ChunkedNioFile;
import io.netty.util.AttributeKey;

/**
 * Return the wave server menu. Similar to earthworm getmenu command.
 *
 * @author Tom Parker
 *
 */
public final class UsageCommand extends HttpBaseCommand {

  private static int DEFAULT_OB = 2;
  private static String DEFAULT_SO = "a";

  private static final Logger LOGGER = LoggerFactory.getLogger(UsageCommand.class);
  private Configuration cfg;

  public UsageCommand() {
    super();
  }

  public void doCommand(ChannelHandlerContext ctx, FullHttpRequest request) throws UtilException {
    StringBuffer error = new StringBuffer();

    Map<String, Object> root = new HashMap<String, Object>();
//    root.put("timeZones", );
//    root.put("channels", );
//    root.put("httpCommands", );
    root.put("versionString", Version.VERSION_STRING);

    try {
      Template template = cfg.getTemplate("usage.ftl");
      Writer sw = new StringWriter();
      template.process(root, sw);
      String html = sw.toString();
      sw.close();

      HttpResponse httpResponse =
          new DefaultHttpResponse(request.getProtocolVersion(), HttpResponseStatus.OK);
      httpResponse.headers().set(HttpHeaders.Names.CONTENT_TYPE, "text/html; charset=UTF-8");

      boolean keepAlive = HttpHeaders.isKeepAlive(request);

      if (keepAlive) {
        httpResponse.headers().set(HttpHeaders.Names.CONTENT_LENGTH, html.length());
        httpResponse.headers().set(HttpHeaders.Names.CONNECTION, HttpHeaders.Values.KEEP_ALIVE);
      }

      ctx.write(httpResponse);
      ctx.writeAndFlush(html);
    } catch (IOException e) {
      LOGGER.error(e.getLocalizedMessage());
    } catch (TemplateException e) {
      LOGGER.error(e.getLocalizedMessage());
    }
  }

  public String getAnchor() {
    return null;
  }

  public String getTitle() {
    return null;
  }

  public String getUsage(HttpRequest req) {
    return null;
  }

  public String getCommand() {
    return null;
  }
}
