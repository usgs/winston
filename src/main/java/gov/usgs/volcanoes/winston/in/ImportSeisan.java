package gov.usgs.volcanoes.winston.in;

import gov.usgs.plot.data.Wave;
import gov.usgs.plot.data.file.FileType;
import gov.usgs.plot.data.file.SeismicDataFile;
import gov.usgs.volcanoes.core.legacy.Arguments;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

public class ImportSeisan extends StaticImporter{

  private String network="XX";
  
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
          channel = channel+network;
        }
         map.put(channel, list);
      }
      return map;
    } catch (final Exception e) {
      System.err.println("Error reading file: " + fn);
    }
    
    return null;
  }

  public static void main(final String[] args) {

    instructions.append("Winston ImportSeisan\n\n");
    instructions.append("This program imports data from Seisan files into a Winston database.\n");
    instructions.append("Information about connecting to the Winston database must be present\n");
    instructions.append("in Winston.config in the current directory.\n\n");
    instructions.append("Usage:\n");
    instructions.append("  java gov.usgs.volcanoes.winston.in.ImportSeisan -n [network code override] [files]\n");

    final ImportSeisan is = new ImportSeisan();
    final Set<String> kvs = is.getArgumentSet();
    kvs.add("-n");
    final Set<String> flags = new HashSet<String>();

    final Arguments arguments = new Arguments(args, flags, kvs);
    String network = arguments.get("-n");
    if(network != null){
      is.network = network;
    }
    final List<String> files = arguments.unused();
    process(files, is);
  }
}
