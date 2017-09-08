/**
 * I waive copyright and related rights in the this work worldwide through the CC0 1.0 Universal
 * public domain dedication. https://creativecommons.org/publicdomain/zero/1.0/legalcode
 */

package gov.usgs.volcanoes.winston.server;

import java.net.InetSocketAddress;
import java.net.SocketAddress;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicLong;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import gov.usgs.volcanoes.core.time.Time;
import io.netty.handler.traffic.ChannelTrafficShapingHandler;

/**
 * Statistics of connections to a wave server.
 * 
 * @author Tom Parker
 *
 */
public class ConnectionStatistics {

  private static final Logger LOGGER = LoggerFactory.getLogger(ConnectionStatistics.class);

  private static enum SortOrder {
    ADDRESS, CONNECT_TIME, LAST_REQUEST_TIME, RX_BYTES, TX_BYTES, INDEX;

    public static SortOrder parse(char c) {
      switch (Character.toUpperCase(c)) {
        default:
        case 'A':
          return ADDRESS;
        case 'L':
          return LAST_REQUEST_TIME;
        case 'C':
          return CONNECT_TIME;
        case 'R':
          return RX_BYTES;
        case 'T':
          return TX_BYTES;
        case 'I':
          return INDEX;
      }
    }
  }

  private final AtomicLong connectionCount;
  private final AtomicLong wwsCount;
  private final AtomicLong httpCount;
  private final AtomicLong openCount;
  private Map<InetSocketAddress, Connection> connectionMap;

  private class Connection {
    private final String address;
    private final ChannelTrafficShapingHandler trafficShapingHandler;
    private final AtomicLong connectTime;
    private AtomicLong lastTime;

    public Connection(String address, ChannelTrafficShapingHandler trafficShapingHandler) {
      this.address = address;
      this.trafficShapingHandler = trafficShapingHandler;
      connectTime = new AtomicLong(System.currentTimeMillis());
      lastTime = new AtomicLong(connectTime.get());
    }

    public String address() {
      return address;
    }

    public long connectTime() {
      return connectTime.get();
    }

    public long lastTime() {
      return lastTime.get();
    }

    public long cumulativeReadBytes() {
      return trafficShapingHandler.trafficCounter().cumulativeReadBytes();
    }


    public long cumulativeWrittenBytes() {
      return trafficShapingHandler.trafficCounter().cumulativeWrittenBytes();
    }
  }

  /**
   * Constructor.
   */
  public ConnectionStatistics() {
    LOGGER.debug("Creating new connection stats");
    connectionCount = new AtomicLong(0);
    wwsCount = new AtomicLong(0);
    httpCount = new AtomicLong(0);
    openCount = new AtomicLong(0);
    connectionMap = Collections.synchronizedMap(new HashMap<InetSocketAddress, Connection>());

  }

  /**
   * return connection count.
   * @return connection count
   */
  public long getCount() {
    return connectionCount.get();
  }

  /**
   * Increment the HTTP count.
   * 
   * @param socketAddress address of new connection
   */
  public void incrHttpCount(SocketAddress socketAddress) {
    httpCount.incrementAndGet();
    connectionMap.get(socketAddress).lastTime.set(System.currentTimeMillis());
  }

  /**
   * Return HTTP connection count.
   * 
   * @return HTTP connection count
   */
  public long getHttpCount() {
    return httpCount.get();
  }

  /**
   * Increment WWS count.
   * 
   * @param socketAddress address of new connection
   */
  public void incrWwsCount(SocketAddress socketAddress) {
    wwsCount.incrementAndGet();
    connectionMap.get(socketAddress).lastTime.set(System.currentTimeMillis());
  }

  /**
   * Return WWS connnection count.
   * 
   * @return count
   */
  public long getWwsCount() {
    return wwsCount.get();
  }

  /** 
   * Increment open connection count.
   */
  public void incrOpenCount() {
    openCount.incrementAndGet();
    connectionCount.incrementAndGet();
  }

  /**
   * decrement connection count.
   */
  public void decrOpenCount() {
    openCount.decrementAndGet();
  }

  /**
   * Find open connection count.
   * 
   * @return connection count
   */
  public long getOpen() {
    return openCount.get();
  }

  /**
   * Add connection.
   * 
   * @param remoteAddress remote address
   * @param trafficCounter counter
   */
  public void mapChannel(InetSocketAddress remoteAddress,
      ChannelTrafficShapingHandler trafficCounter) {
    LOGGER.debug("mapping " + remoteAddress);
    connectionMap.put(remoteAddress, new Connection(remoteAddress.toString(), trafficCounter));
  }

  /**
   * Remove connection.
   * 
   * @param remoteAddress address to remove
   */
  public void unmapChannel(InetSocketAddress remoteAddress) {
    connectionMap.remove(remoteAddress);
  }

  /**
   * Get formatted string of connections.
   * 
   * @param s sort order
   * @return connection string
   */
  public String printConnections(String s) {
    StringBuffer sb = new StringBuffer();
    sb.append("------- Connections --------\n");
    sb.append(
        "[A]ddress                 [C]onnect time         [L]ast time            [R]X bytes  [T]X bytes  [I]ndex\n");

    char col = 'T';
    if (s.length() > 1)
      col = s.charAt(1);
    boolean desc = s.endsWith("-");

    List<Connection> connections = new ArrayList<Connection>(connectionMap.size());
    connections.addAll(connectionMap.values());
    Collections.sort(connections, getComparator(SortOrder.parse(col), desc));

    for (Connection connection : connections) {
      sb.append(String.format("%-25s %-22s %-22s %-11d %-11d%n", connection.address(),
          Time.format(Time.STANDARD_TIME_FORMAT, connection.connectTime()),
          Time.format(Time.STANDARD_TIME_FORMAT, connection.lastTime()),
          connection.cumulativeReadBytes(), connection.cumulativeWrittenBytes()));

    }
    sb.append(
        "[A]ddress                 [C]onnect time         [L]ast time            [R]X bytes  [T]X bytes  [I]ndex\n");
    sb.append("\n\n");
    sb.append("Total Connections : ").append(connectionCount).append('\n');
    sb.append("Open Connections  : ").append(openCount).append('\n');
    sb.append("WWS Commands      : ").append(wwsCount).append('\n');
    sb.append("HTTP commands     : ").append(httpCount).append('\n');
    return sb.toString();
  }

  /**
   * Connection comparator.
   * 
   * @param order sort order
   * @param desc if true sort descending
   * @return comparator
   */
  public static Comparator<Connection> getComparator(final SortOrder order, final boolean desc) {
    return new Comparator<Connection>() {
      public int compare(Connection cs1, Connection cs2) {
        int cmp = 0;
        switch (order) {
          default:
          case ADDRESS:
            cmp = cs1.address.compareTo(cs2.address);
            break;
          case LAST_REQUEST_TIME:
            cmp = (int) (cs1.lastTime() - cs2.lastTime());
            break;
          case CONNECT_TIME:
            cmp = (int) (cs1.connectTime() - cs2.connectTime());
            break;
          case RX_BYTES:
            cmp = (int) (cs1.cumulativeReadBytes() - cs2.cumulativeReadBytes());
            break;
          case TX_BYTES:
            cmp = (int) (cs1.cumulativeWrittenBytes() - cs2.cumulativeWrittenBytes());
            break;
        }
        if (desc)
          cmp = -cmp;

        return cmp;
      }
    };
  }
}
