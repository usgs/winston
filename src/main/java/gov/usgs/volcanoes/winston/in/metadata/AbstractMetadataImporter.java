package gov.usgs.volcanoes.winston.in.metadata;

import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;

import gov.usgs.util.ConfigFile;
import gov.usgs.util.Log;
import gov.usgs.winston.Instrument;
import gov.usgs.winston.db.Channels;
import gov.usgs.winston.db.WinstonDatabase;

/**
 * Import station metadata into instruments table
 *
 * @author Tom Parker
 *
 */

public abstract class AbstractMetadataImporter {

  protected final static Logger LOGGER = Log.getLogger(AbstractMetadataImporter.class.getName());
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
        final Logger l = Log.getLogger(name);
        l.setLevel(Level.ALL);
        LOGGER.fine("debugging " + name);
      }
    }

    final WinstonDatabase winston = new WinstonDatabase(cf.getString("winston.driver"),
        cf.getString("winston.url"), cf.getString("winston.prefix"));
    channels = new Channels(winston);
  }

  public void updateInstruments(final String fileName) {

    for (final Instrument instrument : readMetadata(fileName)) {
      LOGGER.fine("updating " + instrument.toString());
      channels.updateInstrument(instrument);
    }
  }
}
