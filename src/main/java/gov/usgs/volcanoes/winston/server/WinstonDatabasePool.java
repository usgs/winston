/**
 * I waive copyright and related rights in the this work worldwide
 * through the CC0 1.0 Universal public domain dedication.
 * https://creativecommons.org/publicdomain/zero/1.0/legalcode
 */

package gov.usgs.volcanoes.winston.server;

import org.apache.commons.pool2.impl.GenericObjectPool;
import org.apache.commons.pool2.impl.GenericObjectPoolConfig;

import gov.usgs.volcanoes.core.configfile.ConfigFile;
import gov.usgs.volcanoes.winston.db.WinstonDatabase;

public final class WinstonDatabasePool extends GenericObjectPool<WinstonDatabase> {

  public WinstonDatabasePool(ConfigFile configFile, GenericObjectPoolConfig poolConfig) {
    super(new WinstonDatabaseFactory(configFile), poolConfig);
  }
}
