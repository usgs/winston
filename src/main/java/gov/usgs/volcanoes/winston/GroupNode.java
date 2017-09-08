package gov.usgs.volcanoes.winston;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.Map;

/**
 * $Log: not supported by cvs2svn $
 *
 * @author Dan Cervelli
 */
public class GroupNode {
  /** Parent node */
  public GroupNode parent;
  
  /** Node id */
  public int nid;
  
  /** Parent id */
  public int parentID;
  
  /** group name */
  public String name;
  
  /** if true default open */
  public boolean opened;

  /**
   * Constructor.
   * 
   * @param rs result set :(
   * @throws SQLException when things go wrong
   */
  public GroupNode(final ResultSet rs) throws SQLException {
    nid = rs.getInt("nid");
    parentID = rs.getInt("parent");
    name = rs.getString("name");
    opened = rs.getBoolean("open");
  }

  @Override
  public String toString() {
    final String s = name + (opened ? "!" : "");
    if (parent == null)
      return s;
    else
      return parent.toString() + "^" + s;
  }

  /**
   * Populate parent nodes.
   * 
   * @param nodes map
   */
  public static void buildTree(final Map<Integer, GroupNode> nodes) {
    for (final GroupNode node : nodes.values())
      node.parent = nodes.get(node.parentID);
  }
}
