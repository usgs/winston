/**
 * I waive copyright and related rights in the this work worldwide
 * through the CC0 1.0 Universal public domain dedication.
 * https://creativecommons.org/publicdomain/zero/1.0/legalcode
 */

package gov.usgs.volcanoes.winston.server.wws;

import java.util.Arrays;

import gov.usgs.volcanoes.core.data.Scnl;

/**
 * A convenience class for dealing with WWS commands.
 * fields are separated by a single space
 * position 0: command optionally followed by a colon
 * position 1: id
 * positions 2 through 4: SCN
 * positions 2 through 5: SCNL
 * position 5 or 6: start time
 * position 6 or 7: end time
 *
 * @author Dan Cervelli
 */
public class WwsCommandString {
  public final String commandString;
  public final String command;
  public final Scnl scnl;
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
    
    if (commandSplits.length > 5) {
      scnl = new Scnl(commandSplits[2], commandSplits[3], commandSplits[4], commandSplits[5]);
      args = Arrays.copyOfRange(commandSplits, 6, commandSplits.length + 1);
    } else if (commandSplits.length > 4) {
      scnl = new Scnl(commandSplits[2], commandSplits[3], commandSplits[4]);
      args = Arrays.copyOfRange(commandSplits, 5, commandSplits.length + 1);
    } else {
      scnl = null;
      if (commandSplits.length > 1) {
        args = Arrays.copyOfRange(commandSplits, 0, commandSplits.length + 1);
      } else {
        args = null;
      }
    }
  }

  /**
   * Command token accessor.
   * @param i token index
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
   */
  public int getInt(int index) {
    index = getIndex(index);
    int result = Integer.MIN_VALUE;
    try {
      result = Integer.parseInt(args[index]);
    } catch (final Exception e) {
    }
    return result;
  }

  /**
   * Command token accessor
   * @param i token index
   * @return command token
   */
  public double getDouble(int index) {
    index = getIndex(index);
    double result = Double.NaN;
    try {
      result = Double.parseDouble(args[index]);
    } catch (final Exception e) {
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
   * startTime accessor.
   * @param isScnl if true assume location code is present
   * @return the start time
   */
  public double getT1() {
    return Double.parseDouble(args[0]);
  }

  /**
   * endTime accessor.
   * @param isScnl if true assume location code is present
   * @return the end time
   */
  public double getT2() {
    return Double.parseDouble(args[1]);
  }


  /**
   * Validate token count and times.
   * @param cnt number of tokens
   * @return true if token count correct and times are found
   */
  public boolean isLegalSCNTT(final int cnt) {
    if (args.length != cnt)
      return false;

    if (Double.isNaN(getT1()) || Double.isNaN(getT2()))
      return false;

    return true;
  }

  /**
   * Validate token count and times.
   * @param cnt number of tokens
   * @return true if token count correct and times are found
   */
  public boolean isLegalSCNLTT(final int cnt) {
    if (args.length != cnt)
      return false;

    if (Double.isNaN(getT1()) || Double.isNaN(getT2()))
      return false;

    return true;
  }
}
