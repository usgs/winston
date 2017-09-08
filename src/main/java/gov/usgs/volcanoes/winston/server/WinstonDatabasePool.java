/**
 * I waive copyright and related rights in the this work worldwide through the CC0 1.0 Universal
 * public domain dedication. https://creativecommons.org/publicdomain/zero/1.0/legalcode
 */

package gov.usgs.volcanoes.winston.server;

import org.apache.commons.pool2.impl.GenericObjectPool;
import org.apache.commons.pool2.impl.GenericObjectPoolConfig;

import gov.usgs.volcanoes.core.configfile.ConfigFile;
import gov.usgs.volcanoes.core.util.UtilException;
import gov.usgs.volcanoes.winston.db.WinstonDatabase;
import gov.usgs.volcanoes.winston.server.wws.WinstonConsumer;

/**
 * Pool connections to a winston database.
 * 
 * @author Tom Parker
 *
 */
public final class WinstonDatabasePool extends GenericObjectPool<WinstonDatabase> {

  /**
   * Constructor.
   * @param configFile winston config file
   * @param poolConfig pool config
   */
  public WinstonDatabasePool(ConfigFile configFile, GenericObjectPoolConfig poolConfig) {
    super(new WinstonDatabaseFactory(configFile), poolConfig);
  }

  /**
   * Execute a database query.
   * 
   * @param consumer data consumer
   * @return result
   * @throws Exception when things go wrong
   */
  public <T> T doCommand(WinstonConsumer<T> consumer) throws Exception {
    WinstonDatabase winston = null;
    try {
      winston = borrowObject();
      if (!winston.checkConnect()) {
        throw new UtilException("Unable to connect to MySQL.");
      } else {
        return consumer.execute(winston);
      }
    } finally {
      if (winston != null) {
        returnObject(winston);
      }
    }
  }
}
