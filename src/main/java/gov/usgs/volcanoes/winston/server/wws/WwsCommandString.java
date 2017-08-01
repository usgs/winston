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
 * <args>  = [ <channel spec> ] <sp> [ <command-specific args> ]
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
  public final String commandString;
  public final String command;
  private Scnl scnl;
  private TimeSpan timeSpan;
  public final String id;
  public final String[] args;

  /**
   * Constructor.
   * @param commandString my command string
   */
  public WwsCommandString(final String commandString) {
    this.commandString = commandString;
    String[] commandSplits = commandString.split(" ");
    String cmd = commandSplits[0];
    if (cmd.endsWith(":")) {
      command = cmd.substring(0, cmd.length() - 1);
    } else {
      command = cmd;
    }

    id = commandSplits[1];

    if (commandSplits.length > 2) {
      args = Arrays.copyOfRange(commandSplits, 2, commandSplits.length - 1);
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
    } catch (final  NumberFormatException ex) {
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
   * Return command SCN or SCNL. This will always be the first arg.
   * 
   * @return the scnl channel requested
   * @throws MalformedCommandException when arg list is too short
   */
  public synchronized Scnl getScnl() throws MalformedCommandException {
    if (scnl == null) {
      if (args.length > 3) {
        scnl = new Scnl(args[0], args[1], args[2], args[3]);
      } else if (args.length > 2) {
        scnl = new Scnl(args[0], args[1], args[2]);
      } else {
        throw new MalformedCommandException("Not enough args for SCN");
      }
    }
    return scnl;
  }

  /**
   * Return command time stamp. Timestamp always immediately follows a SCN or SCNL.
   * 
   * @return Request time span
   * @throws MalformedCommandException when arg list is too short
   */
  public synchronized TimeSpan getTimeSpan() throws MalformedCommandException {
    if (timeSpan == null) {
      int index = Integer.MAX_VALUE;
      if (args.length > 5) {
        index = 4;
      } else if (args.length > 4) {
        index = 3;
      } else {
        throw new MalformedCommandException("Can't find time in arg list.");
      }

      double st = Double.parseDouble(args[index]);
      double et = Double.parseDouble(args[index + 1]);
      timeSpan = new TimeSpan(J2kSec.asEpoch(st), J2kSec.asEpoch(et));
    }

    return timeSpan;
  }
}
