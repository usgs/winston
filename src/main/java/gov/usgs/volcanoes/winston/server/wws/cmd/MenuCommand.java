/**
 * I waive copyright and related rights in the this work worldwide through the CC0 1.0 Universal
 * public domain dedication. https://creativecommons.org/publicdomain/zero/1.0/legalcode
 */

package gov.usgs.volcanoes.winston.server.wws.cmd;

import java.text.DecimalFormat;
import java.util.ArrayList;
import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import gov.usgs.volcanoes.core.data.Scnl;
import gov.usgs.volcanoes.core.time.Ew;
import gov.usgs.volcanoes.core.time.J2kSec;
import gov.usgs.volcanoes.core.time.Time;
import gov.usgs.volcanoes.core.util.UtilException;
import gov.usgs.volcanoes.winston.Channel;
import gov.usgs.volcanoes.winston.db.Channels;
import gov.usgs.volcanoes.winston.db.WinstonDatabase;
import gov.usgs.volcanoes.winston.server.MalformedCommandException;
import gov.usgs.volcanoes.winston.server.wws.WinstonConsumer;
import gov.usgs.volcanoes.winston.server.wws.WwsBaseCommand;
import gov.usgs.volcanoes.winston.server.wws.WwsCommandString;
import io.netty.channel.ChannelHandlerContext;

/**
 * Return the server menu
 * 
 * request = /^MENU:? \d( SCNL)?$/
 *
 * @author Dan Cervelli
 * @author Tom Parker
 */
public class MenuCommand extends WwsBaseCommand {

  private static final Logger LOGGER = LoggerFactory.getLogger(MenuCommand.class);
  private static final int SCNL_ARG = 0;

  /** 
   * Constructor.
   */
  public MenuCommand() {
    super();
  }

  public void doCommand(ChannelHandlerContext ctx, WwsCommandString cmd)
      throws MalformedCommandException, UtilException {

    boolean isScnl = false;

    if (cmd.args != null) {
      if (cmd.args.length == 1 && "SCNL".equals(cmd.args[SCNL_ARG])) {
        isScnl = true;
      }
    } else if (cmd.args != null && cmd.args.length > 1) {
      throw new MalformedCommandException();
    }

    ctx.write(cmd.id + ' ');

    List<Channel> channels;
    try {
      channels = databasePool.doCommand(new WinstonConsumer<List<Channel>>() {

        public List<Channel> execute(WinstonDatabase winston) throws UtilException {
          return new Channels(winston).getChannels();
        }

      });
    } catch (Exception e) {
      throw new UtilException(e.getMessage());
    }

    for (String line : generateMenu(channels, isScnl)) {
      ctx.write(line);
    }
    ctx.writeAndFlush("\n");
  }

  /**
   * Return server menu as list of strings.
   * 
   * @param channels list of Channels
   * @param isScnl if ture assume locaion code is present
   * @return list of channel names
   * @throws UtilException when things go wrong
   */
  public static List<String> generateMenu(List<Channel> channels, boolean isScnl)
      throws UtilException {

    if (channels == null) {
      return null;
    }

    DecimalFormat decimalFormat = WwsBaseCommand.getDecimalFormat();
    LOGGER.debug("channels count {}", channels.size());
    final List<String> list = new ArrayList<String>(channels.size());
    for (final Channel chan : channels) {
      String line;
      if (isScnl) {
        line = String.format(" %d %s %s %s s4 ", chan.getSID(), chan.scnl.toString(" "),
            decimalFormat.format(Time.j2kToEw(chan.getMinTime())),
            decimalFormat.format(Time.j2kToEw(chan.getMaxTime())));
      } else {
        Scnl scnl = chan.scnl;
        line = String.format(" %d %s %s %s %s %s s4 ", chan.getSID(), scnl.station, scnl.channel,
            scnl.network, decimalFormat.format(Time.j2kToEw(chan.getMinTime())),
            decimalFormat.format(Time.j2kToEw(chan.getMaxTime())));
      }
      list.add(line);
    }
    LOGGER.debug("returning {} items.", list.size());

    return list;
  }
}
