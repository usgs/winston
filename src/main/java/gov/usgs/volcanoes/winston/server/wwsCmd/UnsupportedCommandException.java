package gov.usgs.volcanoes.winston.server.wwsCmd;

import java.io.IOException;

public class UnsupportedCommandException extends IOException {

  public UnsupportedCommandException(String msg) {
    super(msg);
  }
}
