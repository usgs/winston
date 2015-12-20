package gov.usgs.volcanoes.winston.server.wwsCmd;

import gov.usgs.volcanoes.core.util.UtilException;
import gov.usgs.volcanoes.winston.db.WinstonDatabase;

public interface WinstonConsumer<T> {
  public T execute(WinstonDatabase winston) throws UtilException;
}
