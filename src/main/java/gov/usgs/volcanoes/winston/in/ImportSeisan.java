package gov.usgs.volcanoes.winston.in;

import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import gov.usgs.plot.data.Wave;
import gov.usgs.plot.data.file.FileType;
import gov.usgs.plot.data.file.SeismicDataFile;
import gov.usgs.volcanoes.core.legacy.Arguments;

/**
 *
 * @author Tom Parker
 */
public class ImportSeisan extends StaticImporter {
  private String channel;

  @Override
  public Map<String, List<Wave>> readFile(final String fn) {
    final SeismicDataFile file = SeismicDataFile.getFile(fn, FileType.SEISAN);

    try {
      file.read();

      final String ch = file.getChannels().iterator().next();
      
      if (channel == null) {
        channel = ch.replace('_', '$');
      }
      
      System.out.println("Channel " + channel);
      
      final Wave sw = file.getWave(ch);
      final List<Wave> list = sw.split(1000);
      final Map<String, List<Wave>> map = new HashMap<String, List<Wave>>();
      map.put(channel, list);
      return map;
    } catch (final Exception e) {
      System.err.println("Error reading file: " + fn);
    }
    return null;
  }

  public void setChannel(final String s) {
    channel = s;
  }

  public static void main(final String[] as) {
    instructions.append("Winston ImportSeisan\n\n");
    instructions.append("This program imports data from Seisan files into a Winston database.\n");
    instructions.append("Information about connecting to the Winston database must be present\n");
    instructions.append("in Winston.config in the current directory.\n\n");
    instructions.append("Usage:\n");
    instructions.append("  java gov.usgs.volcanoes.winston.in.ImportSeisan [files]\n");

    final ImportSeisan is = new ImportSeisan();

    final Set<String> kvs = is.getArgumentSet();
    kvs.add("-c");
    final Set<String> flags = new HashSet<String>();
    flags.add("-f");

    final Arguments args = new Arguments(as, flags, kvs);
    final List<String> files = args.unused();

    is.processArguments(args);
    is.channel = args.get("-c");

    process(files, is);
  }
}