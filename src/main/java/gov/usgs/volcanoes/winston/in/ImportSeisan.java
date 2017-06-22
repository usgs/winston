package gov.usgs.volcanoes.winston.in;

import gov.usgs.plot.data.Wave;
import gov.usgs.plot.data.file.FileType;
import gov.usgs.plot.data.file.SeismicDataFile;
import gov.usgs.volcanoes.core.legacy.Arguments;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class ImportSeisan extends StaticImporter{

  public ImportSeisan()  {
    // TODO Auto-generated constructor stub
  }

  @Override
  public Map<String, List<Wave>> readFile(String fn) throws IOException {
    
    final SeismicDataFile file = SeismicDataFile.getFile(fn, FileType.SEISAN);
    Map<String, List<Wave>> map = new HashMap<String, List<Wave>>();
    
    try {
      file.read();
      for(String channel: file.getChannels()){
        final List<Wave> list = new ArrayList<Wave>();
        list.add(file.getWave(channel));
        channel = channel.replace('_', '$');
        if(channel.matches("^.*\\$...\\$$")){
          channel = channel+"XX";
        }
        System.out.println(channel);
        map.put(channel, list);
      }
      return map;
    } catch (final Exception e) {
      System.err.println("Error reading file: " + fn);
    }
    
    return null;
  }

  public static void main(final String[] args) {
    final ImportSeisan is = new ImportSeisan();
    final Arguments arguments = new Arguments(args, null, null);
    final List<String> files = arguments.unused();
    process(files, is);
  }
}
