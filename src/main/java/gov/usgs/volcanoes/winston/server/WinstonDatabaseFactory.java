package gov.usgs.volcanoes.winston.server;

import org.apache.commons.pool2.BasePooledObjectFactory;
import org.apache.commons.pool2.PooledObject;
import org.apache.commons.pool2.PooledObjectFactory;
import org.apache.commons.pool2.impl.DefaultPooledObject;

import gov.usgs.volcanoes.core.configfile.ConfigFile;
import gov.usgs.volcanoes.winston.db.WaveServerEmulator;
import gov.usgs.volcanoes.winston.db.WinstonDatabase;

public class WinstonDatabaseFactory extends BasePooledObjectFactory<WinstonDatabase> {

  final String driver;
  final String url;
  final String prefix;
  final String tableEngine;
  final int statementCacheCap;

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

  @Override
  public PooledObject<WinstonDatabase> wrap(WinstonDatabase obj) {
    return new DefaultPooledObject<WinstonDatabase>(obj);
  }

  /**
   * TODO: make sure there's nothing to do here.
   */
  @Override
  public void passivateObject(PooledObject<WinstonDatabase> pooledObject) {
  }

}
