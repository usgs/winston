/**
 * I waive copyright and related rights in the this work worldwide through the CC0 1.0 Universal
 * public domain dedication. https://creativecommons.org/publicdomain/zero/1.0/legalcode
 */

package gov.usgs.volcanoes.winston.server.http;

import java.io.IOException;
import java.io.InputStream;
import java.io.StringWriter;
import java.io.Writer;
import java.nio.charset.Charset;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.TimeZone;

import org.apache.commons.io.IOUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import freemarker.template.Template;
import freemarker.template.TemplateException;
import gov.usgs.volcanoes.core.configfile.ConfigFile;
import gov.usgs.volcanoes.core.util.UtilException;
import gov.usgs.volcanoes.winston.Channel;
import gov.usgs.volcanoes.winston.Version;
import gov.usgs.volcanoes.winston.db.Channels;
import gov.usgs.volcanoes.winston.db.WinstonDatabase;
import gov.usgs.volcanoes.winston.server.ConnectionStatistics;
import gov.usgs.volcanoes.winston.server.MalformedCommandException;
import gov.usgs.volcanoes.winston.server.WinstonDatabasePool;
import gov.usgs.volcanoes.winston.server.wws.WinstonConsumer;
import io.netty.buffer.Unpooled;
import io.netty.channel.ChannelFutureListener;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.SimpleChannelInboundHandler;
import io.netty.handler.codec.http.DefaultFullHttpResponse;
import io.netty.handler.codec.http.FullHttpRequest;
import io.netty.handler.codec.http.FullHttpResponse;
import io.netty.handler.codec.http.HttpHeaders;
import io.netty.handler.codec.http.HttpResponseStatus;
import io.netty.handler.codec.http.HttpVersion;
import io.netty.util.AttributeKey;

/**
 * Receives an HTTP command from the pipeline and passes it to the correct HTTP command class for execution.
 * 
 * Derived from HttpSnoopServerHandler
 *
 * @author Tom Parker
 *
 */
public class HttpCommandHandler extends SimpleChannelInboundHandler<FullHttpRequest> {

  private static final Logger LOGGER = LoggerFactory.getLogger(HttpCommandHandler.class);

  private final WinstonDatabasePool winstonDatabasePool;
  private final ConfigFile configFile;

  private static final AttributeKey<ConnectionStatistics> connectionStatsKey;

  static {
    connectionStatsKey = AttributeKey.valueOf("connectionStatistics");
  }

  /**
   * Constructor.
   * 
   * @param configFile my config file
   * @param winstonDatabasePool my database pool
   */
  public HttpCommandHandler(ConfigFile configFile, WinstonDatabasePool winstonDatabasePool) {
    this.winstonDatabasePool = winstonDatabasePool;
    this.configFile = configFile;
  }

  @Override
  public void channelRead0(ChannelHandlerContext ctx, FullHttpRequest req) throws Exception {

    LOGGER.info("Received HTTP req: {}", req.getUri());
    FullHttpResponse response = null;

    String command = req.getUri().substring(1);
    if (command.length() == 0) {
      response = buildResponse(req.getProtocolVersion(), HttpResponseStatus.OK,
          sendUsage(ctx.channel().localAddress().toString().substring(1)));
    } else {

      // HTTP command
      try {
        final HttpBaseCommand httpWorker = HttpCommandFactory.get(winstonDatabasePool, command);
        httpWorker.setConfig(configFile);
        httpWorker.respond(ctx, req);
        ConnectionStatistics connectionStatistics = ctx.channel().attr(connectionStatsKey).get();
        connectionStatistics.incrHttpCount(ctx.channel().remoteAddress());

        // send file
      } catch (UnknownCommandException e) {
        InputStream is = this.getClass().getClassLoader().getResourceAsStream("www/" + command);

        if (is == null) {
          response =
              buildResponse(req.getProtocolVersion(), HttpResponseStatus.NOT_FOUND, "Not found.");
        } else {
          response = buildResponse(req.getProtocolVersion(), HttpResponseStatus.OK, is);
          final String mimeType = MimeType.guessMimeType(command);
          response.headers().set(HttpHeaders.Names.CONTENT_TYPE, mimeType);
        }
      } catch (MalformedCommandException e) {
        response = buildResponse(req.getProtocolVersion(), HttpResponseStatus.BAD_REQUEST,
            e.getLocalizedMessage());
	LOGGER.debug(e.getLocalizedMessage());
      } catch (UtilException e) {
        LOGGER.debug(e.getLocalizedMessage());
        response = buildResponse(req.getProtocolVersion(), HttpResponseStatus.BAD_REQUEST,
            "" + e.getLocalizedMessage());
      }

    }

    if (response != null) {
      // TODO: icon.ico requests fall here sometimes. Why?
      LOGGER.info("sending error response");
      if (HttpHeaders.isKeepAlive(req)) {
        response.headers().set(HttpHeaders.Names.CONNECTION, HttpHeaders.Values.KEEP_ALIVE);
      }
      ctx.writeAndFlush(response);
    }
    
    // If keep-alive is not set, close the connection once the content is fully written.
    if (!HttpHeaders.isKeepAlive(req)) {
      ctx.writeAndFlush(Unpooled.EMPTY_BUFFER).addListener(ChannelFutureListener.CLOSE);
    }
  }

  private FullHttpResponse buildResponse(HttpVersion httpVersion, HttpResponseStatus status,
      String html) {
    FullHttpResponse response = new DefaultFullHttpResponse(httpVersion, status,
        Unpooled.copiedBuffer(html, Charset.forName("UTF-8")));
    response.headers().set(HttpHeaders.Names.CONTENT_LENGTH, html.length());
    response.headers().set(HttpHeaders.Names.CONTENT_TYPE, "text/html; charset=UTF-8");
    return response;
  }

  private FullHttpResponse buildResponse(HttpVersion httpVersion, HttpResponseStatus status,
      InputStream is) throws IOException {
    byte[] file = IOUtils.toByteArray(is);
    FullHttpResponse response =
        new DefaultFullHttpResponse(httpVersion, status, Unpooled.copiedBuffer(file));
    response.headers().set(HttpHeaders.Names.CONTENT_LENGTH, file.length);
    response.headers().set(HttpHeaders.Names.CONTENT_TYPE, "text/html; charset=UTF-8");
    return response;
  }

  private String sendUsage(String host) throws UtilException {
    List<Channel> channels;
    try {
      channels = winstonDatabasePool.doCommand(new WinstonConsumer<List<Channel>>() {

        public List<Channel> execute(WinstonDatabase winston) throws UtilException {
          Channels channels = new Channels(winston);
          return channels.getChannels();
        }

      });
    } catch (Exception e) {
      LOGGER.error(e.getClass().getName());
      throw new UtilException(e.getMessage());
    }

    List<String> channelNames = new ArrayList<String>();
    for (Channel chan : channels) {
      channelNames.add(chan.scnl.toString("_"));
    }
    
    Map<String, Object> root = new HashMap<String, Object>();

    root.put("timeZones", TimeZone.getAvailableIDs());
    root.put("channels", channelNames);
    root.put("httpCommands", HttpCommandFactory.values());
    root.put("httpCommandNames", HttpCommandFactory.getNames());
    root.put("versionString", Version.VERSION_STRING);
    root.put("host", host);
    HttpConstants.applyDefaults(root);

    String html = null;

    try {
      HttpTemplateConfiguration cfg = HttpTemplateConfiguration.getInstance();
      Template template = cfg.getTemplate("www/usage.ftl");
      Writer sw = new StringWriter();
      template.process(root, sw);
      html = sw.toString();
      sw.close();
    } catch (IOException e) {
      LOGGER.error(e.getLocalizedMessage());
    } catch (TemplateException e) {
      LOGGER.error(e.getLocalizedMessage());
    }

    return html;
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
