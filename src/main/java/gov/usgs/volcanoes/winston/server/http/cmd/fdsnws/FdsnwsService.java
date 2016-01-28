package gov.usgs.volcanoes.winston.server.http.cmd.fdsnws;

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.Charset;

import org.apache.commons.io.IOUtils;

import gov.usgs.volcanoes.core.util.UtilException;
import gov.usgs.volcanoes.winston.server.MalformedCommandException;
import gov.usgs.volcanoes.winston.server.http.MimeType;
import io.netty.buffer.Unpooled;
import io.netty.channel.ChannelHandlerContext;
import io.netty.handler.codec.http.DefaultFullHttpResponse;
import io.netty.handler.codec.http.FullHttpRequest;
import io.netty.handler.codec.http.FullHttpResponse;
import io.netty.handler.codec.http.HttpHeaders;
import io.netty.handler.codec.http.HttpResponseStatus;

public class FdsnwsService {
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
    InputStream is = FdsnwsService.class.getClassLoader()
        .getResourceAsStream("www/" + service + "_application.wadl");
    try {
      byte[] file = IOUtils.toByteArray(is);
      FullHttpResponse response = new DefaultFullHttpResponse(request.getProtocolVersion(),
          HttpResponseStatus.OK, Unpooled.copiedBuffer(file));
      response.headers().set(HttpHeaders.Names.CONTENT_LENGTH, file.length);
      response.headers().set(HttpHeaders.Names.CONTENT_TYPE, "application/xml; charset=UTF-8");

      if (HttpHeaders.isKeepAlive(request)) {
        response.headers().set(HttpHeaders.Names.CONNECTION, HttpHeaders.Values.KEEP_ALIVE);
      }
      ctx.writeAndFlush(response);

    } catch (IOException e) {
      e.printStackTrace();
    }
  }
}
