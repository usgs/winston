/**
 * I waive copyright and related rights in the this work worldwide through the CC0 1.0 Universal
 * public domain dedication. https://creativecommons.org/publicdomain/zero/1.0/legalcode
 */

package gov.usgs.volcanoes.winston.server;

import gov.usgs.volcanoes.core.util.UtilException;
import gov.usgs.volcanoes.winston.db.WinstonDatabase;

/**
 * A consumer of the winston database.
 * 
 * @author Tom Parker
 *
 * @param <T> Type of expected response
 */
public interface WinstonConsumer<T> {
  /**
   * Commands executed while holding a database connection.
   * 
   * @param winston the database
   * @return expected data
   * @throws UtilException when things go wrong
   */
  public T execute(WinstonDatabase winston) throws UtilException;
}
