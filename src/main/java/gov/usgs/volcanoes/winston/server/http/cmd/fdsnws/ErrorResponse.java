package gov.usgs.volcanoes.winston.server.http.cmd.fdsnws;

import java.nio.charset.Charset;
import java.util.Date;

import io.netty.buffer.Unpooled;
import io.netty.channel.ChannelHandlerContext;
import io.netty.handler.codec.http.DefaultFullHttpResponse;
import io.netty.handler.codec.http.FullHttpRequest;
import io.netty.handler.codec.http.FullHttpResponse;
import io.netty.handler.codec.http.HttpHeaders;
import io.netty.handler.codec.http.HttpResponseStatus;

public class ErrorResponse {

  private final ChannelHandlerContext ctx;
  private FullHttpRequest request;
  private HttpResponseStatus status;
  private String shortDescription;
  private String detailedDescription;
  private String version;

  public ErrorResponse(ChannelHandlerContext ctx) {
    this.ctx = ctx;
  }

  public ErrorResponse request(FullHttpRequest request) {
    this.request = request;
    return this;
  }

  public ErrorResponse status(HttpResponseStatus status) {
    this.status = status;
    return this;
  }

  public ErrorResponse shortDescription(String shortDescription) {
    this.shortDescription = shortDescription;
    return this;
  }

  public ErrorResponse detailedDescription(String detailedDescription) {
    this.detailedDescription = detailedDescription;
    return this;
  }

  public ErrorResponse version(String version) {
    this.version = version;
    return this;
  }

  public void sendError() {
    if (request == null || status == null || shortDescription == null || detailedDescription == null
        || version == null || status == null)
      throw new IllegalStateException();

    StringBuffer sb = new StringBuffer();
    sb.append("Error ").append(status.code()).append(": ").append(shortDescription);
    sb.append("\n\n");
    sb.append(detailedDescription);
    sb.append("\n\n");
    sb.append("Request:\n").append(request.getUri());
    sb.append("\n\n");
    sb.append("Request Submitted:\n").append(FdsnwsDate.toString(new Date()));
    sb.append("\n\n");
    sb.append("Service version:\n").append(version);
    String html = sb.toString();

    FullHttpResponse response = new DefaultFullHttpResponse(request.getProtocolVersion(), status,
        Unpooled.copiedBuffer(html, Charset.forName("UTF-8")));
    response.headers().set(HttpHeaders.Names.CONTENT_LENGTH, html.length());
    response.headers().set(HttpHeaders.Names.CONTENT_TYPE, "text/plain; charset=UTF-8");

    if (HttpHeaders.isKeepAlive(request)) {
      response.headers().set(HttpHeaders.Names.CONNECTION, HttpHeaders.Values.KEEP_ALIVE);
    }
    ctx.writeAndFlush(response);
  }
}
