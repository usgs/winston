/**
 * I waive copyright and related rights in the this work worldwide
 * through the CC0 1.0 Universal public domain dedication.
 * https://creativecommons.org/publicdomain/zero/1.0/legalcode
 */

package gov.usgs.volcanoes.winston.server;

import java.net.InetSocketAddress;
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

public class ConnectionStatistics {

  private static final Logger LOGGER = LoggerFactory.getLogger(ConnectionStatistics.class);

  private final AtomicLong connectionCount;
  private final AtomicLong wwsCount;
  private final AtomicLong httpCount;
  private final AtomicLong openCount;
  private Map<InetSocketAddress, TrafficCounter> connectionMap;

  public ConnectionStatistics() {
    LOGGER.warn("Creating new connection stats");
    connectionCount = new AtomicLong(0);
    wwsCount = new AtomicLong(0);
    httpCount = new AtomicLong(0);
    openCount = new AtomicLong(0);
    connectionMap = new HashMap<InetSocketAddress, TrafficCounter>();
  }

  public long getCount() {
    return connectionCount.get();
  }

  public void incrHttpCount() {
    httpCount.incrementAndGet();
  }

  public long getHttpCount() {
    return httpCount.get();
  }

  public void incrWwsCount() {
    wwsCount.incrementAndGet();
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
    sb.append("Total Connections : ").append(connectionCount).append('\n');
    sb.append("Open Connections  : ").append(openCount).append('\n');
    sb.append("WWS Commands      : ").append(wwsCount).append('\n');
    sb.append("HTTP commands     : ").append(httpCount).append('\n');

     for (InetSocketAddress addr : connectionMap.keySet()) {
      sb.append(addr.toString());
     }
//    for (TrafficCounter counter : trafficCounter.channelTrafficCounters()) {
//      InetSocketAddress addr = connectionMap.get(counter.name());
//      if (addr == null)
//        System.out.println("null name " + counter.name());
//      else
//        sb.append(connectionMap.get(addr).toString()).append(' ')
//            .append(counter.cumulativeReadBytes()).append('/')
//            .append(counter.cumulativeWrittenBytes()).append('\n');
//    }
    return sb.toString();
  }

  public void mapChannel(InetSocketAddress remoteAddress, TrafficCounter trafficCounter2) {
    connectionMap.put(remoteAddress, trafficCounter2);
  }
}
