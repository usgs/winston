package gov.usgs.volcanoes.winston.server.wws.cmd;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * 
 * @author Tom Parker
 *
 */
public class GetScnCommand extends GetScnlCommand {

  private static final Logger LOGGER = LoggerFactory.getLogger(GetScnCommand.class);

  /**
   * Constructor.
   */
  public GetScnCommand() {
    super();
    isScnl = false;
  }

}
