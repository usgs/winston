package gov.usgs.winston.in;

import gov.usgs.earthworm.message.TraceBuf;
import gov.usgs.plot.data.Wave;
import gov.usgs.util.Arguments;
import gov.usgs.util.CodeTimer;
import gov.usgs.util.ConfigFile;
import gov.usgs.util.Util;
import gov.usgs.winston.db.Channels;
import gov.usgs.winston.db.Data;
import gov.usgs.winston.db.InputEW;
import gov.usgs.winston.db.WinstonDatabase;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.ListIterator;
import java.util.Map;
import java.util.Set;


/**
 * A base class for writing a static data importer for Winston. Clients just
 * have to fill the instructions <code>StringBuffer</code>, implement
 * <code>readFile()</code> and call <code>process()</code>.
 * 
 * <code>readFile()</code> is responsible for creating a map that maps Winston
 * compatible channel codes to lists of TRACEBUF-sized <code>Wave</code>s.
 * 
 * 
 * @author Dan Cervelli
 */
abstract public class StaticImporter {
    protected static StringBuffer instructions = new StringBuffer();

    protected WinstonDatabase winston;
    protected InputEW input;
    protected Channels channels;
    protected Data data;

    protected static String driver;
    protected static String url;
    protected static String db;

    protected boolean rsamEnable = true;
    protected int rsamDelta = 10;
    protected int rsamDuration = 60;

    public StaticImporter() {
    }

    public void setupWinston() {
        ConfigFile cf = new ConfigFile("Winston.config");
        String driver = cf.getString("winston.driver");
        String url = cf.getString("winston.url");
        String db = cf.getString("winston.prefix");
        winston = new WinstonDatabase(driver, url, db);
        if (!winston.checkDatabase()) {
            System.err.println("Winston database does not exist and could not be created.");
            System.exit(-1);
        }
        input = new InputEW(winston);
        channels = new Channels(winston);
        data = new Data(winston);
    }

    public void importMap(Map<String, List<Wave>> map) {
        if (map == null) {
            System.out.println("Nothing to import.");
            return;
        }

        CodeTimer timer = new CodeTimer("import");
        double minTime = 1E300;
        double maxTime = -1E300;
        for (String code : map.keySet()) {
            if (!channels.channelExists(code)) {
                System.out.println("Creating new channel '" + code + "' in Winston.");
                channels.createChannel(code);
            }

            System.out.printf("Importing channel: %s.\n", code);
            List<Wave> waves = map.get(code);
            System.out.printf("Converting %d waves into TraceBufs.\n", waves.size());
            List<TraceBuf> tbs = new ArrayList<TraceBuf>(waves.size());

            int maxSamples = (int) (Math.pow(2, 16) / 4);
            Iterator<Wave> it = waves.iterator();
            List<Wave> subWaves= new ArrayList<Wave>();
            while(it.hasNext()) {
                Wave wave = it.next();
                if (wave.numSamples() > maxSamples) {
                    subWaves.addAll(wave.split(maxSamples));
                    it.remove();
                }
            }
            waves.addAll(subWaves);
            
            for (Wave wave : waves) {
                TraceBuf tb = new TraceBuf(code, wave);
                minTime = Math.min(minTime, tb.firstSampleTime());
                maxTime = Math.max(maxTime, tb.lastSampleTime());
                tb.createBytes();
                tbs.add(tb);
                wave = null; // for garbage collection
            }
            waves = null;

            int duration = (int) (maxTime - minTime);
            input.setRowParameters(duration + 5, Math.min(duration, 100));
            System.out.println("Writing TraceBufs to database.");
            input.inputTraceBufs(tbs, rsamEnable, rsamDelta, rsamDuration);

            System.out.println("Done.");
        }
        timer.stop();
        System.out.printf("Completed in %.2fs\n", timer.getRunTimeMillis() / 1000);
    }

    public static void process(List<String> files, StaticImporter impt) {
        if (files.size() == 0) {
            System.out.println("No files to import.");
            System.exit(1);
        }
        impt.setupWinston();

        ListIterator<String> it = files.listIterator();
        while(it.hasNext()) {
            String fn = it.next();
            System.out.println("Reading file: " + fn);

            // assume files really means files, not resource records --tjp
            File f = new File(fn);
            if (f.isDirectory()) {
                for (File ff : f.listFiles())
                    it.add(fn + File.pathSeparatorChar + ff.getName());
            } else {
                try {
                    impt.importMap(impt.readFile(fn));
                } catch (IOException e) {
                    System.out.println("Can't read " + fn + " (" + e.getLocalizedMessage() + ")");
                }
            }
        }

    }

    protected Set<String> getArgumentSet() {
        Set<String> kvs = new HashSet<String>();
        kvs.add("-rd");
        kvs.add("-rl");
        return kvs;
    }

    public void processArguments(Arguments args) {
        String rd = args.get("-rd");
        rsamDelta = Util.stringToInt(rd, 10);
        String rl = args.get("-rl");
        rsamDuration = Util.stringToInt(rl, 60);
        System.out.printf("RSAM parameters: delta=%d, duration=%d.\n", rsamDelta, rsamDuration);
    }

    public void setRsamDelta(int i) {
        rsamDelta = i;
    }

    public void setRsamDuration(int i) {
        rsamDuration = i;
    }

    abstract public Map<String, List<Wave>> readFile(String fn) throws IOException;
}