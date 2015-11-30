package gov.usgs.volcanoes.winston.in;

import com.martiansoftware.jsap.FlaggedOption;
import com.martiansoftware.jsap.JSAP;
import com.martiansoftware.jsap.JSAPResult;
import com.martiansoftware.jsap.Parameter;
import com.martiansoftware.jsap.SimpleJSAP;
import com.martiansoftware.jsap.UnflaggedOption;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import gov.usgs.plot.data.Wave;
import gov.usgs.plot.data.file.FileType;
import gov.usgs.plot.data.file.SeismicDataFile;

/**
 *
 * @author Dan Cervelli
 */
public class ImportSeed extends StaticImporter {

  public static final String JSAP_PROGRAM_NAME = "java gov.usgs.winston.in.ImportSeed";

  public static final String JSAP_EXPLANATION = "Import miniSEED\n" + "\n"
      + "This program imports data from miniSEED volumes into a winston database\n";

  private static final Parameter[] JSAP_PARAMETERS = new Parameter[] {
      new FlaggedOption("station", JSAP.STRING_PARSER, JSAP.NO_DEFAULT, JSAP.NOT_REQUIRED, 's',
          "station", "Override station identifier code\n"),
      new FlaggedOption("channel", JSAP.STRING_PARSER, JSAP.NO_DEFAULT, JSAP.NOT_REQUIRED, 'c',
          "channel", "Override channel identifier\n"),
      new FlaggedOption("network", JSAP.STRING_PARSER, JSAP.NO_DEFAULT, JSAP.NOT_REQUIRED, 'n',
          "network", "Override network identifier\n"),
      new FlaggedOption("location", JSAP.STRING_PARSER, JSAP.NO_DEFAULT, JSAP.NOT_REQUIRED, 'l',
          "location", "Override location identifier\n"),
      new FlaggedOption("rsamDelta", JSAP.INTEGER_PARSER, "10", JSAP.NOT_REQUIRED, 'r', "rsamDelta",
          "Override location identifier\n"),
      new FlaggedOption("rsamDuration", JSAP.INTEGER_PARSER, "60", JSAP.NOT_REQUIRED, 'd',
          "rsamDuration", "Override location identifier\n"),
      new UnflaggedOption("file", JSAP.STRING_PARSER, JSAP.NO_DEFAULT, JSAP.NOT_REQUIRED,
          JSAP.GREEDY, "files to import.")};

  private String network;
  private String station;
  private String channel;
  private String location;

  public ImportSeed() {
    super();
  }

  public ImportSeed(final JSAPResult config) {
    this();
    network = config.getString("network");
    station = config.getString("station");
    channel = config.getString("channel");
    location = config.getString("location");
    rsamDelta = config.getInt("rsamDelta");
    rsamDuration = config.getInt("rsamDuration");
  }

  @Override
  public Map<String, List<Wave>> readFile(final String fn) throws IOException {
    final Map<String, List<Wave>> map = new HashMap<String, List<Wave>>();

    final SeismicDataFile file = SeismicDataFile.getFile(fn, FileType.SEED);
    file.setNetwork(network);
    file.setStation(station);
    file.setChannel(channel);
    file.setLocation(location);

    file.read();
    for (final String ch : file.getChannels()) {
      final List<Wave> list = new ArrayList<Wave>();
      list.add(file.getWave(ch));
      map.put(ch, list);
    }

    return map;
  }

  public static JSAPResult getArguments(final String[] args) {
    JSAPResult config = null;
    try {
      final SimpleJSAP jsap = new SimpleJSAP(JSAP_PROGRAM_NAME, JSAP_EXPLANATION, JSAP_PARAMETERS);

      config = jsap.parse(args);

      if (jsap.messagePrinted() || config.getStringArray("file").length == 0) {
        // The following error message is useful for catching the case
        // when args are missing, but help isn't printed.
        if (!config.getBoolean("help"))
          System.err.println("Try using the --help flag.");

        System.exit(1);
      }
    } catch (final Exception ex) {
      ex.printStackTrace();
      System.exit(1);
    }
    return config;
  }

  public static void main(final String[] args) {
    final JSAPResult config = getArguments(args);
    System.out.printf("RSAM parameters: delta=%d, duration=%d.\n", config.getInt("rsamDelta"),
        config.getInt("rsamDuration"));

    final ImportSeed is = new ImportSeed(config);

    final List<String> files = new ArrayList<String>();
    for (final String file : config.getStringArray("file"))
      files.add(file);

    process(files, is);
  }
}
