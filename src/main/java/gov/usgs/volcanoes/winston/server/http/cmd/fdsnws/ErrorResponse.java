/**
 * I waive copyright and related rights in the this work worldwide through the CC0 1.0 Universal
 * public domain dedication. https://creativecommons.org/publicdomain/zero/1.0/legalcode
 */

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

/**
 * Return a properly formatted error to the client.
 * 
 * @author Tom Parker
 *
 */
public class ErrorResponse {

  private final ChannelHandlerContext ctx;
  private FullHttpRequest request;
  private HttpResponseStatus status;
  private String shortDescription;
  private String detailedDescription;
  private String version;

  /**
   * Constructor.
   * 
   * @param ctx handler context
   */
  public ErrorResponse(ChannelHandlerContext ctx) {
    this.ctx = ctx;
  }

  /**
   * Request mutator.
   * 
   * @param request http request
   * @return this for the fluent fans
   */
  public ErrorResponse request(FullHttpRequest request) {
    this.request = request;
    return this;
  }

  /**
   * status mutator.
   * 
   * @param status response status
   * @return this for the fluent fans
   */
  public ErrorResponse status(HttpResponseStatus status) {
    this.status = status;
    return this;
  }

  /**
   * error mutator.
   * 
   * @param shortDescription short description
   * @return this for the fluent fans
   */
  public ErrorResponse shortDescription(String shortDescription) {
    this.shortDescription = shortDescription;
    return this;
  }

  /**
   * detailed description mutator.
   * 
   * @param detailedDescription detailed description
   * @return this for the fluent fans
   */
  public ErrorResponse detailedDescription(String detailedDescription) {
    this.detailedDescription = detailedDescription;
    return this;
  }

  /**
   * version mutator.
   * 
   * @param version version
   * @return this for the fluent fans
   */
  public ErrorResponse version(String version) {
    this.version = version;
    return this;
  }

  /**
   * send the error, thorwing IllegalState when not properly initalized.
   */
  public void sendError() {
    if (request == null || status == null || shortDescription == null || detailedDescription == null
        || version == null)
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
