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
  MENU(MenuCommand.class), 
  VERSION(VersionCommand.class), 
  GETCHANNELS(GetChannelsCommand.class), 
  GETMETADATA(GetMetadataCommand.class), 
  GETSCNLHELIRAW(GetScnlHeliRawCommand.class), 
  GETWAVERAW(GetWaveRawCommand.class), 
  STATUS(StatusCommand.class), 
  GETSCNLRSAMRAW(GetScnlRsamRawCommand.class), 
  GETSCNRAW(GetScnRawCommand.class),
  GETSCNLRAW(GetScnlRawCommand.class),
  GETSCN(GetScnCommand.class),
  GETSCNL(GetScnlCommand.class),
  ;

  private Class<? extends WwsBaseCommand> clazz;

  private WwsCommandFactory(Class<? extends WwsBaseCommand> clazz) {
    this.clazz = clazz;
  }

  /**
   * 
   * @param winstonDatabasePool
   * @param command
   * @return
   * @throws InstantiationException
   * @throws IllegalAccessException
   * @throws UnsupportedCommandException
   */
  public static WwsBaseCommand get(WinstonDatabasePool databasePool, WwsCommandString command)
      throws InstantiationException, IllegalAccessException, UnsupportedCommandException {
    for (WwsCommandFactory cmd : WwsCommandFactory.values()) {
      if (cmd.toString().equals(command.getCommand())) {
        WwsBaseCommand baseCommand = cmd.clazz.newInstance();
        baseCommand.databasePool(databasePool);
        return baseCommand;
      }
    }
    throw new UnsupportedCommandException("Unknown WWS command " + command.getCommand());
  }
}
