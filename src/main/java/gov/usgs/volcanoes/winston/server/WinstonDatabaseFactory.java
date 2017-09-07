/**
 * I waive copyright and related rights in the this work worldwide through the CC0 1.0 Universal
 * public domain dedication. https://creativecommons.org/publicdomain/zero/1.0/legalcode
 */

package gov.usgs.volcanoes.winston.server;

import org.apache.commons.pool2.BasePooledObjectFactory;
import org.apache.commons.pool2.PooledObject;
import org.apache.commons.pool2.impl.DefaultPooledObject;

import gov.usgs.volcanoes.core.configfile.ConfigFile;
import gov.usgs.volcanoes.winston.db.WinstonDatabase;

/**
 * Create a WinstonDatabase object.
 * 
 * @author Tom Parker
 *
 */
public class WinstonDatabaseFactory extends BasePooledObjectFactory<WinstonDatabase> {

  final private String driver;
  final private String prefix;
  final private int statementCacheCap;
  final private String tableEngine;
  final private String url;
  final private int maxDays;

  public WinstonDatabaseFactory(ConfigFile config) {
    driver = config.getString("driver");
    url = config.getString("url");
    prefix = config.getString("prefix");
    tableEngine = config.getString("tableEngine");
    statementCacheCap = config.getInt("statementCacheCap");
    maxDays = config.getInt("maxDays");
  }

  @Override
  public WinstonDatabase create() throws Exception {
    WinstonDatabase winston = new WinstonDatabase(driver, url, prefix, tableEngine, statementCacheCap, maxDays);
    return winston;
  }

  @Override
  public PooledObject<WinstonDatabase> wrap(WinstonDatabase obj) {
    return new DefaultPooledObject<WinstonDatabase>(obj);
  }

}
