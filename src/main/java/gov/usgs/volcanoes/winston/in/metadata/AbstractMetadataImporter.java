package gov.usgs.volcanoes.winston.in.metadata;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;

import gov.usgs.volcanoes.core.configfile.ConfigFile;
import gov.usgs.volcanoes.winston.Instrument;
import gov.usgs.volcanoes.winston.db.Channels;
import gov.usgs.volcanoes.winston.db.WinstonDatabase;

/**
 * Import station metadata into instruments table
 *
 * @author Tom Parker
 *
 */

public abstract class AbstractMetadataImporter {
  private static final Logger LOGGER = LoggerFactory.getLogger(AbstractMetadataImporter.class);

  protected final static String DEFAULT_CONFIG_FILE = "Winston.config";

  protected final Channels channels;

  public abstract List<Instrument> readMetadata(String fn);

  protected AbstractMetadataImporter(final String configFile) {
    final ConfigFile cf = new ConfigFile(configFile);
    if (!cf.wasSuccessfullyRead()) {
      System.err.print("Can't read config file " + configFile);
      System.exit(1);
    }

    if (cf.getList("debug") != null) {
      for (final String name : cf.getList("debug")) {
        LOGGER.debug("debugging {}", name);
      }
    }

    final WinstonDatabase winston = new WinstonDatabase(cf.getString("winston.driver"),
        cf.getString("winston.url"), cf.getString("winston.prefix"));
    channels = new Channels(winston);
  }

  public void updateInstruments(final String fileName) {

    for (final Instrument instrument : readMetadata(fileName)) {
      LOGGER.info("updating {}", instrument.toString());
      channels.updateInstrument(instrument);
    }
  }
}
