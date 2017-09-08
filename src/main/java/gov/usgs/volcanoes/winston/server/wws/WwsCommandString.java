/**
 * I waive copyright and related rights in the this work worldwide
 * through the CC0 1.0 Universal public domain dedication.
 * https://creativecommons.org/publicdomain/zero/1.0/legalcode
 */

package gov.usgs.volcanoes.winston.server.wws;

import java.util.Arrays;

import gov.usgs.volcanoes.core.data.Scnl;
import gov.usgs.volcanoes.core.time.J2kSec;
import gov.usgs.volcanoes.core.time.TimeSpan;
import gov.usgs.volcanoes.winston.server.MalformedCommandException;

/**
 * A convenience class for dealing with WWS commands.
 * 
 * <req> = <cmd> " " <id> " " [ <args> ] <crlf>
 * <args>  = <command-specific arg 1> [ <sp> <command-specific arg 2> ]
 *         | <channel spec> <sp> [ <command-specific args> ]
 * <channel spec> = <SCNL> [ <sp> <time span> ]
 * <SCNL> = <station> <sp> <channel> <sp> <network> <sp> [ <location> ]
 * <time span> = <start time> <sp> <end time>
 * <start time> = <J2kSec>
 * <end time> = <J2kSec>
 * 
 * @author Dan Cervelli
 * @author Tom Parker
 */
public class WwsCommandString {
  /** No location provided in request */
  public static final boolean NO_LOCATION = false;
  
  /** location provided in request */
  public static final boolean HAS_LOCATION = true;

  /** String sent by client  */
  public final String commandString;
  
  /** command requested */
  public final String command;
 
  /** id echoed back for client use */
  public final String id;
  
  /** command args */
  public final String[] args;

  private Scnl scnl;
  private TimeSpan timeSpan;

  /**
   * Constructor.
   * @param commandString my command string
   * @throws MalformedCommandException when things go wrong
   */
  public WwsCommandString(final String commandString) throws MalformedCommandException {
    this.commandString = commandString;
    String[] commandSplits = commandString.split(" ");
    String cmd = commandSplits[0];
    if (cmd.endsWith(":")) {
      command = cmd.substring(0, cmd.length() - 1);
    } else {
      command = cmd;
    }

    if (commandSplits.length > 1) {
      id = commandSplits[1];
    } else {
      id = null;
    }

    if (commandSplits.length > 2) {
      args = Arrays.copyOfRange(commandSplits, 2, commandSplits.length);
    } else {
      args = null;
    }
  }

  /**
   * Command token accessor.
   * @param index token index
   * @return command token
   */
  public String getString(int index) {
    index = getIndex(index);
    if (index >= args.length)
      return null;
    else
      return args[index];
  }

  /**
   * Command token accessor
   * @param index token index
   * @return command token
   * @throws MalformedCommandException When int cannot be parsed
   */
  public int getInt(int index) throws MalformedCommandException {
    index = getIndex(index);
    int result;
    try {
      result = Integer.parseInt(args[index]);
    } catch (final NumberFormatException ex) {
      throw new MalformedCommandException(ex.getLocalizedMessage());
    }
    return result;
  }

  /**
   * Command token accessor
   * @param index token index
   * @return command token
   * @throws MalformedCommandException  when double cannot be parsed
   */
  public double getDouble(int index) throws MalformedCommandException {
    index = getIndex(index);
    double result;
    try {
      result = Double.parseDouble(args[index]);
    } catch (final NumberFormatException ex) {
      throw new MalformedCommandException(ex.getLocalizedMessage());
    }
    return result;
  }

  private int getIndex(int index) {
    if (index < 0) {
      return index + args.length;
    } else {
      return index;
    }
  }

  /**
   * Return command SCN and update where the time span may be located. This will always be the first arg.
   * 
   * @return the scn channel requested.
   * @throws MalformedCommandException when arg list is too short
   */
  public synchronized Scnl getScn() throws MalformedCommandException {
    if (scnl == null) {
      scnl = new Scnl(args[0], args[1], args[2]);
    }
    return scnl;
  }

  /**
   * Return command SCN or SCNL. This will always be the first arg.
   * 
   * @return the scnl channel requested
   * @throws MalformedCommandException when arg list is too short
   */
  public synchronized Scnl getScnl() throws MalformedCommandException {
    if (scnl == null) {
      scnl = new Scnl(args[0], args[1], args[2], args[3]);
    }
    return scnl;
  }

  /**
   * Return command time stamp. Timestamp always immediately follows a SCN or SCNL.
   * 
   * @param isScnl If true, I will search for a time span starting at position 4 otherwise I will search starting at position 3
   * @return Request time span
   * @throws MalformedCommandException when arg list is too short
   */
  public synchronized TimeSpan getEwTimeSpan(boolean isScnl) throws MalformedCommandException {
    if (timeSpan == null) {
      int index = isScnl ? 4 : 3;

      long st = (long) (1000 * Double.parseDouble(args[index]));
      long et = (long) (1000 * Double.parseDouble(args[index + 1]));
      timeSpan = new TimeSpan(st, et);
    }

    return timeSpan;
  }

  /**
   * Return command time stamp. Timestamp always immediately follows a SCN or SCNL.
   * 
   * @param isScnl If true, I will search for a time span starting at position 4 otherwise I will search starting at position 3
   * @return Request time span
   * @throws MalformedCommandException when arg list is too short
   */
  public synchronized TimeSpan getJ2kSecTimeSpan(boolean isScnl) throws MalformedCommandException {
    if (timeSpan == null) {
      int index = isScnl ? 4 : 3;

      double st = Double.parseDouble(args[index]);
      double et = Double.parseDouble(args[index + 1]);
      timeSpan = new TimeSpan(J2kSec.asEpoch(st), J2kSec.asEpoch(et));
    }

    return timeSpan;
  }
}
