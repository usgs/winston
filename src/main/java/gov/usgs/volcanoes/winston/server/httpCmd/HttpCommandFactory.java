/**
 * I waive copyright and related rights in the this work worldwide
 * through the CC0 1.0 Universal public domain dedication.
 * https://creativecommons.org/publicdomain/zero/1.0/legalcode
 */

package gov.usgs.volcanoes.winston.server.httpCmd;

import java.util.ArrayList;
import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import gov.usgs.volcanoes.core.util.UtilException;
import gov.usgs.volcanoes.winston.server.MalformedCommandException;
import gov.usgs.volcanoes.winston.server.WinstonDatabasePool;

public enum HttpCommandFactory {

  MENU(MenuCommand.class, "Server Menu"),
  HELI(HeliCommand.class, "Helicorder"),
  RSAM(RsamCommand.class, "RSAM"),
  STATUS(StatusCommand.class, "Server Status"),
  ;
  
//  addCommand(new HttpGapsCommand(nt, db, wws));
  
//  addCommand(new FdsnDataselectQuery(nt, db, wws));
//  addCommand(new FdsnDataselectVersion(nt, db, wws));
//  addCommand(new FdsnDataselectWadl(nt, db, wws));
//  addCommand(new FdsnDataselectUsage(nt, db, wws));
//  addCommand(new FdsnStationQuery(nt, db, wws));
//  addCommand(new FdsnStationVersion(nt, db, wws));
//  addCommand(new FdsnStationWadl(nt, db, wws));
//  addCommand(new FdsnStationUsage(nt, db, wws));

  private static final Logger LOGGER = LoggerFactory.getLogger(HttpCommandFactory.class);

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
      throws InstantiationException, IllegalAccessException, MalformedCommandException {
    int cmdEnd = command.indexOf('?');
    if (cmdEnd != -1) {
      command = command.substring(0, cmdEnd);
    }
    for (HttpCommandFactory cmd : HttpCommandFactory.values()) {
      if (cmd.toString().toLowerCase().equals(command)) {
        HttpBaseCommand baseCommand = cmd.clazz.newInstance();
        baseCommand.databasePool(databasePool);
        return baseCommand;
      }
    }
    throw new MalformedCommandException();
  }

  public static List<String> getNames() {
    List names = new ArrayList();
    for (HttpCommandFactory command : HttpCommandFactory.values()) {
      names.add(command.commandName());
    }
    return names;
  }
}
