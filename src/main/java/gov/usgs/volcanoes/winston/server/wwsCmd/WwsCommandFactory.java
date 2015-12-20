/**
 * I waive copyright and related rights in the this work worldwide through the CC0 1.0 Universal
 * public domain dedication. https://creativecommons.org/publicdomain/zero/1.0/legalcode
 */

package gov.usgs.volcanoes.winston.server.wwsCmd;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import gov.usgs.volcanoes.winston.server.WinstonDatabasePool;

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
      // GETSCNRAW("GETSCNRAW", StatusCommand.class),
      // GETSCNLRAW("GETSCNLRAW", StatusCommand.class),
      // GETSCN("GETSCN", StatusCommand.class),
      // GETSCNL("GETSCNL", StatusCommand.class),
  ;

  private static final Logger LOGGER = LoggerFactory.getLogger(WwsCommandFactory.class);

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
