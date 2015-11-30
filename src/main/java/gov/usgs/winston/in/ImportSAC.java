package gov.usgs.winston.in;

import gov.usgs.plot.data.Wave;
import gov.usgs.plot.data.file.FileType;
import gov.usgs.plot.data.file.SeismicDataFile;
import gov.usgs.util.Arguments;

import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * 
 * @author Dan Cervelli
 */
public class ImportSAC extends StaticImporter {
    private String channel;
    private boolean fixTrimble;

    // private List<String> files;

    public Map<String, List<Wave>> readFile(String fn) {
        SeismicDataFile file = SeismicDataFile.getFile(fn, FileType.SAC);

        try {
            file.read();

            String ch = file.getChannels().iterator().next();
            if (channel == null) {
                channel = ch.replace('_', '$');
            }

            Wave sw = file.getWave(ch);
            // compensate for incorrect Trimble leap second.
            // subtract 1 second from all data after 23:49:59 UTC on August 3,
            // 2005.
            if (fixTrimble && sw.getStartTime() > 1.76384999E8 && sw.getStartTime() < 1.893456E8)
                sw.setStartTime(sw.getStartTime() - 1);
            List<Wave> list = sw.split(1000);
            Map<String, List<Wave>> map = new HashMap<String, List<Wave>>();
            map.put(ch, list);
            return map;
        } catch (Exception e) {
            System.err.println("Error reading file: " + fn);
        }
        return null;
    }

    public void setChannel(String s) {
        channel = s;
    }

    public static void main(String[] as) {
        instructions.append("Winston ImportSAC\n\n");
        instructions.append("This program imports data from SAC files into a Winston database.\n");
        instructions.append("Information about connecting to the Winston database must be present\n");
        instructions.append("in Winston.config in the current directory.\n\n");
        instructions.append("Usage:\n");
        instructions.append("  java gov.usgs.winston.in.ImportSAC [files]\n");

        ImportSAC is = new ImportSAC();

        Set<String> kvs = is.getArgumentSet();
        kvs.add("-c");
        Set<String> flags = new HashSet<String>();
        flags.add("-f");

        Arguments args = new Arguments(as, flags, kvs);
        List<String> files = args.unused();

        is.processArguments(args);
        is.channel = args.get("-c");

        if (args.flagged("-f")) {
            System.out.println("Using fix Trimble flag (-1 second from all data after 2005-08-03 23:49:59 UTC.)");
            is.fixTrimble = true;
        }

        process(files, is);
    }
}
