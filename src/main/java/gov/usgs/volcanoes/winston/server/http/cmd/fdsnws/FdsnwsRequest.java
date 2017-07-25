/**
 * I waive copyright and related rights in the this work worldwide through the CC0 1.0 Universal
 * public domain dedication. https://creativecommons.org/publicdomain/zero/1.0/legalcode
 */

package gov.usgs.volcanoes.winston.server.http.cmd.fdsnws;

import io.netty.handler.codec.http.FullHttpRequest;

/**
 * Immutable class to hold FDSN web services request.
 * 
 * @author Tom Parker
 *
 */
public class FdsnwsRequest {
  private final FullHttpRequest request;
  public final String service;
  public final int majorVersion;
  public final String method;

  /**
   * Constructor.
   * 
   * @param request HTTP request
   */
  public FdsnwsRequest(FullHttpRequest request) {
    this.request = request;

    String[] splits = request.getUri().split("/");
    this.service = splits[2];
    this.majorVersion = Integer.parseInt(splits[3]);
    this.method = splits[4];
  }
}
