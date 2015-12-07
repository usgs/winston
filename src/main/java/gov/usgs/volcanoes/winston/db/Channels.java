package gov.usgs.volcanoes.winston.db;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import gov.usgs.volcanoes.winston.Channel;
import gov.usgs.volcanoes.winston.GroupNode;
import gov.usgs.volcanoes.winston.Instrument;

/**
 * A class for dealing with channels, instruments, and their metdata in a
 * Winston database.
 *
 * TODO: support channel selection
 *
 * @author Dan Cervelli
 */
public class Channels {
  private static final Logger LOGGER = LoggerFactory.getLogger(InputEW.class);

  private final WinstonDatabase winston;

  /**
   * Constructor
   * 
   * @param w
   *          WinstonDatabase
   */
  public Channels(final WinstonDatabase w) {
    winston = w;
  }

  /**
   * Get map of group nodes
   * 
   * @return Map of ids to group nodes
   */
  public Map<Integer, GroupNode> getGroupNodes() {
    if (!winston.checkConnect())
      return null;

    try {
      final HashMap<Integer, GroupNode> result = new HashMap<Integer, GroupNode>();
      winston.useRootDatabase();
      final ResultSet rs = winston.executeQuery("SELECT * FROM groupnodes");
      while (rs.next()) {
        final GroupNode gn = new GroupNode(rs);
        result.put(gn.nid, gn);
      }
      GroupNode.buildTree(result);
      rs.close();

      return result;
    } catch (final Exception e) {
      LOGGER.error("Could not get groups.");
    }
    return null;
  }

  /**
   * Get List of channels
   * 
   * @return List of channels
   */
  public List<Channel> getChannels() {
    return getChannels(false);
  }

  /**
   * Get List of channels sorted by data age
   * 
   * @return List of channels
   */
  public List<Channel> getChannelsByLastInsert() {
    final List<Channel> sts = getChannels();

    Collections.sort(sts, new Comparator<Channel>() {
      public int compare(final Channel c1, final Channel c2) {
        final Double t1 = c1.getMaxTime();
        final Double t2 = c2.getMaxTime();
        return t2.compareTo(t1);
      }
    });

    return sts;
  }

  /**
   * Get List of channels
   * 
   * @param fullMetadata
   *          if true, included full metadata
   * @return List of channels
   */
  public List<Channel> getChannels(final boolean fullMetadata) {
    if (!winston.checkConnect())
      return null;

    try {
      winston.useRootDatabase();
      ResultSet rs = winston.executeQuery(
          "SELECT sid, instruments.iid, code, alias, unit, linearA, linearB, st, et, instruments.lon, instruments.lat, height, name, description, timezone "
              + "FROM channels LEFT JOIN instruments ON channels.iid=instruments.iid "
              + "ORDER BY code ASC");
      final Map<Integer, Channel> channelsMap = new HashMap<Integer, Channel>();
      final List<Channel> channels = new ArrayList<Channel>();
      while (rs.next()) {
        final Channel ch = new Channel(rs);
        channelsMap.put(ch.getSID(), ch);
        channels.add(ch);
      }
      rs.close();
      final Map<Integer, GroupNode> nodes = getGroupNodes();

      rs = winston.executeQuery("SELECT sid, nid FROM grouplinks");
      while (rs.next()) {
        final int sid = rs.getInt(1);
        final int nid = rs.getInt(2);
        final Channel ch = channelsMap.get(sid);
        if (ch != null) {
          final GroupNode gn = nodes.get(nid);
          if (gn != null)
            ch.addGroup(gn.toString());
        }
      }
      rs.close();

      if (fullMetadata) {
        final PreparedStatement ps = winston
            .getPreparedStatement("SELECT * FROM channelmetadata WHERE sid=? ORDER BY name ASC");
        for (final Channel ch : channels) {
          HashMap<String, String> md = null;
          ps.setInt(1, ch.getSID());
          rs = ps.executeQuery();
          while (rs.next()) {
            if (md == null)
              md = new HashMap<String, String>();
            md.put(rs.getString("name"), rs.getString("value"));
          }
          ch.setMetadata(md);
        }
      }

      return channels;
    } catch (final Exception e) {
      LOGGER.error("Could not get channels.");
    }
    return null;
  }

  // public Map<String, Channel[]> getChannelsByNet(boolean fullMetadata) {
  // Map<String, Deque<Channel>> chansByNet = new HashMap<String, Deque<Channel>>();
  //
  // for (Channel c : getChannels(fullMetadata)) {
  // String net = c.getNetwork();
  // Deque<Channel> chans = chansByNet.get(net);
  // if (chans == null) {
  // chans = new ArrayDeque<Channel>();
  // chansByNet.put(net, chans);
  // }
  //
  // chans.add(c);
  // }
  //
  // Map<String, Channel[]> chans = new HashMap<String, Channel[]>();
  // for (String net : chansByNet.keySet())
  // chans.put(net, (Channel[])chansByNet.get(net).toArray());
  //
  // return chans;
  // }

  /**
   * Get channel ID from code
   * 
   * @param code
   * @return channel ID
   */
  public int getChannelID(final String code) {
    if (!winston.checkConnect())
      return -1;

    try {
      int result = -1;
      winston.useRootDatabase();
      final ResultSet rs =
          winston.getStatement().executeQuery("SELECT sid FROM channels WHERE code='" + code + "'");
      if (rs.next())
        result = rs.getInt(1);
      rs.close();
      return result;
    } catch (final Exception e) {
      LOGGER.error("Could not get channel ID. ({})", e.getLocalizedMessage());
    }
    return -1;
  }

  /**
   * Get the list of channel codes that match the specified expression.
   * 
   * @param chx
   *          the channel expression.
   * @return the list of codes or null if error.
   */
  public List<String> getChannelCodes(final String chx) {
    if (!winston.checkConnect())
      return null;

    final List<String> codes = new ArrayList<String>();
    try {
      winston.useRootDatabase();
      final ResultSet rs = winston.executeQuery(
          "SELECT code FROM channels WHERE code LIKE '" + chx + "' ORDER BY code ASC");
      while (rs.next()) {
        codes.add(rs.getString(1));
      }
      rs.close();
      return codes;
    } catch (final Exception e) {
      LOGGER.error("Could not get channel codes.");
    }
    return null;
  }

  /**
   * Get instrument ID from name
   * 
   * @param name
   * @return channel ID
   */
  public int getInstrumentId(final String name) {
    if (!winston.checkConnect())
      return -1;

    try {
      int result = -1;
      winston.useRootDatabase();
      final ResultSet rs = winston.getStatement()
          .executeQuery("SELECT iid FROM instruments WHERE name='" + name + "'");
      if (rs.next())
        result = rs.getInt(1);
      rs.close();
      return result;
    } catch (final Exception e) {
      LOGGER.error("Could not get instrument ID. ({})", e.getLocalizedMessage());
    }
    return -1;
  }

  /**
   * Get code from channel ID
   * 
   * @param sid
   *          channel ID
   * @return code
   */
  public String getChannelCode(final int sid) {
    if (!winston.checkConnect())
      return null;

    try {
      String result = null;
      winston.useRootDatabase();
      final ResultSet rs =
          winston.getStatement().executeQuery("SELECT code FROM channels WHERE sid=" + sid);
      if (rs.next())
        result = rs.getString(1);
      rs.close();
      return result;
    } catch (final Exception e) {
      LOGGER.error("Could not get channel code.");
    }
    return null;
  }

  /**
   * Check existence of channel w/ code
   * 
   * @param code
   * @return true if channel w/ code exists
   */
  public boolean channelExists(final String code) {
    if (!winston.checkConnect())
      return false;
    try {
      boolean result = false;
      winston.useRootDatabase();
      final ResultSet rs =
          winston.getStatement().executeQuery("SELECT sid FROM channels WHERE code='" + code + "'");
      result = rs.next();
      rs.close();
      return result;
    } catch (final Exception e) {
      LOGGER.error("Could not determine channel existence.");
    }
    return false;
  }

  /**
   * 
   * @param name
   * @return true if instrument exists in instruments table
   */
  public boolean instrumentExists(final String name) {
    return getInstrumentId(name) > 0;
  }

  /**
   * Create channel w/ code
   * 
   * @param code
   * @return true if successful
   */
  public boolean createChannel(final String code) {
    if (!winston.checkConnect())
      return false;
    try {
      winston.useRootDatabase();
      winston.getStatement()
          .execute("INSERT INTO channels (code, st, et) VALUES ('" + code + "', 1E300, -1E300)");
      winston.getStatement()
          .execute("CREATE DATABASE `" + winston.databasePrefix + "_" + code + "`");
      winston.getStatement().execute("USE `" + winston.databasePrefix + "_" + code + "`");
    } catch (final Exception e) {
      LOGGER.error("Could not create channel.  Are permissions set properly?");
    }
    return false;
  }

  /**
   * Create or update row in instrument table
   * 
   * TODO: add network code once winston schema supports it
   * 
   * @param inst
   * @return true if successful
   */
  public void updateInstrument(final Instrument inst) {
    if (!winston.checkConnect())
      return;
    try {
      winston.useRootDatabase();
      int iid = getInstrumentId(inst.getName());
      PreparedStatement ps = null;
      final boolean instrumentExists = iid > 0;
      if (instrumentExists) {
        ps = winston.getPreparedStatement(
            "UPDATE instruments SET name=?, description=?, lon=?, lat=?, height=? where iid=?;");
        ps.setInt(6, iid);
      } else {
        ps = winston.getPreparedStatement(
            "INSERT INTO instruments (name, description, lon, lat, height) VALUES (?,?,?,?,?);");
      }
      ps.setString(1, inst.getName());
      ps.setString(2, inst.getDescription());
      ps.setDouble(3, inst.getLongitude());
      ps.setDouble(4, inst.getLatitude());
      ps.setDouble(5, inst.getHeight());
      LOGGER.debug(ps.toString());
      ps.execute();

      if (!instrumentExists) {
        iid = getInstrumentId(inst.getName());
        ps = winston.getPreparedStatement("UPDATE channels set iid=? WHERE code LIKE ?;");
        ps.setInt(1, iid);
        ps.setString(2, inst.getName() + "$%");
        LOGGER.debug(ps.toString());

        ps.execute();
      }
    } catch (final Exception e) {
      LOGGER.error("Could not create channel.  Are permissions set properly? ({})", e.getMessage());
      System.exit(1);
    }
  }

  /**
   * Getter for instruments
   * 
   * @return List of instruments
   */
  public List<Instrument> getInstruments() {
    if (!winston.checkConnect())
      return null;

    try {
      winston.useRootDatabase();
      ResultSet rs = winston.executeQuery("SELECT * FROM instruments ORDER BY name ASC");
      final List<Instrument> insts = new ArrayList<Instrument>();
      while (rs.next()) {
        final Instrument inst = new Instrument(rs);
        insts.add(inst);
      }
      rs.close();

      final PreparedStatement ps = winston
          .getPreparedStatement("SELECT * FROM instrumentmetadata WHERE iid=? ORDER BY name ASC");
      for (final Instrument inst : insts) {
        HashMap<String, String> md = null;
        ps.setInt(1, inst.getID());
        rs = ps.executeQuery();
        while (rs.next()) {
          if (md == null)
            md = new HashMap<String, String>();
          md.put(rs.getString("name"), rs.getString("value"));
        }
        inst.setMetadata(md);
      }
      return insts;
    } catch (final Exception e) {
      LOGGER.error("Could not get instruments.");
    }
    return null;
  }

}
