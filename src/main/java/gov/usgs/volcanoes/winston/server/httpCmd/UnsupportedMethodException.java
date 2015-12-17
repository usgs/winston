/**
 * I waive copyright and related rights in the this work worldwide
 * through the CC0 1.0 Universal public domain dedication.
 * https://creativecommons.org/publicdomain/zero/1.0/legalcode
 */

package gov.usgs.volcanoes.winston.server.httpCmd;

import java.io.IOException;

public class UnsupportedMethodException extends IOException {

  public UnsupportedMethodException(String msg) {
    super(msg);
  }
}
