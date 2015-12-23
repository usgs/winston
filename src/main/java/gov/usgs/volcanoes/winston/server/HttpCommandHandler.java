/**
 * I waive copyright and related rights in the this work worldwide through the CC0 1.0 Universal
 * public domain dedication. https://creativecommons.org/publicdomain/zero/1.0/legalcode
 */

package gov.usgs.volcanoes.winston.server;

import java.io.IOException;
import java.io.InputStream;
import java.io.StringWriter;
import java.io.Writer;
import java.net.URL;
import java.nio.channels.SocketChannel;
import java.nio.charset.Charset;
import java.nio.file.Files;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.TimeZone;

import org.apache.log4j.lf5.util.ResourceUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.apache.commons.io.IOUtils;
import freemarker.template.Template;
import freemarker.template.TemplateException;
import gov.usgs.volcanoes.core.configfile.ConfigFile;
import gov.usgs.volcanoes.core.util.UtilException;
import gov.usgs.volcanoes.winston.Channel;
import gov.usgs.volcanoes.winston.Version;
import gov.usgs.volcanoes.winston.db.Channels;
import gov.usgs.volcanoes.winston.db.WinstonDatabase;
import gov.usgs.volcanoes.winston.legacyServer.cmd.http.AbstractHttpCommand;
import gov.usgs.volcanoes.winston.server.httpCmd.HttpBaseCommand;
import gov.usgs.volcanoes.winston.server.httpCmd.HttpCommand;
import gov.usgs.volcanoes.winston.server.httpCmd.HttpCommandFactory;
import gov.usgs.volcanoes.winston.server.httpCmd.HttpConstants;
import gov.usgs.volcanoes.winston.server.httpCmd.HttpTemplateConfiguration;
import gov.usgs.volcanoes.winston.server.httpCmd.MimeType;
import gov.usgs.volcanoes.winston.server.wwsCmd.WinstonConsumer;
import io.netty.buffer.Unpooled;
import io.netty.channel.ChannelFutureListener;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.SimpleChannelInboundHandler;
import io.netty.handler.codec.http.DefaultFullHttpResponse;
import io.netty.handler.codec.http.DefaultHttpResponse;
import io.netty.handler.codec.http.FullHttpRequest;
import io.netty.handler.codec.http.FullHttpResponse;
import io.netty.handler.codec.http.HttpHeaders;
import io.netty.handler.codec.http.HttpResponse;
import io.netty.handler.codec.http.HttpResponseStatus;
import io.netty.util.AttributeKey;
import io.netty.util.concurrent.Future;
import io.netty.util.concurrent.FutureListener;

/**
 * Derived from HttpSnoopServerHandler
 *
 * @author Tom Parker
 *
 */
public class HttpCommandHandler extends SimpleChannelInboundHandler<FullHttpRequest> {

  private static final Logger LOGGER = LoggerFactory.getLogger(HttpCommandHandler.class);

  private final WinstonDatabasePool winstonDatabasePool;

  private static final AttributeKey<ConnectionStatistics> connectionStatsKey;

  static {
    connectionStatsKey = AttributeKey.valueOf("connectionStatistics");
  }

  public HttpCommandHandler(ConfigFile configFile, WinstonDatabasePool winstonDatabasePool) {
    this.winstonDatabasePool = winstonDatabasePool;
  }

  @Override
  public void channelRead0(ChannelHandlerContext ctx, FullHttpRequest req) throws Exception {

    LOGGER.info("Received HTTP req: {}", req.getUri());
    String command = req.getUri().substring(1);
    if (command.length() == 0) {
      sendUsage(ctx, req);
    } else {

      // HTTP command
      try {
        final HttpBaseCommand httpWorker = HttpCommandFactory.get(winstonDatabasePool, command);
        httpWorker.respond(ctx, req);
        ConnectionStatistics connectionStatistics = ctx.channel().attr(connectionStatsKey).get();
        connectionStatistics.incrHttpCount(ctx.channel().remoteAddress());

        // send file
      } catch (MalformedCommandException e) {
        String uri = req.getUri();

        InputStream is = this.getClass().getClassLoader().getResourceAsStream("www/" + command);

        if (is == null) {
          send404(ctx, req);
        } else {
          final String mimeType = MimeType.guessMimeType(command);
          sendFile(ctx, req, mimeType, is);
        }
      }
    }

    // If keep-alive is not set, close the connection once the content is fully written.
    if (!HttpHeaders.isKeepAlive(req)) {
      ctx.writeAndFlush(Unpooled.EMPTY_BUFFER).addListener(ChannelFutureListener.CLOSE);
    }
  }

  private void sendFile(ChannelHandlerContext ctx, FullHttpRequest req, String mimeType,
      InputStream is) throws IOException {

    byte[] file = IOUtils.toByteArray(is);

    FullHttpResponse response = new DefaultFullHttpResponse(req.getProtocolVersion(),
        HttpResponseStatus.OK, Unpooled.copiedBuffer(file));
    response.headers().set(HttpHeaders.Names.CONTENT_LENGTH, file.length);
    response.headers().set(HttpHeaders.Names.CONTENT_TYPE, mimeType);

    if (HttpHeaders.isKeepAlive(req)) {
      response.headers().set(HttpHeaders.Names.CONNECTION, HttpHeaders.Values.KEEP_ALIVE);
    }

    ctx.write(response);
  }

  private void send404(ChannelHandlerContext ctx, FullHttpRequest req) {
    String html = "Unknown command.";

    FullHttpResponse response = new DefaultFullHttpResponse(req.getProtocolVersion(),
        HttpResponseStatus.OK, Unpooled.copiedBuffer(html, Charset.forName("UTF-8")));
    response.headers().set(HttpHeaders.Names.CONTENT_LENGTH, html.length());
    response.headers().set(HttpHeaders.Names.CONTENT_TYPE, "text/html; charset=UTF-8");

    ctx.writeAndFlush(response);
  }

  private void sendUsage(ChannelHandlerContext ctx, FullHttpRequest req) throws UtilException {
    HttpTemplateConfiguration cfg = HttpTemplateConfiguration.getInstance();
    List<Channel> channels;
    try {
      channels = winstonDatabasePool.doCommand(new WinstonConsumer<List<Channel>>() {

        public List<Channel> execute(WinstonDatabase winston) throws UtilException {
          return new Channels(winston).getChannels();
        }

      });
    } catch (Exception e) {
      throw new UtilException(e.getMessage());
    }

    Map<String, Object> root = new HashMap<String, Object>();

    root.put("timeZones", TimeZone.getAvailableIDs());
    root.put("channels", channels);
    root.put("httpCommands", HttpCommandFactory.values());
    root.put("versionString", Version.VERSION_STRING);
    root.put("host", ctx.channel().localAddress().toString().substring(1));
    HttpConstants.applyDefaults(root);

    try {
      Template template = cfg.getTemplate("usage.ftl");
      Writer sw = new StringWriter();
      template.process(root, sw);
      String html = sw.toString();
      sw.close();

      FullHttpResponse response = new DefaultFullHttpResponse(req.getProtocolVersion(),
          HttpResponseStatus.OK, Unpooled.copiedBuffer(html, Charset.forName("UTF-8")));
      response.headers().set(HttpHeaders.Names.CONTENT_LENGTH, html.length());

      if (HttpHeaders.isKeepAlive(req)) {
        response.headers().set(HttpHeaders.Names.CONNECTION, HttpHeaders.Values.KEEP_ALIVE);
      }
      ctx.writeAndFlush(response);
    } catch (IOException e) {
      LOGGER.error(e.getLocalizedMessage());
    } catch (TemplateException e) {
      LOGGER.error(e.getLocalizedMessage());
    }
  }

  @Override
  public void channelReadComplete(ChannelHandlerContext ctx) {
    ctx.flush();
  }

  @Override
  public void exceptionCaught(ChannelHandlerContext ctx, Throwable cause) {
    cause.printStackTrace();
    ctx.close();
  }
}
