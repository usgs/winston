package gov.usgs.volcanoes.winston.legacyServer.cmd;

import java.nio.channels.SocketChannel;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import gov.usgs.net.ConnectionStatistics;
import gov.usgs.net.Connections;
import gov.usgs.net.NetTools;
import gov.usgs.volcanoes.core.time.J2kSec;
import gov.usgs.volcanoes.core.util.StringUtils;
import gov.usgs.volcanoes.winston.Channel;
import gov.usgs.volcanoes.winston.db.Channels;
import gov.usgs.volcanoes.winston.db.WinstonDatabase;
import gov.usgs.volcanoes.winston.legacyServer.WWS;
import gov.usgs.volcanoes.winston.legacyServer.WWSClient;
import gov.usgs.volcanoes.winston.legacyServer.WWSCommandString;

/**
 * Example command string:
 * STATUS:
 *
 * @author Tom Parker
 */
public class StatusCommand extends BaseCommand {
  private static final Logger LOGGER = LoggerFactory.getLogger(WWSClient.class);

  private final Channels channels;
  private static Connections connections = Connections.getInstance();

  public StatusCommand(final NetTools nt, final WinstonDatabase db, final WWS wws) {
    super(nt, db, wws);
    channels = new Channels(db);
  }

  public void doCommand(final Object info, final SocketChannel channel) {

    final WWSCommandString cmd = new WWSCommandString((String) info);
    final double ageThreshold = StringUtils.stringToDouble(cmd.getString(2), 0);
    final double now = J2kSec.fromEpoch(System.currentTimeMillis());
    LOGGER.debug("STATUS: {}", channel);

    final StringBuilder sb = new StringBuilder();
    int lines = 0;
    final Collection<ConnectionStatistics> css = connections.getConnectionStats();
    sb.append(String.format("Connection count: %d\n", css.size()));
    lines++;

    final List<Channel> sts = channels.getChannels();
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

    netTools.writeString("GC: " + lines + "\n" + sb.toString(), channel);

    LOGGER.debug("STATUS: {}", channel);
  }
}
