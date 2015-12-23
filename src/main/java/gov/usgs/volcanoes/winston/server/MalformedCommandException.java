/**
 * I waive copyright and related rights in the this work worldwide
 * through the CC0 1.0 Universal public domain dedication.
 * https://creativecommons.org/publicdomain/zero/1.0/legalcode
 */

package gov.usgs.volcanoes.winston.server;

import java.io.IOException;

public class MalformedCommandException extends IOException {
  private static final long serialVersionUID = 1L;

  public MalformedCommandException() {
    super();
  }

  public MalformedCommandException(String msg) {
    super(msg);
  }
}
