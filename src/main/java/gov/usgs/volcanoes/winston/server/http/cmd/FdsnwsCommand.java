/**
 * I waive copyright and related rights in the this work worldwide through the CC0 1.0 Universal
 * public domain dedication. https://creativecommons.org/publicdomain/zero/1.0/legalcode
 */

package gov.usgs.volcanoes.winston.server.http.cmd;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import gov.usgs.volcanoes.core.util.UtilException;
import gov.usgs.volcanoes.winston.server.http.HttpBaseCommand;
import gov.usgs.volcanoes.winston.server.http.cmd.fdsnws.DataselectService;
import gov.usgs.volcanoes.winston.server.http.cmd.fdsnws.ErrorResponse;
import gov.usgs.volcanoes.winston.server.http.cmd.fdsnws.FdsnwsRequest;
import gov.usgs.volcanoes.winston.server.http.cmd.fdsnws.StationService;
import io.netty.channel.ChannelHandlerContext;
import io.netty.handler.codec.http.FullHttpRequest;
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
