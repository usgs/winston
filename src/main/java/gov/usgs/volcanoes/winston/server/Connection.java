package gov.usgs.volcanoes.winston.server;

import java.util.Comparator;
import java.util.concurrent.atomic.AtomicLong;

import io.netty.handler.traffic.ChannelTrafficShapingHandler;

public class Connection {



  public static enum SortField {
    ADDRESS, CONNECT_TIME, LAST_REQUEST_TIME, RX_BYTES, TX_BYTES, INDEX;

    public static SortField parse(char c) {
      switch (Character.toUpperCase(c)) {
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
        case 'A':
        default:
          return ADDRESS;
      }
    }
  }

  public static enum SortOrder {
    ASCENDING, DESCENDING;
  }

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

  public void lastTime(long currentTimeMillis) {
    lastTime.set(currentTimeMillis);
  }

  public long cumulativeReadBytes() {
    return trafficShapingHandler.trafficCounter().cumulativeReadBytes();
  }


  public long cumulativeWrittenBytes() {
    return trafficShapingHandler.trafficCounter().cumulativeWrittenBytes();
  }

  public static Comparator<Connection> getComparator(final SortField sortField,
      final SortOrder sortOrder) {
    return new Comparator<Connection>() {
      public int compare(final Connection c1, final Connection c2) {
        int cmp;

        switch (sortField) {
          case LAST_REQUEST_TIME:
            cmp = Long.compare(c1.lastTime(), c2.lastTime());
            break;
          case CONNECT_TIME:
            cmp = Long.compare(c1.connectTime(), c2.connectTime());
            break;
          case RX_BYTES:
            cmp = Long.compare(c1.cumulativeReadBytes(), c2.cumulativeReadBytes());
            break;
          case TX_BYTES:
            cmp = Long.compare(c1.cumulativeWrittenBytes(), c2.cumulativeWrittenBytes());
            break;
          case ADDRESS:
          default:
            cmp = c1.address.compareTo(c2.address);
            break;
        }

        if (sortOrder == SortOrder.DESCENDING) {
          cmp = -cmp;
        }

        return cmp;
      }
    };
  }

}
