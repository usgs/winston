package gov.usgs.volcanoes.winston.server.wws.cmd;

import gov.usgs.volcanoes.winston.server.MalformedCommandException;
import gov.usgs.volcanoes.winston.server.wws.WwsCommandString;

/**
 * 
 * @author Tom Parker
 *
 */
public class GetScnCommand extends GetScnlCommand {

  /**
   * Constructor.
   */
  public GetScnCommand() {
    super();
  }


  protected void parseCommand(WwsCommandString cmd) throws MalformedCommandException {
    scnl = cmd.getScn();
    timeSpan = cmd.getEwTimeSpan(WwsCommandString.NO_LOCATION);
  }
}
