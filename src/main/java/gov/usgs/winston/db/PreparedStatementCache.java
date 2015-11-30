package gov.usgs.winston.db;

import gov.usgs.util.CodeTimer;

import java.sql.PreparedStatement;
import java.sql.SQLException;

import org.apache.commons.collections.map.LRUMap;

/**
 * A class that implements a LRUMap for prepared statements.
 * This exists solely to close statements before being 
 * removing them from the map.
 * 
 * @author Tom Parker
 */

public class PreparedStatementCache extends LRUMap {


	private static final long serialVersionUID = 1L;

	public PreparedStatementCache(int cacheCap, boolean b) {
		super(cacheCap, b);
	}

	 public PreparedStatementCache(int cacheCap) {
		 super(cacheCap);
	 }

	protected boolean removeLRU(LinkEntry entry) {
		 PreparedStatement ps = (PreparedStatement)entry.getValue();
		 try {
			 CodeTimer ct = new CodeTimer("close");
			 ps.close();
			 ct.stopAndReport();
			 return true;
		 } catch (SQLException e) {
			 System.err.println("Can't close ps: " + e.getMessage());
		 }
		 
		 return false;
	}
}
