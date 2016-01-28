/**
 * I waive copyright and related rights in the this work worldwide through the CC0 1.0 Universal
 * public domain dedication. https://creativecommons.org/publicdomain/zero/1.0/legalcode
 */

package gov.usgs.volcanoes.winston.server.http.cmd;

import java.io.IOException;
import java.io.StringWriter;
import java.io.Writer;
import java.nio.charset.Charset;
import java.util.HashMap;
import java.util.Map;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import freemarker.template.Template;
import freemarker.template.TemplateException;
import gov.usgs.volcanoes.core.time.J2kSec;
import gov.usgs.volcanoes.core.util.UtilException;
import gov.usgs.volcanoes.winston.Version;
import gov.usgs.volcanoes.winston.server.http.HttpBaseCommand;
import gov.usgs.volcanoes.winston.server.http.HttpConstants;
import gov.usgs.volcanoes.winston.server.http.HttpTemplateConfiguration;
import gov.usgs.volcanoes.winston.server.http.cmd.fdsnws.DataselectService;
import gov.usgs.volcanoes.winston.server.http.cmd.fdsnws.ErrorResponse;
import gov.usgs.volcanoes.winston.server.http.cmd.fdsnws.FdsnwsRequest;
import gov.usgs.volcanoes.winston.server.http.cmd.fdsnws.StationService;
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
public final class FdsnwsCommand extends HttpBaseCommand {

  private static final Logger LOGGER = LoggerFactory.getLogger(FdsnwsCommand.class);

  public static final String VERSION = "1.1.2";
 
  /**
   * Constructor.
   */
  public FdsnwsCommand() {
    super();
  }


  public void doCommand(ChannelHandlerContext ctx, FullHttpRequest request) throws UtilException {
    String[] splits = request.getUri().split("/");
    
    if (splits.length == 5) {
      dispatch(splits[2], ctx, request);
    } else if (splits.length == 4) {
      sendUsage(splits[2], ctx, request);
    } else {
      ErrorResponse error = new ErrorResponse(ctx);
      error.request(request);
      error.version(VERSION);
      error.status(HttpResponseStatus.BAD_REQUEST);
      error.shortDescription("Bad request");
      error.detailedDescription("Request cannot be parsed.");
      error.sendError();
    }
  }

  private void sendUsage(String service, ChannelHandlerContext ctx, FullHttpRequest request) {
    Map<String, Object> root = new HashMap<String, Object>();
    root.put("service", service);
    root.put("baseUrl", "http://" + ctx.channel().localAddress().toString().substring(1) + "/");
    root.put("UrlBuilderTemplate", service + "_UrlBuilder");
    root.put("InterfaceDescriptionTemplate", service + "_InterfaceDescription");
    root.put("versionString", Version.VERSION_STRING);


    try {
      HttpTemplateConfiguration cfg = HttpTemplateConfiguration.getInstance();
      Template template = cfg.getTemplate("fdsnws/usage.ftl");
      
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
  
  private void dispatch(String service, ChannelHandlerContext ctx, FullHttpRequest request) {
    ErrorResponse error;
    switch (service) {
      case "dataselect":
        DataselectService.dispatch(ctx, request);
        break;
      case "station":
        StationService.dispatch(ctx, request);
        break;
      case "event":
        error = new ErrorResponse(ctx);
        error.request(request);
        error.version(VERSION);
        error.status(HttpResponseStatus.NOT_IMPLEMENTED);
        error.shortDescription("Not implemented");
        error.detailedDescription("Winston does not implement the event service");
        error.sendError();
        break;
      default:
        error = new ErrorResponse(ctx);
        error.request(request);
        error.version(VERSION);
        error.status(HttpResponseStatus.BAD_REQUEST);
        error.shortDescription("Bad request");
        error.detailedDescription("Request cannot be parsed.");
        error.sendError();
    }
  }
}
