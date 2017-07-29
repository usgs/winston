/**
 * I waive copyright and related rights in the this work worldwide through the CC0 1.0 Universal
 * public domain dedication. https://creativecommons.org/publicdomain/zero/1.0/legalcode
 */

package gov.usgs.volcanoes.winston.server.wws;

import gov.usgs.volcanoes.winston.server.WinstonDatabasePool;
import gov.usgs.volcanoes.winston.server.wws.cmd.GetChannelsCommand;
import gov.usgs.volcanoes.winston.server.wws.cmd.GetMetadataCommand;
import gov.usgs.volcanoes.winston.server.wws.cmd.GetScnCommand;
import gov.usgs.volcanoes.winston.server.wws.cmd.GetScnRawCommand;
import gov.usgs.volcanoes.winston.server.wws.cmd.GetScnlCommand;
import gov.usgs.volcanoes.winston.server.wws.cmd.GetScnlHeliRawCommand;
import gov.usgs.volcanoes.winston.server.wws.cmd.GetScnlRawCommand;
import gov.usgs.volcanoes.winston.server.wws.cmd.GetScnlRsamRawCommand;
import gov.usgs.volcanoes.winston.server.wws.cmd.GetWaveRawCommand;
import gov.usgs.volcanoes.winston.server.wws.cmd.MenuCommand;
import gov.usgs.volcanoes.winston.server.wws.cmd.StatusCommand;
import gov.usgs.volcanoes.winston.server.wws.cmd.VersionCommand;

/**
 * 
 * @author Tom Parker
 *
 */
public enum WwsCommandFactory {
  
  /** menu */
  MENU(MenuCommand.class), 
  
  /** version */
  VERSION(VersionCommand.class), 
  
  /** getchannels */
  GETCHANNELS(GetChannelsCommand.class), 
  
  /** getmetadata */
  GETMETADATA(GetMetadataCommand.class), 
  
  /** get heli */
  GETSCNLHELIRAW(GetScnlHeliRawCommand.class), 
  
  /** returns a wave */
  GETWAVERAW(GetWaveRawCommand.class), 
  
  /** status */
  STATUS(StatusCommand.class), 
  
  /** get rsam */
  GETSCNLRSAMRAW(GetScnlRsamRawCommand.class), 
  
  /** get scn tracebufs */
  GETSCNRAW(GetScnRawCommand.class),
  
  /** get scnl tracebufs */
  GETSCNLRAW(GetScnlRawCommand.class),
  
  /** get scn samples */
  GETSCN(GetScnCommand.class),
  
  /** get scnl samples */
  GETSCNL(GetScnlCommand.class),
  ;

  private Class<? extends WwsBaseCommand> clazz;

  private WwsCommandFactory(Class<? extends WwsBaseCommand> clazz) {
    this.clazz = clazz;
  }

  /**
   * Return the appropriate initialized object.
   * 
   * @param databasePool database pool
   * @param command the WWS command
   * @return the initialized object
   * @throws InstantiationException when I cannot create object
   * @throws IllegalAccessException when I cannot create object
   * @throws UnsupportedCommandException when no classes can service the command
   */
  public static WwsBaseCommand get(WinstonDatabasePool databasePool, WwsCommandString command)
      throws InstantiationException, IllegalAccessException, UnsupportedCommandException {
    for (WwsCommandFactory cmd : WwsCommandFactory.values()) {
      if (cmd.toString().equals(command.command)) {
        WwsBaseCommand baseCommand = cmd.clazz.newInstance();
        baseCommand.databasePool(databasePool);
        return baseCommand;
      }
    }
    throw new UnsupportedCommandException("Unknown WWS command " + command.command);
  }
}
