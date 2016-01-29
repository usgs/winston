/**
 * I waive copyright and related rights in the this work worldwide through the CC0 1.0 Universal
 * public domain dedication. https://creativecommons.org/publicdomain/zero/1.0/legalcode
 */

package gov.usgs.volcanoes.winston.server.http.cmd.fdsnws.constraint;

import gov.usgs.volcanoes.winston.Channel;

/**
 * A constraint on results returned from a FDSN WS query.
 * 
 * @author Tom Parker
 *
 */
public interface FdsnConstraint {
  
  /**
   * Match channel.
   * @param chan channel
   * @return true if channel matches this constraint
   */
  public abstract boolean matches(Channel chan);
}
