/**
 * I waive copyright and related rights in the this work worldwide through the CC0 1.0 Universal
 * public domain dedication. https://creativecommons.org/publicdomain/zero/1.0/legalcode
 */

package gov.usgs.volcanoes.winston.server.wwsCmd;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.text.DecimalFormat;
import java.util.ArrayList;
import java.util.List;

import gov.usgs.volcanoes.core.time.Ew;
import gov.usgs.volcanoes.core.time.J2kSec;
import gov.usgs.volcanoes.core.util.UtilException;
import gov.usgs.volcanoes.winston.Channel;
import gov.usgs.volcanoes.winston.db.Channels;
import gov.usgs.volcanoes.winston.db.WinstonDatabase;
import gov.usgs.volcanoes.winston.server.MalformedCommandException;
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

  public MenuCommand() {
    super();
  }

  public void doCommand(ChannelHandlerContext ctx, WwsCommandString cmd)
      throws MalformedCommandException, UtilException {

    boolean isScnl = false;

    if (cmd.length() == 3) {
      if (cmd.getString(2).equals("SCNL")) {
        isScnl = true;
      }
    } else if (cmd.length() != 2) {
      throw new MalformedCommandException();
    }

    ctx.write(cmd.getID() + " ");

    WinstonDatabase winston = null;
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

    ctx.write(generateMenu(channels, isScnl).toString());
    ctx.writeAndFlush('\n');
  }

  public static List<String> generateMenu(List<Channel> channels, boolean isScnl) throws UtilException {
    
    if (channels == null) {
      return null;
    }

    DecimalFormat decimalFormat = WwsBaseCommand.getDecimalFormat();
    
    LOGGER.debug("channels count {}", channels.size());
    final List<String> list = new ArrayList<String>(channels.size());
    for (final Channel chan : channels) {
      final String[] ss = chan.getCode().split("\\$");
      final double[] ts = {chan.getMinTime(), chan.getMaxTime()};


      if (ts != null && ts[0] < ts[1]) {

        if (isScnl) {
          final String loc = (ss.length == 4 ? ss[3] : "--");
          final String line = " " + chan.getSID() + " " + ss[0] + " " + ss[1] + " " + ss[2] + " "
              + loc + " " + decimalFormat.format(Ew.fromEpoch(J2kSec.asEpoch(ts[0]))) + " "
              + decimalFormat.format(Ew.fromEpoch(J2kSec.asEpoch(ts[1]))) + " s4 ";
          list.add(line);
        } else {
          list.add(" " + chan.getSID() + " " + ss[0] + " " + ss[1] + " " + ss[2] + " "
              + decimalFormat.format(Ew.fromEpoch(J2kSec.asEpoch(ts[0]))) + " "
              + decimalFormat.format(Ew.fromEpoch(J2kSec.asEpoch(ts[1]))) + " s4 ");
        }
      }
    }
    LOGGER.debug("returning {} items.", list.size());
    return list;
  }
}
