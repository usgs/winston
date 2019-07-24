package gov.usgs.volcanoes.winston.in.metadata;

import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;
import java.util.LinkedList;
import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import gov.usgs.volcanoes.winston.Instrument;


/**
 * Import station metadata from hypoinverse station file
 *
 * Format taken from:
 * http://folkworm.ceri.memphis.edu/ew-doc/USER_GUIDE/hypoinv_sta.html
 *
 * @author Tom Parker
 *
 */
public class ImportHypoinverse extends AbstractMetadataImporter {
  private static final Logger LOGGER = LoggerFactory.getLogger(ImportHypoinverse.class);

  public ImportHypoinverse(final String configFile) {
    super(configFile);
  }

  @Override
  public List<Instrument> readMetadata(final String fn) {
    LOGGER.info("Reading {}", fn);

    final List<Instrument> list = new LinkedList<Instrument>();
    BufferedReader in = null;
    try {
      String record;
      in = new BufferedReader(new FileReader(fn));
      record = in.readLine();

      while (record != null) {
        Instrument.Builder builder = new Instrument.Builder();
        builder.name(record.substring(0, 5).trim());

        double lat = Double.parseDouble(record.substring(15, 17));
        lat += Double.parseDouble(record.substring(18, 25).trim()) / 60;
        if (record.substring(25, 26).equals("S"))
          lat *= -1;
        builder.latitude(lat);

        double lon = Double.parseDouble(record.substring(26, 29).trim());
        lon += Double.parseDouble(record.substring(30, 36).trim()) / 60;
        if (!record.substring(37, 38).equals("E"))
          lon *= -1;
        builder.longitude(lon);

        final int height = Integer.parseInt(record.substring(38, 42).trim());
        builder.height(height);

        list.add(builder.build());
        record = in.readLine();

      }
      in.close();
    } catch (final IOException e) {
      e.printStackTrace();
    } finally {
      if (in != null) {
        try {
          in.close();
        } catch (IOException e) {
          throw new RuntimeException(e);
        }
      }
    }
    return list;
  }

  /**
   * @param args
   */
  public static void main(final String[] args) {
    String configFile;
    String dataless;

    if (args.length == 0) {
      System.err.println("Usage: ImportDataless [-c <winston.config>] <dataless>");
      System.exit(1);
    }

    if (args[0].equals("-c")) {
      configFile = args[1];
      dataless = args[2];
    } else {
      configFile = DEFAULT_CONFIG_FILE;
      dataless = args[0];
    }

    final ImportHypoinverse imp = new ImportHypoinverse(configFile);
    imp.updateInstruments(dataless);
  }

}
