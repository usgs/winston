package gov.usgs.volcanoes.winston.server.wwsCmd;

import java.io.IOException;

public class WwsMalformedCommand extends IOException {
  public WwsMalformedCommand(String msg) {
    super(msg);
  }
}
