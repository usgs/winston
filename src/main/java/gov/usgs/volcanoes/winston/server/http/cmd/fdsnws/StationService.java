package gov.usgs.volcanoes.winston.server.http.cmd.fdsnws;

import io.netty.channel.ChannelHandlerContext;
import io.netty.handler.codec.http.FullHttpRequest;
import io.netty.handler.codec.http.HttpResponseStatus;

public class StationService extends FdsnwsService {
  private static final String VERSION = "1.1.2";
  
  static {
    version = VERSION;
  }
  
  public static void dispatch(ChannelHandlerContext ctx, FullHttpRequest request) {
    String method = request.getUri().split("/")[4];

    switch (method) {
      case "version":
        sendVersion(ctx, request);
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
}
