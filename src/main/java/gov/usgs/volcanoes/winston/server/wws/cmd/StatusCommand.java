/**
 * I waive copyright and related rights in the this work worldwide through the CC0 1.0 Universal
 * public domain dedication. https://creativecommons.org/publicdomain/zero/1.0/legalcode
 */

package gov.usgs.volcanoes.winston.server.wws.cmd;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.List;

import gov.usgs.volcanoes.core.time.Ew;
import gov.usgs.volcanoes.core.time.J2kSec;
import gov.usgs.volcanoes.core.util.StringUtils;
import gov.usgs.volcanoes.core.util.UtilException;
import gov.usgs.volcanoes.winston.Channel;
import gov.usgs.volcanoes.winston.db.Channels;
import gov.usgs.volcanoes.winston.db.WinstonDatabase;
import gov.usgs.volcanoes.winston.server.ConnectionStatistics;
import gov.usgs.volcanoes.winston.server.MalformedCommandException;
import gov.usgs.volcanoes.winston.server.wws.WinstonConsumer;
import gov.usgs.volcanoes.winston.server.wws.WwsBaseCommand;
import gov.usgs.volcanoes.winston.server.wws.WwsCommandString;
import io.netty.channel.ChannelHandlerContext;
import io.netty.util.AttributeKey;

public class StatusCommand extends WwsBaseCommand {

  private static final Logger LOGGER = LoggerFactory.getLogger(StatusCommand.class);

  private ConnectionStatistics connectionStatistics;

  private static final AttributeKey<ConnectionStatistics> connectionStatsKey;

  static {
    connectionStatsKey = AttributeKey.valueOf("connectionStatistics");
  }

  public StatusCommand() {
    super();
  }

  public void doCommand(ChannelHandlerContext ctx, WwsCommandString cmd)
      throws MalformedCommandException, UtilException {

    final double ageThreshold = StringUtils.stringToDouble(cmd.getString(2), 0);
    final double now = J2kSec.fromEpoch(System.currentTimeMillis());

    final StringBuilder sb = new StringBuilder();
    int lines = 0;

    connectionStatistics = ctx.channel().attr(connectionStatsKey).get();
    sb.append(String.format("Connection count: %d\n", connectionStatistics.getCount()));
    lines++;

    List<Channel> sts;
    try {
      sts = databasePool.doCommand(new WinstonConsumer<List<Channel>>() {
        public List<Channel> execute(WinstonDatabase winston) {
          return new Channels(winston).getChannels();
        }
      });
    } catch (Exception e) {
      throw new UtilException("Unable to get channels for status command");
    }
    
    sb.append(String.format("Channel count: %d\n", sts.size()));
    lines++;

    final ArrayList<Double> ages = new ArrayList<Double>();
    for (final Channel st : sts)
      if (st.getMaxTime() < now && (ageThreshold == 0 || now - st.getMaxTime() < ageThreshold))
        ages.add(now - st.getMaxTime());

    if (ages.size() == 0)
      ages.add(0d);

    Double[] d = new Double[ages.size() - 1];
    d = ages.toArray(d);
    Arrays.sort(d);

    sb.append(String.format("Median data age: %s\n", d[(d.length - 1) / 2]));
    lines++;

    ctx.write("GC: " + lines + '\n');
    ctx.writeAndFlush(sb.toString());
  }
}
