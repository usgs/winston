/**
 * I waive copyright and related rights in the this work worldwide
 * through the CC0 1.0 Universal public domain dedication.
 * https://creativecommons.org/publicdomain/zero/1.0/legalcode
 */

package gov.usgs.volcanoes.winston.server;

import org.apache.commons.pool2.BasePooledObjectFactory;
import org.apache.commons.pool2.PooledObject;
import org.apache.commons.pool2.impl.DefaultPooledObject;

import gov.usgs.volcanoes.core.configfile.ConfigFile;
import gov.usgs.volcanoes.winston.db.WinstonDatabase;

public class WinstonDatabaseFactory extends BasePooledObjectFactory<WinstonDatabase> {

  final String driver;
  final String prefix;
  final int statementCacheCap;
  final String tableEngine;
  final String url;

  public WinstonDatabaseFactory(ConfigFile config) {
    driver = config.getString("driver");
    url = config.getString("url");
    prefix = config.getString("prefix");
    tableEngine = config.getString("tableEngine");
    statementCacheCap = config.getInt("statementCacheCap");
  }

  @Override
  public WinstonDatabase create() throws Exception {
    return new WinstonDatabase(driver, url, prefix, tableEngine, statementCacheCap);
  }

  /**
   * TODO: make sure there's nothing to do here.
   */
  @Override
  public void passivateObject(PooledObject<WinstonDatabase> pooledObject) {}

  @Override
  public PooledObject<WinstonDatabase> wrap(WinstonDatabase obj) {
    return new DefaultPooledObject<WinstonDatabase>(obj);
  }

}
