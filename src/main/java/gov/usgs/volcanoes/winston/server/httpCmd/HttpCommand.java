package gov.usgs.volcanoes.winston.server.httpCmd;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import gov.usgs.volcanoes.winston.server.WinstonDatabasePool;

public enum HttpCommand {

  MENU("/menu", HttpMenuCommand.class);

  private static final Logger LOGGER = LoggerFactory.getLogger(HttpCommand.class);

  private String command;
  private Class<? extends HttpBaseCommand> clazz;

  private HttpCommand(String command, Class<? extends HttpBaseCommand> clazz) {
    this.command = command;
    this.clazz = clazz;
  }

  public static HttpBaseCommand get(WinstonDatabasePool databasePool, String command)
      throws InstantiationException, IllegalAccessException {
    int cmdEnd = command.indexOf('?');
    if (cmdEnd != -1) {
      command = command.substring(0, cmdEnd);
    }
    for (HttpCommand cmd : HttpCommand.values()) {
      if (cmd.command.equals(command)) {
        HttpBaseCommand baseCommand = cmd.clazz.newInstance();
        baseCommand.databasePool(databasePool);
        return baseCommand;
      }
    }
    throw new RuntimeException("Unknown HTTP command " + command);
  }
}
