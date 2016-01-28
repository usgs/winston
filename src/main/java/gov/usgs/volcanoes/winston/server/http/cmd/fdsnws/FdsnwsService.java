package gov.usgs.volcanoes.winston.server.http.cmd.fdsnws;

import java.io.IOException;
import java.io.InputStream;
import java.io.StringWriter;
import java.io.Writer;
import java.nio.charset.Charset;
import java.util.HashMap;
import java.util.Map;

import org.apache.commons.io.IOUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import freemarker.template.Template;
import freemarker.template.TemplateException;
import gov.usgs.volcanoes.winston.server.http.HttpTemplateConfiguration;
import io.netty.buffer.Unpooled;
import io.netty.channel.ChannelHandlerContext;
import io.netty.handler.codec.http.DefaultFullHttpResponse;
import io.netty.handler.codec.http.FullHttpRequest;
import io.netty.handler.codec.http.FullHttpResponse;
import io.netty.handler.codec.http.HttpHeaders;
import io.netty.handler.codec.http.HttpResponseStatus;

public class FdsnwsService {
  private static final Logger LOGGER = LoggerFactory.getLogger(FdsnwsService.class);

  protected static String version;
  protected static String service;

  protected static void sendVersion(ChannelHandlerContext ctx, FullHttpRequest request) {
    FullHttpResponse response = new DefaultFullHttpResponse(request.getProtocolVersion(),
        HttpResponseStatus.OK, Unpooled.copiedBuffer(version, Charset.forName("UTF-8")));
    response.headers().set(HttpHeaders.Names.CONTENT_LENGTH, version.length());
    response.headers().set(HttpHeaders.Names.CONTENT_TYPE, "text/plain; charset=UTF-8");

    if (HttpHeaders.isKeepAlive(request)) {
      response.headers().set(HttpHeaders.Names.CONNECTION, HttpHeaders.Values.KEEP_ALIVE);
    }
    ctx.writeAndFlush(response);
  }

  protected static void sendWadl(ChannelHandlerContext ctx, FullHttpRequest request) {
    Map<String, Object> root = new HashMap<String, Object>();
    root.put("host", ctx.channel().localAddress().toString().substring(1));


    try {
      HttpTemplateConfiguration cfg = HttpTemplateConfiguration.getInstance();
      Template template = cfg.getTemplate("fdsnws/" + service + "_application.ftl");

      Writer sw = new StringWriter();
      template.process(root, sw);
      String xml = sw.toString();
      sw.close();

      FullHttpResponse response = new DefaultFullHttpResponse(request.getProtocolVersion(),
          HttpResponseStatus.OK, Unpooled.copiedBuffer(xml, Charset.forName("UTF-8")));
      response.headers().set(HttpHeaders.Names.CONTENT_LENGTH, xml.length());
      response.headers().set(HttpHeaders.Names.CONTENT_TYPE, "application/xml; charset=UTF-8");

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
