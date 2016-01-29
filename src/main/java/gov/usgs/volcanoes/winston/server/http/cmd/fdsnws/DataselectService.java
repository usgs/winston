/**
 * I waive copyright and related rights in the this work worldwide through the CC0 1.0 Universal
 * public domain dedication. https://creativecommons.org/publicdomain/zero/1.0/legalcode
 */

package gov.usgs.volcanoes.winston.server.http.cmd.fdsnws;

import gov.usgs.volcanoes.winston.server.WinstonDatabasePool;
import io.netty.channel.ChannelHandlerContext;
import io.netty.handler.codec.http.FullHttpRequest;
import io.netty.handler.codec.http.HttpResponseStatus;

/**
 * FDSN WS dataselect service.
 * 
 * @author Tom Parker
 *
 */
public class DataselectService extends FdsnwsService {
  private static final String VERSION = "1.1.2";
  private static final String SERVICE = "dataselect";

  static {
    version = VERSION;
    service = SERVICE;
  }

  /**
   * Constructor.
   * 
   * @param databasePool winston database pool
   * @param ctx handler context
   * @param request the request
   */
  public static void dispatch(WinstonDatabasePool databasePool, ChannelHandlerContext ctx, FullHttpRequest request) {
    String method = request.getUri().split("/")[4];

    switch (method) {
      case "version":
        sendVersion(ctx, request);
        break;
      case "application.wadl":
        sendWadl(ctx, request);
        break;
      case "query":
        sendQueryResponse(databasePool, ctx, request);
        break;
      default:
        ErrorResponse error = new ErrorResponse(ctx);
        error.request(request);
        error.version(VERSION);
        error.status(HttpResponseStatus.BAD_REQUEST);
        error.shortDescription("Bad request");
        error.detailedDescription("Request cannot be parsed.");
        error.sendError();
    }
  }
  private static void sendQueryResponse(WinstonDatabasePool databasePool, ChannelHandlerContext ctx,
      FullHttpRequest request) {
  }
}
