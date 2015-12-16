package gov.usgs.volcanoes.winston.server.httpCmd;

import java.io.IOException;

public class UnsupportedMethodException extends IOException {

  public UnsupportedMethodException(String msg) {
    super(msg);
  }
}
