package gov.usgs.volcanoes.winston.db;

import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import gov.usgs.volcanoes.core.data.Scnl;
import gov.usgs.volcanoes.core.time.J2kSec;
import gov.usgs.volcanoes.core.time.Time;
import gov.usgs.volcanoes.core.time.TimeSpan;
import gov.usgs.volcanoes.core.util.UtilException;
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
    } catch (RuntimeException ex) {
      throw ex;
    } catch (Exception ex) {
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
        final Double t1 = J2kSec.fromEpoch(c1.timeSpan.endTime);
        final Double t2 = J2kSec.fromEpoch(c2.timeSpan.endTime);
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

      final Map<Integer, GroupNode> nodes = getGroupNodes();
      Map<Integer, List<String>> chanNodes = new HashMap<Integer, List<String>>();

      ResultSet rs = winston.executeQuery("SELECT sid, nid FROM grouplinks");
      while (rs.next()) {
        int sid = rs.getInt(1);
        int nid = rs.getInt(2);
        GroupNode gn = nodes.get(nid);
        if (gn != null) {
          List<String> links = chanNodes.get(sid);
          if (links == null) {
            links = new ArrayList<String>();
            chanNodes.put(sid, links);
          }

          links.add(gn.toString());
        }
      }
      rs.close();

      final PreparedStatement ps = winston
          .getPreparedStatement("SELECT * FROM channelmetadata WHERE sid=? ORDER BY name ASC");
      rs = winston.executeQuery(
          "SELECT sid, instruments.iid, code, alias, unit, linearA, linearB, st, et, instruments.lon, instruments.lat, height, name, description, timezone "
              + "FROM channels LEFT JOIN instruments ON channels.iid=instruments.iid "
              + "ORDER BY code ASC");
      final List<Channel> channels = new ArrayList<Channel>();

      while (rs.next()) {
        double lookBack = J2kSec.now() - winston.maxDays * Time.DAY_IN_S;
        double et = rs.getDouble("et");

        if (et <= lookBack) {
          LOGGER.debug("Skipping channel: {} <= {} : {}", et, lookBack, winston.maxDays);
          continue;
        }

        int sid = rs.getInt("sid");
        Scnl scnl;
        try {
          scnl = Scnl.parse(rs.getString("code"));
        } catch (UtilException e) {
          LOGGER.error("Cannot parse station code: {}", rs.getString("code"));
          continue;
        }

        double st = Math.max(rs.getDouble("st"), lookBack);
        TimeSpan timeSpan = new TimeSpan(J2kSec.asEpoch(st), J2kSec.asEpoch(et));

        Channel.Builder builder = new Channel.Builder().sid(sid).scnl(scnl).timeSpan(timeSpan);

        Instrument.Builder instrument_builder = new Instrument.Builder();

        instrument_builder.parse(rs);

        builder.instrument(instrument_builder.build());

        builder.linearA(rs.getDouble("linearA")).linearB(rs.getDouble("linearB"));
        builder.unit(rs.getString("unit")).alias(rs.getString("alias"));
        List<String> links = chanNodes.get(sid);
        if (links != null) {
          for (String group : links) {

            builder.group(group);
          }
        }

        if (fullMetadata) {
          HashMap<String, String> md = null;
          ps.setInt(1, sid);
          ResultSet rs1 = ps.executeQuery();
          while (rs1.next()) {
            if (md == null)
              md = new HashMap<String, String>();
            md.put(rs.getString("name"), rs1.getString("value"));
          }
          rs1.close();
          builder.metadata(md);
        }

        Channel ch = builder.build();

        channels.add(ch);

      }
      rs.close();


      return channels;
    } catch (RuntimeException ex) {
      throw ex;
    } catch (final Exception ex) {
      LOGGER.error("Could not get channels.", ex);
    }
    return null;
  }

  // /**
  // * Get channel ID from code
  // *
  // * @param code
  // * @return channel ID
  // */
  // public int getChannelID(final String code) {
  // if (!winston.checkConnect())
  // return -1;
  //
  // try {
  // int result = -1;
  // winston.useRootDatabase();
  // final ResultSet rs =
  // winston.getStatement().executeQuery("SELECT sid FROM channels WHERE code='" + code + "'");
  // if (rs.next())
  // result = rs.getInt(1);
  // rs.close();
  // return result;
  // } catch (final Exception e) {
  // LOGGER.error("Could not get channel ID. ({})", e.getLocalizedMessage());
  // }
  // return -1;
  // }

  /**
   * Get channel ID from code
   * 
   * @param code
   * @return channel ID
   */
  public int getChannelID(final Scnl scnl) {
    if (!winston.checkConnect())
      return -1;

    try {
      int result = -1;
      winston.useRootDatabase();
      String sql = String.format("SELECT sid FROM channels WHERE code='%s'",
          DbUtils.scnlAsWinstonCode(scnl));
      final ResultSet rs = winston.getStatement().executeQuery(sql);
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
      String cmd = "SELECT code FROM channels WHERE code LIKE '" + chx + "' ORDER BY code ASC";
      LOGGER.info(cmd);
      final ResultSet rs = winston.executeQuery(cmd);
      while (rs.next()) {
        String chan = rs.getString(1);
        LOGGER.info("Found channel {}", chan);
        codes.add(chan);
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
    } catch (final SQLException e) {
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
          .execute("CREATE DATABASE `" + winston.databasePrefix + "_" + code + "`");
      winston.getStatement()
          .execute("INSERT INTO channels (code, st, et) VALUES ('" + code + "', 1E300, -1E300)");
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
      int iid = getInstrumentId(inst.name);
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
      ps.setString(1, inst.name);
      ps.setString(2, inst.description);
      ps.setDouble(3, inst.longitude);
      ps.setDouble(4, inst.latitude);
      ps.setDouble(5, inst.height);
      LOGGER.debug(ps.toString());
      ps.execute();

      if (!instrumentExists) {
        iid = getInstrumentId(inst.name);
        ps = winston.getPreparedStatement("UPDATE channels set iid=? WHERE code LIKE ?;");
        ps.setInt(1, iid);
        ps.setString(2, inst.name + "$%");
        LOGGER.debug(ps.toString());

        ps.execute();
      }
    } catch (final Exception e) {
      String msg = String.format("Could not create channel. Are permissions set properly? (%s)",
          e.getMessage());
      throw new RuntimeException(msg);
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
      final PreparedStatement ps = winston
          .getPreparedStatement("SELECT * FROM instrumentmetadata WHERE iid=? ORDER BY name ASC");
      ResultSet rs = winston.executeQuery("SELECT * FROM instruments ORDER BY name ASC");
      final List<Instrument> insts = new ArrayList<Instrument>();
      while (rs.next()) {
        Instrument.Builder builder = new Instrument.Builder().parse(rs);

        HashMap<String, String> md = null;
        ps.setInt(1, rs.getInt("instruments.iid"));
        ResultSet rs1 = ps.executeQuery();
        while (rs1.next()) {
          if (md == null)
            md = new HashMap<String, String>();
          md.put(rs.getString("name"), rs1.getString("value"));
        }
        rs1.close();
        builder.metadata(md);

        insts.add(builder.build());
      }
      rs.close();
      return insts;
    } catch (RuntimeException e) {
      throw e;
    } catch (final Exception e) {
      LOGGER.error("Could not get instruments.");
    }
    return null;
  }

}
