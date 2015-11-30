package gov.usgs.winston;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.Map;

/**
 * $Log: not supported by cvs2svn $
 * @author Dan Cervelli
 */
public class GroupNode
{
	public GroupNode parent;
	public int nid;
	public int parentID;
	public String name;
	public boolean opened;
	
	public GroupNode(ResultSet rs) throws SQLException
	{
		nid = rs.getInt("nid");
		parentID = rs.getInt("parent");
		name = rs.getString("name");
		opened = rs.getBoolean("open");
	}
	
	public String toString()
	{
		String s = name + (opened ? "!" : "");
		if (parent == null)
			return s;
		else
			return parent.toString() + "^" + s;
	}
	
	public static void buildTree(Map<Integer, GroupNode> nodes)
	{
		for (GroupNode node : nodes.values())
			node.parent = nodes.get(node.parentID);
	}
}
