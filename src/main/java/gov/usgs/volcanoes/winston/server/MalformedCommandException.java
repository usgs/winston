/**
 * I waive copyright and related rights in the this work worldwide through the CC0 1.0 Universal
 * public domain dedication. https://creativecommons.org/publicdomain/zero/1.0/legalcode
 */

package gov.usgs.volcanoes.winston.server;

import java.io.IOException;

/**
 * Exception to be thrown when a malformed command is presented.
 * 
 * @author Tom Parker
 *
 */
public class MalformedCommandException extends IOException {
  private static final long serialVersionUID = 1L;

  /**
   * Constructor.
   */
  public MalformedCommandException() {
    super();
  }

  /**
   * Constructor.
   * 
   * @param msg message
   */
  public MalformedCommandException(String msg) {
    super(msg);
  }
}
