package gov.usgs.volcanoes.winston.server;

import org.apache.commons.pool2.impl.GenericObjectPool;
import org.apache.commons.pool2.impl.GenericObjectPoolConfig;

import gov.usgs.volcanoes.core.configfile.ConfigFile;
import gov.usgs.volcanoes.winston.db.WinstonDatabase;

public final class WinstonDatabasePool extends GenericObjectPool<WinstonDatabase> {
  
  public WinstonDatabasePool(ConfigFile configFile) {
    super(new WinstonDatabaseFactory(configFile));
  }

}