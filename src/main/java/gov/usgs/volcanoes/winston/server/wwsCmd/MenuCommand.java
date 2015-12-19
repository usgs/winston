/**
 * I waive copyright and related rights in the this work worldwide
 * through the CC0 1.0 Universal public domain dedication.
 * https://creativecommons.org/publicdomain/zero/1.0/legalcode
 */

package gov.usgs.volcanoes.winston.server.wwsCmd;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.List;

import gov.usgs.volcanoes.core.time.Ew;
import gov.usgs.volcanoes.core.time.J2kSec;
import gov.usgs.volcanoes.winston.Channel;
import gov.usgs.volcanoes.winston.db.Channels;
import gov.usgs.volcanoes.winston.db.WinstonDatabase;
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
      throws MalformedCommandException {

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
    List<String> serverMenu = null;

    try {
      winston = databasePool.borrowObject();
      if (!winston.checkConnect()) {
        LOGGER.error("WinstonDatabase unable to connect to MySQL.");
      } else {
        serverMenu = generateMenu(winston, isScnl);
      }
    } catch (Exception e) {
      LOGGER.error("Unable to fulfill command.", e);
    } finally {
      if (winston != null) {
        databasePool.returnObject(winston);
      }
    }

    if (serverMenu != null) {
      LOGGER.info("sending {} items", serverMenu.size());
      for (String s : serverMenu) {
        ctx.write(s);
        LOGGER.info("sending: {}", s);
      }
      ctx.writeAndFlush("\n");
    } else {
      LOGGER.error("NULL server menu.");
    }
  }

  public List<String> generateMenu(WinstonDatabase winston, boolean isScnl) {
    final List<Channel> channels = new Channels(winston).getChannels();
    if (channels == null) {
      return null;
    }

    LOGGER.debug("channels count {}", channels.size());
    final List<String> list = new ArrayList<String>(channels.size());
    for (final Channel chan : channels) {
      final String[] ss = chan.getCode().split("\\$");
      final double[] ts = {chan.getMinTime(), chan.getMaxTime()};


      if (maxDays > 0) {
        ts[0] = Math.max(ts[0], J2kSec.now() - (maxDays * ONE_DAY_S));
      }

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
