package gov.usgs.volcanoes.winston.server;

import java.util.concurrent.atomic.AtomicLong;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class ConnectionStatistics {

  private static final Logger LOGGER = LoggerFactory.getLogger(ConnectionStatistics.class);
  
  AtomicLong connectionCount;
  AtomicLong wwsCount;
  AtomicLong httpCount;
  AtomicLong activeCount;
  
  public ConnectionStatistics() {
    LOGGER.warn("Creating new connection stats");
    connectionCount = new AtomicLong(0);
    wwsCount = new AtomicLong(0);
    httpCount = new AtomicLong(0);
    activeCount = new AtomicLong(0);
  }
  
  public void incrCount() {
    connectionCount.incrementAndGet();
  }
  
  public void decrCount() {
    connectionCount.decrementAndGet();
  }

  public long getCount() {
    return connectionCount.get();
  }
  
  public void incrHttpCount() {
    httpCount.incrementAndGet();
  }
  
  public void decrHttpCount() {
    httpCount.decrementAndGet();
  }
  
  public long getHttpCount() {
    return httpCount.get();
  }
  
  public void incrWwsCount() {
    wwsCount.incrementAndGet();
  }
  
  public void decrWwsCount() {
    wwsCount.decrementAndGet();
  }

  public long getWwsCount() {
    return wwsCount.get();
  }

  public void incrActiveCount() {
    activeCount.incrementAndGet();
  }
  
  public void decrActiveCount() {
    activeCount.decrementAndGet();
  }
  
  public long getActive() {
    return activeCount.get();
  }
}
