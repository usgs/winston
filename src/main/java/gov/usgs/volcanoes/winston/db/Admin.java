package gov.usgs.volcanoes.winston.db;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.Iterator;
import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import gov.usgs.volcanoes.core.configfile.ConfigFile;
import gov.usgs.volcanoes.core.time.TimeSpan;
import gov.usgs.volcanoes.winston.Channel;

/**
 *
 * @author Dan Cervelli
 */
public class Admin {
  private static final Logger LOGGER = LoggerFactory.getLogger(Admin.class);

  /** The channel expression SQL wild card. */
  public static final String WILDCARD = "%";

  /**
   * Main method
   *
   * @param args command-line args
   */
  public static void main(final String[] args) {
    if (args.length == 0) {
      printUsage(-1, null);
    }

    int argIndex = 0;
    String cmd = null;
    String arg;
    int status = 0;
    final ConfigFile cf = new ConfigFile("Winston.config");
    final String driver = cf.getString("winston.driver");
    final String url = cf.getString("winston.url");
    final String db = cf.getString("winston.prefix");
    final Admin admin = new Admin(driver, url, db);
    long delay = 0;

    while (argIndex < args.length && status == 0) {
      arg = args[argIndex++];
      // first check for options
      if (arg.equals("--delay")) {
        try {
          arg = args[argIndex++];
          // convert seconds to milliseconds
          delay = Integer.parseInt(arg) * 1000L;
        } catch (final Exception e) {
          status = 2;
        }
      }
      // then check the command and command arguments
      else if (cmd != null || (cmd = arg).length() == 0) {
        // already had a command or empty command
        status = 2;
      } else if (cmd.equals("--span")) {
        admin.calculateTableSpans();
      } else if (cmd.equals("--list")) {
        boolean times = false;
        if (argIndex < args.length) {
          arg = args[argIndex++];
          if (arg.equals("times")) {
            times = true;
          } else {
            status = 2;
          }
        }
        if (status == 0) {
          admin.listChannels(times);
        }
      } else if (cmd.equals("--delete") || cmd.equals("--deletex")) {
        if (argIndex < args.length) {
          arg = args[argIndex++];
          if (cmd.equals("--deletex")) {
            if (admin.isChannelExpressionValid(arg)) {
              admin.deleteChannels(arg, delay);
            } else {
              status = 3;
              System.err.println(
                  "channel expresion may not have wild card for network or station (" + arg + ")");
            }
          } else {
            admin.deleteChannel(arg);
          }
        } else {
          status = 2;
        }
      } else if (cmd.equals("--purge") || cmd.equals("--purgex")) {
        String channel = null;
        int days = 0;
        try {
          arg = args[argIndex++];
          channel = arg;
          arg = args[argIndex++];
          days = Integer.parseInt(arg);
          if (days <= 0) {
            status = 2;
          }
        } catch (final Exception e) {
          status = 2;
        }

        if (status == 0) {
          if (cmd.equals("--purgex")) {
            admin.purgeChannels(channel, days, delay);
          } else {
            admin.purge(channel, days);
          }
        }
      } else if (cmd.equals("--repair")) {
        if (argIndex < args.length) {
          final String day = args[argIndex++];
          final String ch;
          if (argIndex < args.length) {
            ch = args[argIndex++];
          } else {
            ch = null;
          }
          admin.repair(day, ch);
        } else {
          status = 2;
        }
      }
      // else if (cmd.equals("--deletewinston"))
      // {
      // admin.deleteWinston();
      // }
      else {
        System.err.println("Invalid argument(" + argIndex + "): '" + arg + "'");
        status = 1;
      }
    }
    admin.close();
    
    if (status != 0) {
      printUsage(status, cmd);
    }
    
    System.exit(0);
  }

  private static void printUsage(final int status, final String cmd) {
    if (status == 2) {
      System.err.println("Missing or invalid command arguments for command (" + cmd + ")");
    }
    System.out.println(
        "Winston Admin\n\n" + "A collection of commands for administering a Winston database.\n"
            + "Information about connecting to the Winston database must be present\n"
            + "in Winston.config in the current directory.\n\n" + "Usage:\n"
            + "  java gov.usgs.volcanoes.winston.db.Admin [options] command [command arguments]\n"
            + "\nValid options:\n"
            + "  --delay seconds                 the delay between each channel for commands\n"
            + "                                  for multiple channels\n" + "\nValid commands:\n"
            + "  --list                          lists all channels\n"
            + "  --list times                    lists all channels with time span\n"
            + "  --delete channel                delete the specified channel\n"
            + "  --deletex SSSS$CCC$NN[$LL]      delete the specified channels where:\n"
            + "                                  SSSS is the station,\n"
            + "                                  CCC is the channel which may contain\n"
            + "                                  a wild card (%),\n"
            + "                                  SSSS is the station,\n"
            + "                                  NN is the network,\n"
            + "                                  LL is the optional location which may contain\n"
            + "                                  a wild card (%)\n"
            + "  --span                          recalculate table spans\n"
            + "  --purge channel days            purge the specified channel for the\n"
            + "                                  specified number of days\n"
            + "  --purgex channel days           purge the specified channel for the\n"
            + "                                  specified number of days where the channel\n"
            + "                                  may contain a wild card (%) anywhere\n"
            + "  --repair YYYY_MM_DD [channel]   repair all tables on given day\n"
            + "                                  optionally, just repair the specified channel\n" +
            // " --deletewinston completely deletes all Winston databases\n" +
            "");
    if (status != 0) {
      System.exit(status);
    }
  }

  private final Channels channels;

  private final Input input;

  private final WinstonDatabase winston;

  /**
   * Constructor
   *
   * @param driver
   * @param url
   * @param db
   */
  public Admin(final String driver, final String url, final String db) {
    this(new WinstonDatabase(driver, url, db));
  }

  /**
   * Constructor
   *
   * @param w WinstonDatabase
   */
  public Admin(final WinstonDatabase w) {
    winston = w;
    channels = new Channels(winston);
    input = new Input(winston);
  }
  
  /**
   * Close winston connection
   */
  public void close() {
    winston.close();
  }
  
  /**
   * Calculate table spans
   */
  public void calculateTableSpans() {
    try {
      final List<Channel> st = channels.getChannels();
      for (final Iterator<Channel> it = st.iterator(); it.hasNext();) {
        final String code = it.next().scnl.toString();
        System.out.print(code + "...");
        input.calculateSpan(code);
        System.out.println("done.");
      }
    } catch (final Exception e) {
      LOGGER.error("Error during calculateTableSpans(). ({})", e.getLocalizedMessage());
    }
  }

  /**
   * Check on specified table/database
   *
   * @param database name of database
   * @param table name of table
   * @return true if OK, false otherwise
   * @throws SQLException
   */
  public boolean checkTable(final String database, final String table) throws SQLException {
    final ResultSet rs =
        winston.getStatement().executeQuery("CHECK TABLE `" + table + "` FAST QUICK");
    rs.next();
    final String s = rs.getString("Msg_text");
    if (s.endsWith("doesn't exist")) {
      LOGGER.info("{} doesn't exist.", table);
      return false;
    }
    return s.equals("Table is already up to date");
  }

  /**
   * Remove a channel from the database
   *
   * @param ch channel to remove
   */
  public void deleteChannel(final String ch) {
    try {
      winston.useRootDatabase();
      doDeleteChannel(ch);
    } catch (final Exception e) {
      LOGGER.error("Error during deleteChannel(): {}", e.getMessage());
    }
  }

  /**
   * Remove channels from the database
   *
   * @param chx the channel expression
   * @param delay the delay in milliseconds between channels
   * @throws InterruptedException 
   */
  public void deleteChannels(final String chx, final long delay) {
    String ch = chx;
    try {
      final List<String> channelList = channels.getChannelCodes(chx);
      if (channelList == null || channelList.size() == 0) {
        LOGGER.info("deleteChannels: no channels found ({})", chx);
      } else {
        // delete each channel
        LOGGER.info("Deleting channels: {}", channelList);
        Thread.sleep(2000);
        for (int i = 0; i < channelList.size(); i++) {
          ch = channelList.get(i);
          if (i != 0 && delay != 0) {
            Thread.sleep(delay);
          }
          doDeleteChannel(ch);
          LOGGER.info("Deleted channel: {}", ch);
        }
      }
    } catch (final SQLException ex) {
      LOGGER.error("Error during deleteChannels({})", ch);
    } catch (InterruptedException ex) {
      Thread.currentThread().interrupt();
      throw new RuntimeException(ex);
    }
  }

  /**
   * Use with extreme caution.
   */
  public void deleteWinston() {
    try {
      final List<Channel> chs = channels.getChannels();
      for (final Channel ch : chs) {
        deleteChannel(ch.scnl.toString());
      }
      winston.getStatement().execute("DROP DATABASE `" + winston.databasePrefix + "_ROOT`");
    } catch (final Exception e) {
      e.printStackTrace();
    }
  }

  /**
   * Remove a channel from the database
   *
   * @param ch channel to remove
   * @throws SQLException if a SQL exception occurs.
   */
  private void doDeleteChannel(final String ch) throws SQLException {
    winston.useRootDatabase();
    LOGGER.info("Deleting channel {}", ch);
   String cmd = "DELETE FROM channels WHERE code='" + ch + "';";
   LOGGER.info(cmd);
    winston.getStatement().execute(cmd);

    LOGGER.info("Dropping channel database {}", winston.databasePrefix + "_" + ch);
    cmd = "DROP DATABASE `" + winston.databasePrefix + "_" + ch + "`;";
    LOGGER.info(cmd);
    winston.getStatement().execute(cmd);
  }

  /**
   * Determines if the channel expression is valid. The channel
   * expression used for <code>deleteChannels</code> is restricted
   * and should be verified.
   *
   * @param chx the channel expression.
   * @return true if valid, false otherwise.
   * @see #deleteChannels(String, long)
   */
  public boolean isChannelExpressionValid(final String chx) {
    boolean valid = true;
    int index;
    // ensure the station and network do not have wild card
    final String[] ca = chx.split("\\$");
    switch (ca.length) {
      case 3: // station$channel$network
        if (ca[0].indexOf(WILDCARD) >= 0
            || ((index = ca[2].indexOf(WILDCARD)) >= 0 && index <= 1)) {
          valid = true;
        }
        break;
      case 4: // station$channel$network$location
        if (ca[0].indexOf(WILDCARD) >= 0 || ca[2].indexOf(WILDCARD) >= 0) {
          valid = true;
        }
        break;
      default:
        valid = true;
        break;
    }
    return valid;
  }

  /**
   * List channels to system out
   *
   * @param times if true, include min & max times
   */
  public void listChannels(final boolean times) {
    final List<Channel> st = channels.getChannels();
    for (final Channel ch : st) {
      System.out.print("chan: " + ch);
      final String code = ch.scnl.toString();
      System.out.print(code);
      if (times) {
        TimeSpan timeSpan = ch.timeSpan;
        System.out.print("\t" + timeSpan.startTime + "\t" + timeSpan.endTime);
      }
      System.out.println();
    }
  }

  /**
   * Purge specified number of days from specified channel
   *
   * @param channel
   * @param days
   */
  public void purge(final String channel, final int days) {
    input.purgeTables(channel, days, this);
  }

  /**
   * Purge specified number of days from specified channels
   *
   * @param chx the channel expression
   * @param days the number of days
   * @param delay the delay in milliseconds between channels
   */
  public void purgeChannels(final String chx, final int days, final long delay) {
    String ch = chx;
    try {
      final List<String> channelList = channels.getChannelCodes(chx);
      if (channelList == null || channelList.size() == 0) {
        LOGGER.info("purgeChannels: no channels found ({})", chx);
      } else {
        for (int i = 0; i < channelList.size(); i++) {
          ch = channelList.get(i);
          if (i != 0 && delay != 0) {
            Thread.sleep(delay);
          }
          purge(ch, days);
          LOGGER.info("Purged channel: {}", ch);
        }
      }
    } catch (final Exception e) {
      LOGGER.error("Error during purgeChannels({})", ch);
    }
  }

  /**
   * Repair specified channel for specified day
   * If channel unspecified, repair all
   *
   * @param day
   * @param chString channel to repair; null for all
   */
  public void repair(final String day, final String chString) {
    if (chString == null) {
      final List<Channel> chs = channels.getChannels();
      for (final Channel ch : chs) {
        repairChannel(day, ch.scnl.toString());
      }
    } else {
      repairChannel(day, chString);
    }
  }

  /**
   * Leaving as-is for old importer.
   *
   * @param day
   * @param ch
   * @return true if successful, false otherwise
   */
  public boolean repairChannel(final String day, final String ch) {
    try {
      winston.useDatabase(ch);
      boolean fix = false;
      final Statement st = winston.getStatement();

      LOGGER.info("Checking: {} {}", ch, day);
      ResultSet rs = st.executeQuery("CHECK TABLE `" + ch + "$$" + day + "` FAST QUICK");
      rs.next();
      String s = rs.getString("Msg_text");
      if (s.endsWith("doesn't exist")) {
        LOGGER.info("{} wave table doesn't exist.", ch);
        return true;
      }
      if (!s.equals("Table is already up to date")) {
        fix = true;
      }

      rs = st.executeQuery("CHECK TABLE `" + ch + "$$H" + day + "` FAST QUICK");
      rs.next();
      s = rs.getString("Msg_text");
      if (s.endsWith("doesn't exist")) {
        LOGGER.info("{} helicorder table doesn't exist.", ch);
        return true;
      }
      // TODO: check table existence
      if (!rs.getString("Msg_text").equals("Table is already up to date")) {
        fix = true;
      }

      if (fix) {
        LOGGER.info("Repairing: {}", ch);
        // winston.getStatement().execute("REPAIR TABLE " + ch + "$$" + day + " QUICK");
        // winston.getStatement().execute("REPAIR TABLE " + ch + "$$H" + day + " QUICK");
        winston.getStatement().execute("REPAIR TABLE `" + ch + "$$" + day + "`");
        winston.getStatement().execute("REPAIR TABLE `" + ch + "$$H" + day + "`");
      }
      return true;
    } catch (final Exception e) {
      LOGGER.error("Failed to repair: {}", ch);
    }
    return false;
  }

  /**
   * Attempts to repair a table.
   *
   * @param database the database
   * @param table the table name
   * @return flag indicating a whether table is healthy
   */
  public boolean repairTable(final String database, final String table) {
    try {
      winston.useDatabase(database);
      LOGGER.info("Checking table: {}", table);
      boolean ct = checkTable(database, table);
      if (ct == true) {
        return true;
      }

      LOGGER.info("Repairing table: {}", table);
      winston.getStatement().execute("REPAIR TABLE `" + table + "` QUICK");
      ct = checkTable(database, table);
      if (ct == true) {
        return true;
      }

      LOGGER.info("Still broken, attempting further repair: {}", table);
      winston.getStatement().execute("REPAIR TABLE `" + table + "` QUICK USE_FRM");
      return checkTable(database, table);
    } catch (final Exception e) {
      LOGGER.error("Failed to repair: {}", table);
    }
    return false;
  }
}
