/**
 * I waive copyright and related rights in the this work worldwide through the CC0 1.0 Universal
 * public domain dedication. https://creativecommons.org/publicdomain/zero/1.0/legalcode
 */

package gov.usgs.volcanoes.winston.server.http.cmd.fdsnws.constraint;

import java.util.Map;

import gov.usgs.volcanoes.winston.Channel;

/**
 * A constraint on results returned from a FDSN WS query.
 * 
 * @author Tom Parker
 *
 */
public abstract class FdsnConstraint {
  
  /**
   * Match channel.
   * @param chan channel
   * @return true if channel matches this constraint
   */
  public abstract boolean matches(Channel chan);
  
  /**
   * Non-matching objects should be pruned.
   * 
   * @return if true, non-matching objects should be pruned
   */
  public boolean isTerminal() {
    return true;
  }
  
  protected static String getArg(Map<String, String> arguments, final String s1, final String s2) {
    String arg = arguments.get(s1);
    if (arg == null)
      arg = arguments.get(s2);

    return arg;
  }
}
