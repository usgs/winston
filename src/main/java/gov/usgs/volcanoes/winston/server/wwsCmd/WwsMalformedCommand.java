/**
 * I waive copyright and related rights in the this work worldwide
 * through the CC0 1.0 Universal public domain dedication.
 * https://creativecommons.org/publicdomain/zero/1.0/legalcode
 */

package gov.usgs.volcanoes.winston.server.wwsCmd;

import java.io.IOException;

public class WwsMalformedCommand extends IOException {
  private static final long serialVersionUID = 1L;

  public WwsMalformedCommand(String msg) {
    super(msg);
  }
}
