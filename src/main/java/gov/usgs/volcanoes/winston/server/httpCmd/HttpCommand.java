package gov.usgs.volcanoes.winston.server.httpCmd;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public enum HttpCommand {
  
  MENU("/menu", HttpMenuCommand.class)
  ;
  
  private static final Logger LOGGER = LoggerFactory.getLogger(HttpCommand.class);

  private String command;
  private Class<? extends HttpBaseCommand> clazz;
  
  private HttpCommand(String command, Class<? extends HttpBaseCommand> clazz) {
    this.command = command;
    this.clazz = clazz;
  }
  
  public static HttpBaseCommand get(String command) throws InstantiationException, IllegalAccessException {
    for (HttpCommand cmd : HttpCommand.values()) {
      if (cmd.command.equals(command)) {
          return cmd.clazz.newInstance();
      }
    }
    throw new RuntimeException("Unknown HTTP command " + command);
  }
}
