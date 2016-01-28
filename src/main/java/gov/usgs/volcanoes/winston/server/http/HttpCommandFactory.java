/**
 * I waive copyright and related rights in the this work worldwide
 * through the CC0 1.0 Universal public domain dedication.
 * https://creativecommons.org/publicdomain/zero/1.0/legalcode
 */

package gov.usgs.volcanoes.winston.server.http;

import java.util.ArrayList;
import java.util.List;

import gov.usgs.volcanoes.core.util.UtilException;
import gov.usgs.volcanoes.winston.server.WinstonDatabasePool;
import gov.usgs.volcanoes.winston.server.http.cmd.FdsnwsCommand;
import gov.usgs.volcanoes.winston.server.http.cmd.GapsCommand;
import gov.usgs.volcanoes.winston.server.http.cmd.HeliCommand;
import gov.usgs.volcanoes.winston.server.http.cmd.MenuCommand;
import gov.usgs.volcanoes.winston.server.http.cmd.RsamCommand;
import gov.usgs.volcanoes.winston.server.http.cmd.StatusCommand;

/**
 * Factory class for HTTP commands
 * 
 * @author Tom Parker
 *
 */
public enum HttpCommandFactory {

  /** menu */
  MENU(MenuCommand.class, "Server Menu"), 
  
  /** heli */
  HELI(HeliCommand.class, "Helicorder"), 
  
  /** rsam */
  RSAM(RsamCommand.class, "RSAM"), 
  
  /** status */
  STATUS(StatusCommand.class, "Server Status"), 
  
  /** gaps */
  GAPS(GapsCommand.class, "Data Gaps"),
  
  /** FDSNWS */
  FDSNWS(FdsnwsCommand.class, "FDSN Web Service")
  ;


  private Class<? extends HttpBaseCommand> clazz;
  private String commandName;

  private HttpCommandFactory(Class<? extends HttpBaseCommand> clazz, String commandName) {
    this.clazz = clazz;
    this.commandName = commandName;
  }

  /**
   * Class accessor.
   * @return the class
   */
  public Class<? extends HttpBaseCommand> getCommandClass() {
    return clazz;
  }

  /**
   * Name accessor.
   * @return the name
   */
  public String commandName() {
    return commandName;
  }

  /**
   * Instantiate the HTTP command class.
   * 
   * @param databasePool database pool 
   * @param command received command string
   * @return an instantiated command object
   * @throws UtilException when things go wrong
   * @throws UnknownCommandException when no known class can service the request
   */
  public static HttpBaseCommand get(WinstonDatabasePool databasePool, String command)
      throws UtilException, UnknownCommandException {
    int cmdEnd = command.indexOf('?');
    if (cmdEnd != -1) {
      command = command.substring(0, cmdEnd);
    }
    
    cmdEnd = command.indexOf('/');
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

  /**
   * Return list of known command names
   * @return name list
   */
  public static List<String> getNames() {
    List<String> names = new ArrayList<String>();
    for (HttpCommandFactory command : HttpCommandFactory.values()) {
      names.add(command.commandName());
    }
    return names;
  }
}