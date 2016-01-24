/**
 * I waive copyright and related rights in the this work worldwide
 * through the CC0 1.0 Universal public domain dedication.
 * https://creativecommons.org/publicdomain/zero/1.0/legalcode
 */

package gov.usgs.volcanoes.winston.server.http;

import java.util.ArrayList;
import java.util.List;

import gov.usgs.volcanoes.core.util.UtilException;
import gov.usgs.volcanoes.winston.server.MalformedCommandException;
import gov.usgs.volcanoes.winston.server.WinstonDatabasePool;
import gov.usgs.volcanoes.winston.server.http.cmd.GapsCommand;
import gov.usgs.volcanoes.winston.server.http.cmd.HeliCommand;
import gov.usgs.volcanoes.winston.server.http.cmd.MenuCommand;
import gov.usgs.volcanoes.winston.server.http.cmd.RsamCommand;
import gov.usgs.volcanoes.winston.server.http.cmd.StatusCommand;

public enum HttpCommandFactory {

  MENU(MenuCommand.class, "Server Menu"), 
  HELI(HeliCommand.class, "Helicorder"), 
  RSAM(RsamCommand.class, "RSAM"), 
  STATUS(StatusCommand.class, "Server Status"), 
  GAPS(GapsCommand.class, "Data Gaps"),
  ;


  private Class<? extends HttpBaseCommand> clazz;
  private String commandName;

  private HttpCommandFactory(Class<? extends HttpBaseCommand> clazz, String commandName) {
    this.clazz = clazz;
    this.commandName = commandName;
  }

  public Class<? extends HttpBaseCommand> getCommandClass() {
    return clazz;
  }

  public String commandName() {
    return commandName;
  }

  public static HttpBaseCommand get(WinstonDatabasePool databasePool, String command)
      throws UtilException, UnknownCommandException {
    int cmdEnd = command.indexOf('?');
    if (cmdEnd != -1) {
      command = command.substring(0, cmdEnd);
    }
    for (HttpCommandFactory cmd : HttpCommandFactory.values()) {
      if (cmd.toString().toLowerCase().equals(command)) {
        HttpBaseCommand baseCommand;
        try {
          baseCommand = cmd.clazz.newInstance();
        } catch (InstantiationException e) {
          throw new UtilException(e.getLocalizedMessage());
        } catch (IllegalAccessException e) {
          throw new UtilException(e.getLocalizedMessage());
        }
        baseCommand.databasePool(databasePool);
        return baseCommand;
      }
    }
    throw new UnknownCommandException();
  }

  public static List<String> getNames() {
    List<String> names = new ArrayList<String>();
    for (HttpCommandFactory command : HttpCommandFactory.values()) {
      names.add(command.commandName());
    }
    return names;
  }
}
