/**
 * I waive copyright and related rights in the this work worldwide through the CC0 1.0 Universal
 * public domain dedication. https://creativecommons.org/publicdomain/zero/1.0/legalcode
 */

package gov.usgs.volcanoes.winston.server.http.cmd.fdsnws;

/**
 * Should you be using ErrorResponse instead?
 * 
 * @author Tom Parker
 *
 */
@Deprecated
public class FdsnException extends Exception {

  /**
   * 
   */
  private static final long serialVersionUID = 1L;
  public final int code;
  public final String message;

  /**
   * Constructor.
   * 
   * @param code code
   * @param message message
   */
  public FdsnException(final int code, final String message) {
    this.code = code;
    this.message = message;
  }
}
