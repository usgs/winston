package gov.usgs.volcanoes.winston.db;

import java.sql.PreparedStatement;
import java.sql.SQLException;

import org.apache.commons.collections.map.LRUMap;

import gov.usgs.volcanoes.core.CodeTimer;

/**
 * A class that implements a LRUMap for prepared statements.
 * This exists solely to close statements before being
 * removing them from the map.
 *
 * @author Tom Parker
 */

public class PreparedStatementCache extends LRUMap {


  private static final long serialVersionUID = 1L;

  public PreparedStatementCache(final int cacheCap, final boolean b) {
    super(cacheCap, b);
  }

  public PreparedStatementCache(final int cacheCap) {
    super(cacheCap);
  }

  @Override
  protected boolean removeLRU(final LinkEntry entry) {
    final PreparedStatement ps = (PreparedStatement) entry.getValue();
    try {
      final CodeTimer ct = new CodeTimer("close");
      ps.close();
      ct.stopAndReport();
      return true;
    } catch (final SQLException e) {
      System.err.println("Can't close ps: " + e.getMessage());
    }

    return false;
  }
}
