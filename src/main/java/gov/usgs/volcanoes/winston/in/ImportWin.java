package gov.usgs.volcanoes.winston.in;

import gov.usgs.plot.data.Wave;
import gov.usgs.plot.data.file.FileType;
import gov.usgs.plot.data.file.SeismicDataFile;
import gov.usgs.plot.data.file.WinDataFile;
import gov.usgs.volcanoes.core.legacy.Arguments;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * Import WIN file into Winston.
 * @author Diana Norgaard
 */
public class ImportWin extends StaticImporter {

  @Override
  public Map<String, List<Wave>> readFile(String fn) throws IOException {
    final SeismicDataFile file = SeismicDataFile.getFile(fn, FileType.WIN);
    Map<String, List<Wave>> map = new HashMap<String, List<Wave>>();

    file.read();
    for (String channel : file.getChannels()) {
      final List<Wave> list = new ArrayList<Wave>();
      list.add(file.getWave(channel));
      channel = channel.replace('_', '$');
      map.put(channel, list);
    }
    return map;
  }

  public static void main(final String[] args) {

    instructions.append("Winston ImportWin\n\n");
    instructions.append("This program imports data from WIN files into a Winston database.\n");
    instructions.append("Information about connecting to the Winston database must be present\n");
    instructions.append("in Winston.config in the current directory.\n\n");
    instructions.append("Usage:\n");
    instructions.append(" java gov.usgs.volcanoes.winston.in.ImportWin -c [config file] [files]\n");


    final ImportWin is = new ImportWin();
    final Set<String> kvs = is.getArgumentSet();
    kvs.add("-c");
    final Set<String> flags = new HashSet<String>();

    final Arguments arguments = new Arguments(args, flags, kvs);
    final List<String> files = arguments.unused();

    is.processArguments(arguments);
    String cf = arguments.get("-c");
    WinDataFile.configFile = new File(cf);

    process(files, is);
  }
}
