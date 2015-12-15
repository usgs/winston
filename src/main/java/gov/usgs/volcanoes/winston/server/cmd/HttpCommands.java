package gov.usgs.volcanoes.winston.server.cmd;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import gov.usgs.volcanoes.winston.server.cmd.http.AbstractHttpCommand;
import gov.usgs.volcanoes.winston.server.cmd.http.HttpMenuCommand;

public enum HttpCommands {
  
  MENU("/menu", HttpMenuCommand.class)
  ;
  
  private static final Logger LOGGER = LoggerFactory.getLogger(HttpCommands.class);

  private String command;
  private Class<? extends AbstractHttpCommand> clazz;
  
  private HttpCommands(String command, Class<? extends AbstractHttpCommand> clazz) {
    this.command = command;
    this.clazz = clazz;
  }
  
  public static AbstractHttpCommand get(String command) throws InstantiationException, IllegalAccessException {
    for (HttpCommands cmd : HttpCommands.values()) {
      if (cmd.command.equals(command)) {
          return cmd.clazz.newInstance();
      }
    }
    throw new RuntimeException("Unknown HTTP command " + command);
  }
}
