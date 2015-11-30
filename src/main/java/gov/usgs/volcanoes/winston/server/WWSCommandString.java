package gov.usgs.volcanoes.winston.server;

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
public class WWSCommandString {
  private final String command;
  private final String[] commandSplits;

  public WWSCommandString(final String cmd) {
    command = cmd;
    commandSplits = command.split(" ");
  }

  public String getCommand() {
    return command;
  }

  public String[] getCommandSplits() {
    return commandSplits;
  }

  public String getString(final int i) {
    if (i >= commandSplits.length)
      return null;
    else
      return commandSplits[i];
  }

  public int getInt(final int i) {
    int result = Integer.MIN_VALUE;
    try {
      result = Integer.parseInt(commandSplits[i]);
    } catch (final Exception e) {
    }
    return result;
  }

  public double getDouble(final int i) {
    double result = Double.NaN;
    try {
      result = Double.parseDouble(commandSplits[i]);
    } catch (final Exception e) {
    }
    return result;
  }

  public String getID() {
    if (commandSplits.length < 2)
      return null;
    else
      return commandSplits[1];
  }

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

  public double getT1(final boolean isScnl) {
    int ofs = 0;
    if (isScnl)
      ofs = 1;
    if (commandSplits.length < 6 + ofs)
      return Double.NaN;
    else
      return getDouble(5 + ofs);
  }

  public double getT2(final boolean isScnl) {
    int ofs = 0;
    if (isScnl)
      ofs = 1;
    if (commandSplits.length < 7 + ofs)
      return Double.NaN;
    else
      return getDouble(6 + ofs);
  }

  public int length() {
    return commandSplits.length;
  }

  public boolean isLegal(final int cnt) {
    return commandSplits.length == cnt;
  }

  public boolean isLegalSCNTT(final int cnt) {
    if (commandSplits.length != cnt)
      return false;

    if (Double.isNaN(getT1(false)) || Double.isNaN(getT2(false)))
      return false;

    return true;
  }

  public boolean isLegalSCNLTT(final int cnt) {
    if (commandSplits.length != cnt)
      return false;

    if (Double.isNaN(getT1(true)) || Double.isNaN(getT2(true)))
      return false;

    return true;
  }

  public String getS() {
    if (commandSplits.length <= 2)
      return null;

    return commandSplits[2];
  }

  public String getC() {
    if (commandSplits.length <= 3)
      return null;

    return commandSplits[3];
  }

  public String getN() {
    if (commandSplits.length <= 4)
      return null;

    return commandSplits[4];
  }

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
