package gov.usgs.volcanoes.winston.in;

import java.io.IOException;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import gov.usgs.volcanoes.core.data.Wave;
import gov.usgs.volcanoes.core.data.file.FileType;
import gov.usgs.volcanoes.core.data.file.SeismicDataFile;
import gov.usgs.volcanoes.core.legacy.Arguments;

/**
 *
 * @author Dan Cervelli
 */
public class ImportSAC extends StaticImporter {
  private String channel;
  private boolean fixTrimble;


  public ImportSAC() {
    super();
  }
  
  @Override
  public Map<String, List<Wave>> readFile(final String fn) {
    final SeismicDataFile file = SeismicDataFile.getFile(fn, FileType.SAC);

    try {
      file.read();

      final String ch = file.getChannels().iterator().next();
      if (channel == null) {
        channel = ch.replace('_', '$');
      }

      final Wave sw = file.getWave(ch);
      // compensate for incorrect Trimble leap second.
      // subtract 1 second from all data after 23:49:59 UTC on August 3,
      // 2005.
      if (fixTrimble && sw.getStartTime() > 1.76384999E8 && sw.getStartTime() < 1.893456E8)
        sw.setStartTime(sw.getStartTime() - 1);
      final List<Wave> list = sw.split(1000);
      final Map<String, List<Wave>> map = new HashMap<String, List<Wave>>();
      map.put(channel, list);
      return map;
    } catch (final IOException e) {
      System.err.println("Error reading file: " + fn);
    }
    return null;
  }

  public void setChannel(final String s) {
    channel = s;
  }

  public static void main(final String[] as) {
    instructions.append("Winston ImportSAC\n\n");
    instructions.append("This program imports data from SAC files into a Winston database.\n");
    instructions.append("Information about connecting to the Winston database must be present\n");
    instructions.append("in Winston.config in the current directory.\n\n");
    instructions.append("Usage:\n");
    instructions.append("  java gov.usgs.volcanoes.winston.in.ImportSAC [files]\n");

    final ImportSAC is = new ImportSAC();

    final Set<String> kvs = is.getArgumentSet();
    kvs.add("-c");
    final Set<String> flags = new HashSet<String>();
    flags.add("-f");

    final Arguments args = new Arguments(as, flags, kvs);
    final List<String> files = args.unused();

    is.processArguments(args);
    is.channel = args.get("-c");

    if (args.flagged("-f")) {
      System.out.println(
          "Using fix Trimble flag (-1 second from all data after 2005-08-03 23:49:59 UTC.)");
      is.fixTrimble = true;
    }

    process(files, is);
  }
}
