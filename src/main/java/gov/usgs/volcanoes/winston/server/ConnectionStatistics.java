/**
 * I waive copyright and related rights in the this work worldwide through the CC0 1.0 Universal
 * public domain dedication. https://creativecommons.org/publicdomain/zero/1.0/legalcode
 */

package gov.usgs.volcanoes.winston.server;

import java.net.InetSocketAddress;
import java.net.SocketAddress;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.atomic.AtomicLong;

import io.netty.channel.Channel;
import io.netty.channel.ChannelFuture;
import io.netty.channel.ChannelFutureListener;
import io.netty.handler.traffic.ChannelTrafficShapingHandler;
import io.netty.handler.traffic.GlobalChannelTrafficShapingHandler;
import io.netty.handler.traffic.TrafficCounter;
import io.netty.util.concurrent.Future;
import io.netty.util.concurrent.GenericFutureListener;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import gov.usgs.volcanoes.core.time.Time;

public class ConnectionStatistics {

  private static final Logger LOGGER = LoggerFactory.getLogger(ConnectionStatistics.class);

  private final AtomicLong connectionCount;
  private final AtomicLong wwsCount;
  private final AtomicLong httpCount;
  private final AtomicLong openCount;
  private Map<InetSocketAddress, Connection> connectionMap;

  public class Connection {
    public final ChannelTrafficShapingHandler trafficShapingHandler;
    public final AtomicLong connectTime;
    public AtomicLong lastTime;
    
    public Connection(ChannelTrafficShapingHandler trafficShapingHandler) {
      this.trafficShapingHandler = trafficShapingHandler;
      connectTime = new AtomicLong(System.currentTimeMillis());
      lastTime = new AtomicLong(connectTime.get());
    }
    
  }
  
  public ConnectionStatistics() {
    LOGGER.warn("Creating new connection stats");
    connectionCount = new AtomicLong(0);
    wwsCount = new AtomicLong(0);
    httpCount = new AtomicLong(0);
    openCount = new AtomicLong(0);
    connectionMap = new HashMap<InetSocketAddress, Connection>();
  }

  public long getCount() {
    return connectionCount.get();
  }

  public void incrHttpCount(SocketAddress socketAddress) {
    httpCount.incrementAndGet();
    connectionMap.get(socketAddress).lastTime.set(System.currentTimeMillis());
  }

  public long getHttpCount() {
    return httpCount.get();
  }

  public void incrWwsCount(SocketAddress socketAddress) {
    wwsCount.incrementAndGet();
    connectionMap.get(socketAddress).lastTime.set(System.currentTimeMillis());
  }

  public long getWwsCount() {
    return wwsCount.get();
  }

  public void incrOpenCount() {
    openCount.incrementAndGet();
    connectionCount.incrementAndGet();
  }

  public void decrOpenCount() {
    openCount.decrementAndGet();
  }

  public long getOpen() {
    return openCount.get();
  }

  public String toString() {
    StringBuffer sb = new StringBuffer();
    sb.append("------- Connections --------\n");
    sb.append(
        "[A]ddress                 [C]onnect time         [L]ast time            [R]X bytes  [T]X bytes  [I]ndex\n");
    for (InetSocketAddress addr : connectionMap.keySet()) {
      Connection conn = connectionMap.get(addr);
      TrafficCounter counter = conn.trafficShapingHandler.trafficCounter();
      sb.append(String.format("%-25s %-22s %-22s %-11d %-11d\n", addr.toString(), Time.format(Time.STANDARD_TIME_FORMAT, conn.connectTime.get()), Time.format(Time.STANDARD_TIME_FORMAT, conn.lastTime.get()),
          counter.cumulativeReadBytes(), counter.cumulativeWrittenBytes()));
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

  public void mapChannel(InetSocketAddress remoteAddress,
      ChannelTrafficShapingHandler trafficCounter2) {
    LOGGER.warn("mapping " + remoteAddress);
    connectionMap.put(remoteAddress, new Connection(trafficCounter2));
  }

  public void unmapChannel(InetSocketAddress remoteAddress) {
    connectionMap.remove(remoteAddress);
  }

}