/**
 * I waive copyright and related rights in the this work worldwide
 * through the CC0 1.0 Universal public domain dedication.
 * https://creativecommons.org/publicdomain/zero/1.0/legalcode
 */

package gov.usgs.volcanoes.winston.server.wws;

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
  private final String commandString;
  private final String command;
  private final String[] commandSplits;

  /**
   * Constructor.
   * @param commandString my command string
   */
  public WwsCommandString(final String commandString) {
    this.commandString = commandString;
    commandSplits = commandString.split(" ");
    String cmd = commandString;
    int cmdEnd = cmd.indexOf(' ');
    if (cmdEnd != -1) {
      cmd = cmd.substring(0, cmdEnd);
    }
    if (cmd.endsWith(":")) {
      cmd = cmd.substring(0, cmd.length() - 1);
    }

    command = cmd;
  }

  /**
   * Command name accessor.
   * @return the command name
   */
  public String getCommand() {
    return command;
  }

  /**
   * Full command string accessor.
   * @return the full command string
   */
  public String getCommandString() {
    return commandString;
  }

  /**
   * Command token accessor.
   * @return command tokens
   */
  public String[] getCommandSplits() {
    return commandSplits;
  }

  /**
   * Command token accessor.
   * @param i token index
   * @return command token
   */
  public String getString(final int i) {
    if (i >= commandSplits.length)
      return null;
    else
      return commandSplits[i];
  }

  /**
   * Command token accessor
   * @param i token index
   * @return command token
   */
  public int getInt(final int i) {
    int result = Integer.MIN_VALUE;
    try {
      result = Integer.parseInt(commandSplits[i]);
    } catch (final Exception e) {
    }
    return result;
  }

  /**
   * Command token accessor
   * @param i token index
   * @return command token
   */
  public double getDouble(final int i) {
    double result = Double.NaN;
    try {
      result = Double.parseDouble(commandSplits[i]);
    } catch (final Exception e) {
    }
    return result;
  }

  /**
   * Id accessor.
   * @return the Id
   */
  public String getID() {
    if (commandSplits.length < 2)
      return null;
    else
      return commandSplits[1];
  }

  /**
   * $-delimited SNL accessor.
   * @return the SCNL
   */
  public String getWinstonSCNL() {
    if (commandSplits.length < 6)
      return null;
    else {
      String loc = "";
      if (!commandSplits[5].equals("--"))
        loc = "$" + commandSplits[5];
      return commandSplits[2] + "$" + commandSplits[3] + "$" + commandSplits[4] + loc;
    }
  }

  /**
   * startTime accessor.
   * @param isScnl if true assume location code is present
   * @return the start time
   */
  public double getT1(final boolean isScnl) {
    int ofs = 0;
    if (isScnl)
      ofs = 1;
    if (commandSplits.length < 6 + ofs)
      return Double.NaN;
    else
      return getDouble(5 + ofs);
  }
  
  /**
   * endTime accessor.
   * @param isScnl if true assume location code is present
   * @return the end time
   */
  public double getT2(final boolean isScnl) {
    int ofs = 0;
    if (isScnl)
      ofs = 1;
    if (commandSplits.length < 7 + ofs)
      return Double.NaN;
    else
      return getDouble(6 + ofs);
  }

  /**
   * token count accessor.
   * @return number of tokens
   */
  public int length() {
    return commandSplits.length;
  }

  /**
   * Validate token count.
   * @param cnt number of tokens
   * @return true if command has expected number of tokens
   */
  public boolean isLegal(final int cnt) {
    return commandSplits.length == cnt;
  }

  /**
   * Validate token count and times.
   * @param cnt number of tokens
   * @return true if token count correct and times are found
   */
  public boolean isLegalSCNTT(final int cnt) {
    if (commandSplits.length != cnt)
      return false;

    if (Double.isNaN(getT1(false)) || Double.isNaN(getT2(false)))
      return false;

    return true;
  }

  /**
   * Validate token count and times.
   * @param cnt number of tokens
   * @return true if token count correct and times are found
   */
  public boolean isLegalSCNLTT(final int cnt) {
    if (commandSplits.length != cnt)
      return false;

    if (Double.isNaN(getT1(true)) || Double.isNaN(getT2(true)))
      return false;

    return true;
  }

  /**
   * Station accessor.
   * @return station
   */
  public String getS() {
    if (commandSplits.length <= 2)
      return null;

    return commandSplits[2];
  }

  /**
   * Channel accessor.
   * @return accessor
   */
  public String getC() {
    if (commandSplits.length <= 3)
      return null;

    return commandSplits[3];
  }

  /**
   * Network accessor.
   * @return network
   */
  public String getN() {
    if (commandSplits.length <= 4)
      return null;

    return commandSplits[4];
  }

  /**
   * Location accessor.
   * @return location
   */
  public String getL() {
    if (commandSplits.length <= 5)
      return null;

    return commandSplits[5];
  }

  // TODO: fix getL() below.
  public String getEarthwormErrorString(final int sid, final String msg) {
    return getID() + " " + sid + " " + getS() + " " + getC() + " " + getN() + " " + getL() + " "
        + msg + "\n";
  }
}
