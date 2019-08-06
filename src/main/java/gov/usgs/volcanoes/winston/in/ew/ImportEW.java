package gov.usgs.volcanoes.winston.in.ew;

import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.InputStreamReader;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
import java.util.TimeZone;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import org.apache.log4j.Level;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.martiansoftware.jsap.FlaggedOption;
import com.martiansoftware.jsap.JSAP;
import com.martiansoftware.jsap.JSAPException;
import com.martiansoftware.jsap.JSAPResult;
import com.martiansoftware.jsap.Parameter;
import com.martiansoftware.jsap.SimpleJSAP;
import com.martiansoftware.jsap.Switch;
import com.martiansoftware.jsap.UnflaggedOption;

import gov.usgs.volcanoes.core.CodeTimer;
import gov.usgs.volcanoes.core.Log;
import gov.usgs.volcanoes.core.configfile.ConfigFile;
import gov.usgs.volcanoes.core.legacy.ew.ImportGeneric;
import gov.usgs.volcanoes.core.legacy.ew.MessageListener;
import gov.usgs.volcanoes.core.legacy.ew.message.Message;
import gov.usgs.volcanoes.core.legacy.ew.message.MessageType;
import gov.usgs.volcanoes.core.legacy.ew.message.TraceBuf;
import gov.usgs.volcanoes.core.time.CurrentTime;
import gov.usgs.volcanoes.core.time.J2kSec;
import gov.usgs.volcanoes.core.time.Time;
import gov.usgs.volcanoes.core.util.StringUtils;
import gov.usgs.volcanoes.winston.Version;
import gov.usgs.volcanoes.winston.db.Admin;
import gov.usgs.volcanoes.winston.db.Channels;
import gov.usgs.volcanoes.winston.db.InputEW;
import gov.usgs.volcanoes.winston.db.WinstonDatabase;

/**
 * The new ImportEW.
 *
 * @author Dan Cervelli
 * @author Richard Guest
 * @author Tom Parker
 */
public class ImportEW extends Thread {

  private static final Logger LOGGER = LoggerFactory.getLogger(ImportEW.class);

  public static final String DEFAULT_CONFIG_FILENAME = "ImportEW.config";
  public static final String DEFAULT_HOST = "localhost";
  public static final int DEFAULT_PORT = 16022;
  // public static final String DEFAULT_EXPORT_TYPE = "old";
  public static final int DEFAULT_TIMEOUT = 2000;
  public static final int DEFAULT_HEARTBEAT_INTERVAL = 30000;
  public static final int DEFAULT_EXPECTED_HEARTBEAT_INTERVAL = 30000;
  public static final int DEFAULT_STATUS_INTERVAL = 60;
  public static final String DEFAULT_RECEIVE_ID = "MSG_FROM_EXPORT";
  public static final String DEFAULT_SEND_ID = "MSG_TO_EXPORT";
  public static final String DEFAULT_DRIVER = "com.mysql.jdbc.Driver";
  public static final String DEFAULT_TABLE_ENGINE = "MyISAM";
  public static final int DEFAULT_MAX_DAYS = 0;
  public static final int DEFAULT_MAX_BACKLOG = 100;
  public static final String DEFAULT_LOG_LEVEL = "FINE";
  public static final boolean DEFAULT_ENABLE_VALARM_VIEW = false;

  public static final double DEFAULT_TIME_THRESHOLD = 1.0;
  public static final int DEFAULT_BACKLOG_THRESHOLD = 1;
  public static final boolean DEFAULT_RSAM_ENABLE = true;
  public static final int DEFAULT_RSAM_DELTA = 10;
  public static final int DEFAULT_RSAM_DURATION = 60;

  public static final int DEFAULT_DROP_TABLE_DELAY = 10;

  public static final int DEFAULT_REPAIR_RETRY_INTERVAL = 10 * 60;

  // JSAP related stuff.
  public static String JSAP_PROGRAM_NAME = "java gov.usgs.volcanoes.winston.in.ew.ImportEW";
  public static String JSAP_EXPLANATION_PREFACE = "Winston ImportEW\n" + "\n"
      + "This program gets data from an Earthworm export process and imports\n"
      + "it into a Winston database. See 'ImportEW.config' for more options.\n" + "\n";

  private static final String DEFAULT_JSAP_EXPLANATION =
      "All output goes to both standard error and the file log.\n" + "\n"
          + "While the process is running (and accepting console input) you can enter\n"
          + "these commands into the console (followed by [Enter]):\n" + "0: turn logging off.\n"
          + "1: normal logging level (WARNING).\n" + "2: high logging level (FINE).\n"
          + "3: log everything.\n" + "s: display status information.\n"
          + "c[col][-]: channel list, sorted by col, - for descending. Examples: c, cl-, cx\n"
          + "i: no longer accept console input.\n" + "q: quit cleanly.\n" + "ctrl-c: quit now.\n"
          + "\n" + "Note that if console input is disabled the only way to\n"
          + "terminate the program is with ctrl-c or by killing the process.\n";

  private static final Parameter[] DEFAULT_JSAP_PARAMETERS = new Parameter[] {
      new FlaggedOption("logLevel", JSAP.STRING_PARSER, JSAP.NO_DEFAULT, JSAP.NOT_REQUIRED, 'l',
          "log-level",
          "The level of logging to start with\n"
              + "This may consist of either a java.util.logging.Level name or an integer value.\n"
              + "For example: \"SEVERE\", or \"1000\""),
      new Switch("logoff", '0', "logoff", "Turn logging off (equivalent to --log-level OFF)."),
      new Switch("lognormal", '1', "lognormal",
          "Normal (default) logging level (equivalent to --log-level WARNING)."),
      new Switch("loghigh", '2', "loghigh", "High logging level (equivalent to --log-level FINE)."),
      new Switch("logall", '3', "logall", "High logging level (equivalent to --log-level ALL)."),
      new Switch("noinput", 'i', "noinput", "Don't accept input from the console."),
      new FlaggedOption("log-dir", JSAP.STRING_PARSER, ".",
          JSAP.NOT_REQUIRED, JSAP.NO_SHORTFLAG, "log-dir", "where to place log files"),
      new UnflaggedOption("configFilename", JSAP.STRING_PARSER, JSAP.NO_DEFAULT, JSAP.NOT_REQUIRED,
          JSAP.NOT_GREEDY, "The config file name.")
  };

  protected String configFilename;
  protected ConfigFile config;

  private WinstonDatabase winston;
  private Channels channels;
  private InputEW input;

  protected ImportGeneric importGeneric;
  private final Set<String> existingChannels;
  private final Map<String, ConcurrentLinkedQueue<TraceBuf>> channelTraceBufs;

  protected String logFile;
  protected int logNumFiles;
  protected int logSize;

  private final ExecutorService fixer;
  private WinstonDatabase fixerWinston;
  private Admin fixerAdmin;
  private InputEW fixerInput;
  private final Set<String> underRepair;
  private final Map<String, Double> attemptedRepair;
  private int repairRetryInterval;

  protected final CodeTimer inputTimer;
  protected int totalTraceBufsWritten = 0;
  protected int totalTraceBufs;
  protected int totalTraceBufsDropped;
  protected int totalTraceBufsAccepted;
  protected int totalTraceBufsRejected;
  protected int totalTraceBufsFailed;
  protected final Map<String, ChannelStatus> channelStatus;
  protected final Date importStartTime;
  protected final DateFormat dateFormat;
  protected final DateFormat winstonDateFormat;

  protected int dropTableDelay = 10000;
  protected boolean enableValarmView;

  protected Options defaultOptions;
  protected final Map<String, Options> channelOptions;
  protected Map<String, Map<String, String>> channelMetadata;

  private volatile boolean quit = false;

  protected List<TraceBufFilter> traceBufFilters;
  protected List<OptionsFilter> optionFilters;

  public ImportEW(final String fn) {
    this();

    configFilename = StringUtils.stringToString(fn, DEFAULT_CONFIG_FILENAME);

    importGeneric = new ImportGeneric() {
      @Override
      public void outOfMemoryErrorOccurred(final OutOfMemoryError e) {
        handleOutOfMemoryError(e);
      }
    };

    config = new ConfigFile(configFilename);

    channelMetadata = new HashMap<String, Map<String, String>>();

    processConfigFile();
  }

  /**
   * Sets up all the standard private member variables, the logger and
   * instantiates a ConfigFile using configFilename.
   */
  public ImportEW() {
    setName("ImportEW");

    importStartTime = new Date(CurrentTime.getInstance().now());
    dateFormat = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
    dateFormat.setTimeZone(TimeZone.getTimeZone("GMT"));
    winstonDateFormat = new SimpleDateFormat("yyyy_MM_dd");
    winstonDateFormat.setTimeZone(TimeZone.getTimeZone("GMT"));
    channelOptions = new HashMap<String, Options>();
    channelStatus = new HashMap<String, ChannelStatus>();

    inputTimer = new CodeTimer("inputTimer", false);
    channelTraceBufs =
        new ConcurrentHashMap<String, ConcurrentLinkedQueue<TraceBuf>>(200, 0.75f, 1);
    fixer = Executors.newSingleThreadExecutor();
    underRepair = Collections.synchronizedSet(new HashSet<String>());
    attemptedRepair = Collections.synchronizedMap(new HashMap<String, Double>());
    existingChannels = Collections.synchronizedSet(new HashSet<String>());
  }

  /**
   * Try to recover from an {@link OutOfMemoryError} by dropping all
   * {@link TraceBuf}s from the processing queue. Ideally,
   * {@link OutOfMemoryError}s should be prevented by aggressive tuning; this
   * is a last-gasp effort to allow the program to continue running.
   */
  public void handleOutOfMemoryError(final OutOfMemoryError e) {
    channelTraceBufs.clear();
    LOGGER.warn("Handled OutOfMemoryError, TraceBuf queues cleared.");
    e.printStackTrace();
  }

  /**
   * Logs a severe message and exits uncleanly.
   */
  protected void fatalError(final String msg) {
    throw new RuntimeException(msg);
  }

  /**
   * Processes logger, import and winston config, plus options, default
   * options and filters.
   */
  protected void processConfigFile() {
    processImportConfig();
    processWinstonConfig();
    processDefaultOptions();
    processOptions();
    processFilters();
  }


  /**
   * Extracts generalised configuration information.
   */
  protected void processImportConfig() {
    final String host = StringUtils.stringToString(config.getString("import.host"), DEFAULT_HOST);
    final int port = StringUtils.stringToInt(config.getString("import.port"), DEFAULT_PORT);
    // String exportType =
    // Util.stringToString(config.getString("import.exportType"),
    // DEFAULT_EXPORT_TYPE);
    LOGGER.info("config: import.host=" + host);
    LOGGER.info("config: import.port=" + port);
    // logger.info("config: import.exportType=" + exportType);

    // importGeneric.setHostAndPortAndType(host, port, exportType);
    importGeneric.setHostAndPort(host, port);

    final String recvID =
        StringUtils.stringToString(config.getString("import.receiveID"), DEFAULT_RECEIVE_ID);
    importGeneric.setRecvIDString(recvID);
    LOGGER.info("config: import.receiveID=" + recvID);
    final String sendID =
        StringUtils.stringToString(config.getString("import.sendID"), DEFAULT_SEND_ID);
    importGeneric.setSendIDString(sendID);
    LOGGER.info("config: import.sendID=" + sendID);

    final int hbInt = StringUtils.stringToInt(config.getString("import.heartbeatInterval"),
        DEFAULT_HEARTBEAT_INTERVAL);
    importGeneric.setHeartbeatInterval(hbInt);
    LOGGER.info("config: import.heartbeatInterval=" + hbInt);

    final int ehbInt = StringUtils.stringToInt(config.getString("import.expectedHeartbeatInterval"),
        DEFAULT_EXPECTED_HEARTBEAT_INTERVAL);
    importGeneric.setExpectedHeartbeatInterval(ehbInt);
    LOGGER.info("config: import.expectedHeartbeatInterval=" + ehbInt);

    final int to = StringUtils.stringToInt(config.getString("import.timeout"), DEFAULT_TIMEOUT);
    importGeneric.setTimeout(to);
    LOGGER.info("config: import.timeout=" + to);

    dropTableDelay = StringUtils.stringToInt(config.getString("import.dropTableDelay"),
        DEFAULT_DROP_TABLE_DELAY);
    dropTableDelay *= 1000;
    LOGGER.info("config: import.dropTableDelay=" + dropTableDelay);

    enableValarmView = StringUtils.stringToBoolean(config.getString("import.enableValarmView"),
        DEFAULT_ENABLE_VALARM_VIEW);

  }

  /**
   * Extracts configuration information specific to Winston DB and
   * instantiates a Winston DB connection.
   */
  protected void processWinstonConfig() {
    final String winstonDriver =
        StringUtils.stringToString(config.getString("winston.driver"), DEFAULT_DRIVER);
    LOGGER.info("config: winston.driver=" + winstonDriver);

    final String winstonPrefix = config.getString("winston.prefix");
    if (winstonPrefix == null)
      fatalError("winston.prefix is missing from config file.");
    LOGGER.info("config: winston.prefix=" + winstonPrefix);

    final String winstonURL = config.getString("winston.url");
    if (winstonURL == null)
      fatalError("winston.url is missing from config file.");
    LOGGER.info("config: winston.url=" + winstonURL);

    final String winstonTableEngine = config.getString("winston.tableEngine");
    if (winstonTableEngine != null)
      LOGGER.info("config: winston.tableEngine=" + winstonTableEngine);

    final int winstonStatementCacheCap =
        StringUtils.stringToInt(config.getString("winston.statementCacheCap"), 100);
    LOGGER.info("config: winston.statementCacheCap=" + winstonStatementCacheCap);

    winston = new WinstonDatabase(winstonDriver, winstonURL, winstonPrefix, winstonTableEngine,
        winstonStatementCacheCap);
    if (!winston.checkDatabase())
      fatalError("Winston database does not exist.");
    fixerWinston = new WinstonDatabase(winstonDriver, winstonURL, winstonPrefix, winstonTableEngine,
        winstonStatementCacheCap);
    fixerInput = new InputEW(fixerWinston);
    fixerInput.setEnableValarmView(enableValarmView);
    fixerAdmin = new Admin(fixerWinston);

    channels = new Channels(winston);
    input = new InputEW(winston);
    input.setEnableValarmView(enableValarmView);

    repairRetryInterval = DEFAULT_REPAIR_RETRY_INTERVAL;
  }

  protected void processDefaultOptions() {
    defaultOptions = new Options();
    defaultOptions.bufThreshold = DEFAULT_BACKLOG_THRESHOLD;
    defaultOptions.timeThreshold = DEFAULT_TIME_THRESHOLD;
    defaultOptions.maxBacklog = DEFAULT_MAX_BACKLOG;
    defaultOptions.maxDays = DEFAULT_MAX_DAYS;
    defaultOptions.rsamEnable = DEFAULT_RSAM_ENABLE;
    defaultOptions.rsamDelta = DEFAULT_RSAM_DELTA;
    defaultOptions.rsamDuration = DEFAULT_RSAM_DURATION;

    final ConfigFile dcf = config.getSubConfig("Default");
    defaultOptions = Options.createOptions(dcf, defaultOptions);
    LOGGER.info("config, options, Default: " + defaultOptions);
  }

  protected void processOptions() {
    optionFilters = new ArrayList<OptionsFilter>();
    final List<String> optionSets = config.getList("options");
    for (final String options : optionSets) {
      if (options.equals("Default"))
        continue;
      final ConfigFile tc = config.getSubConfig(options);
      try {
        final OptionsFilter filter = new OptionsFilter(options, tc, defaultOptions);
        optionFilters.add(filter);
      } catch (final Exception ex) {
        LOGGER.error("Could not create options: {}", ex);
      }
    }
    for (final OptionsFilter filter : optionFilters)
      LOGGER.info("config, options, {}: {}", filter.getName(), filter);
  }

  protected void processFilters() {
    traceBufFilters = new ArrayList<TraceBufFilter>();
    final List<String> filters = config.getList("filter");
    for (final String filterName : filters) {
      final ConfigFile fc = config.getSubConfig(filterName);
      try {
        String filterClass = fc.getString("class");
        if (filterClass.startsWith("gov.usgs.winston")) {
          String newFilterClass =
              filterClass.replace("gov.usgs.winston", "gov.usgs.volcanoes.winston");
          LOGGER.error(
              "Defunct filter class found. I'll fix it this time, but this will cease to work in future versions.");
          LOGGER.error("Update ImportEW config to reference {} in place of {}", newFilterClass,
              filterClass);
          filterClass = newFilterClass;
        }
        final TraceBufFilter filter =
            (TraceBufFilter) Class.forName(filterClass).newInstance();
        filter.configure(fc);
        traceBufFilters.add(filter);
      } catch (final IllegalAccessException ex) {
        LOGGER.error("Could not create TraceBuf filter: {}", ex);
      } catch (InstantiationException ex) {
        LOGGER.error("Could not create TraceBuf filter: {}", ex);
      } catch (ClassNotFoundException e) {
        // TODO Auto-generated catch block
        e.printStackTrace();
      }
    }
    Collections.sort(traceBufFilters);
    for (final TraceBufFilter filter : traceBufFilters)
      LOGGER.info("config, filter: {}", filter);
  }

  /**
   * Tests the TraceBuf against each filter, stopping at first match.
   * 
   * @param tb
   *          the TraceBuf to test
   * @return filter that matched, null if no match
   */
  protected TraceBufFilter matchFilter(final TraceBuf tb) {
    final Options options = getOptions(tb);
    for (final TraceBufFilter filter : traceBufFilters) {
      if (filter.match(tb, options)) {

        final Map<String, String> m = filter.getMetadata();
        if (m != null)
          addMetadata(tb.toWinstonString(), m);

        if (filter.isTerminal())
          return filter;
      }

    }
    return null;
  }

  private void addMetadata(final String c, final Map<String, String> m) {
    Map<String, String> cm = channelMetadata.get(c);
    if (cm == null) {
      cm = new HashMap<String, String>();
      channelMetadata.put(c, cm);
    }

    cm.putAll(m);
  }

  /**
   * This class is called by the {@link ImportGeneric} thread whenever a
   * tracebuf has been received.
   * 
   * $Log: not supported by cvs2svn $ Revision 1.11 2007/05/03 23:25:54
   * dcervelli OOM diagnosis.
   * 
   * @author Dan Cervelli
   */
  class TraceBufHandler implements MessageListener {
    public void messageReceived(final Message msg) {
      totalTraceBufs++;
      if (!(msg instanceof TraceBuf)) {
        throw new RuntimeException("Expected TraceBuf but received " + msg.getClass());
      }
      final TraceBuf tb = (TraceBuf) msg;
      LOGGER.debug("RX: {}", tb.toString());

      final TraceBufFilter matchingFilter = matchFilter(tb);
      boolean accept = false;
      if (matchingFilter != null) {
        accept = matchingFilter.isAccept();

        // Don't consume cycles on String.format if the message is not
        // going
        // to be logged. Yes, it does matter. Running String.format for
        // every
        // single tracebuf adds up.
        LOGGER.debug("{}: {}", matchingFilter, tb);
      } else
        LOGGER.debug("No matching filter, rejected: " + tb);

      if (accept) {
        totalTraceBufsAccepted++;
        addTraceBufToQueue(tb);
      } else {
        totalTraceBufsRejected++;

        // ack tracebufs that were rejected, so they will not be resent.
        if (msg.sendAck)
          importGeneric.sendAck(tb.seq);

        if (matchingFilter.keepRejects()) {
          // TODO: deal with the rejected keepers
        }
      }
    }
  }

  protected void addTraceBufToQueue(final TraceBuf tb) {
    final String channel = tb.toWinstonString();
    ConcurrentLinkedQueue<TraceBuf> q = channelTraceBufs.get(channel);
    if (q == null) {
      q = new ConcurrentLinkedQueue<TraceBuf>();
      channelTraceBufs.put(channel, q);
    }

    final Options ip = getOptions(tb);

    q.add(tb);
    while (q.size() > ip.maxBacklog) {
      q.poll();
      // TODO: improve logging of dropped tracebufs
      totalTraceBufsDropped++;
      if (totalTraceBufsDropped % 100 == 1) {
        LOGGER.info("Overfull backlog, dropped TraceBuf");
      }
    }
  }

  private void cycle(final boolean force) {
    // CodeTimer ct0 = new CodeTimer("init");
    for (final Iterator<Entry<String, ConcurrentLinkedQueue<TraceBuf>>> iter =
        channelTraceBufs.entrySet().iterator(); iter.hasNext();) {
      Entry<String, ConcurrentLinkedQueue<TraceBuf>> entry = iter.next();
      String key = entry.getKey();
      ConcurrentLinkedQueue<TraceBuf> q = entry.getValue();
      if (q.isEmpty())
        continue;

      final Options ip = getOptions(q.peek());

      if (force || ip.thresholdExceeded(q.peek().getStartTimeJ2K(), q.size())) {

        importChannel(q);
        if (channelMetadata.containsKey(key))
          importMetadata(key, channelMetadata.get(key));
      }

    }
    // ct0.stop();
    // if (ct0.getRunTimeMillis() > 1000)
    // System.out.println("Long cycle: " + ct0);
  }

  private Runnable getPurgeRunnable(final String code, final int maxDays) {
    return new Runnable() {
      public void run() {
        try {
          fixerInput.purgeTables(code, maxDays);
          try {
            Thread.sleep(dropTableDelay);
          } catch (final InterruptedException e) {
            Thread.currentThread().interrupt();
          }
        } catch (final OutOfMemoryError e) {
          handleOutOfMemoryError(e);
        }
      }
    };
  }

  private Runnable getRepairRunnable(final String database, final String table) {
    if (underRepair.contains(table))
      return null;

    final Double lastRepair = attemptedRepair.get(table);
    if (lastRepair != null) {
      if (J2kSec.now() - lastRepair.doubleValue() > repairRetryInterval)
        attemptedRepair.remove(table);
      else
        return null;
    }

    underRepair.add(table);
    return new Runnable() {
      public void run() {
        try {
          final boolean healthy = fixerAdmin.repairTable(database, table);
          LOGGER.info("After repair attempt, table {} appears to {}.", table,
              (healthy ? "be healthy" : "still be broken"));
          attemptedRepair.put(table, J2kSec.now());
          underRepair.remove(table);
        } catch (final OutOfMemoryError e) {
          handleOutOfMemoryError(e);
        }
      }
    };
  }

  private void importChannel(final ConcurrentLinkedQueue<TraceBuf> q) {
    if (q.isEmpty() || underRepair.contains("channels")) {
      System.out.println("isempty: " + q.isEmpty());
      System.out.println("underRepair: " + underRepair.contains("channels"));
      return;
    }
    final TraceBuf tb = q.peek();
    final String code = tb.toWinstonString();

    if (!existingChannels.contains(code) && !channels.channelExists(code)) {
      LOGGER.info("Creating new channel '" + code + "' in Winston database.");
      channels.createChannel(code);
    }
    existingChannels.add(code);

    final ArrayList<TraceBuf> tbs = new ArrayList<TraceBuf>(q.size());
    while (!q.isEmpty()) {
      final TraceBuf t = q.poll();
      tbs.add(t);
      if (t.sendAck)
        importGeneric.sendAck(t.seq);

    }

    inputTimer.start();
    // TODO: catch exceptions around here
    final Options ip = getOptions(tbs.get(0));
    final List<InputEW.InputResult> results =
        input.inputTraceBufs(tbs, ip.rsamEnable, ip.rsamDelta, ip.rsamDuration);
    inputTimer.stop();

    ChannelStatus status = channelStatus.get(code);
    if (status == null) {
      status = new ChannelStatus(code);
      channelStatus.put(code, status);
    }
    // TODO: suppress repetitive MySQL exceptions
    if (results.size() == 1) {
      final InputEW.InputResult result = results.get(0);
      switch (result.code) {
        case ERROR_DATABASE:
        case ERROR_NO_WINSTON:
          // shouldn't happen because several database accesses must have
          // already occurred by this point
          break;
        case ERROR_INPUT:
          // should never happen, conditions checked before call
          break;
        case ERROR_TIME_SPAN:
          LOGGER.warn("Time span error.");
          final Runnable repairTask = getRepairRunnable("ROOT", "channels");
          if (repairTask != null)
            fixer.submit(repairTask);
          break;
        default:
          LOGGER.warn("Error: " + result.code);
      }
    } else {
      for (int i = 0; i < results.size() - 2; i++) {
        final InputEW.InputResult result = results.get(i);
        status.process(result.traceBuf, result.code);
        boolean repair = false;
        switch (result.code) {
          case SUCCESS_CREATED_TABLE:
            LOGGER.info("Day table created: " + tb.toWinstonString() + " "
                + winstonDateFormat.format(J2kSec.asDate(tb.getStartTimeJ2K())));
            fixer.submit(getPurgeRunnable(code, ip.maxDays));
            attemptedRepair.remove(code);
            totalTraceBufsWritten++;
            LOGGER.debug("Insert: " + tb.toString());
            break;
          case SUCCESS:
            attemptedRepair.remove(code);
            totalTraceBufsWritten++;
            LOGGER.debug("Insert: " + tb.toString());
            break;
          case ERROR_DATABASE:
            totalTraceBufsFailed++;
            repair = true;
            LOGGER.warn("Database error: " + tb.toString());
            break;
          case ERROR_UNKNOWN:
            totalTraceBufsFailed++;
            repair = true;
            LOGGER.warn("Unknown insert error: " + tb.toString());
            break;
          case ERROR_CHANNEL:
          case ERROR_NULL_TRACEBUF:
            totalTraceBufsFailed++;
            // these errors should never occur
            LOGGER.warn("Bad channel/null TraceBuf.");
            break;
          case ERROR_DUPLICATE:
            totalTraceBufsFailed++;
            LOGGER.warn("Duplicate TraceBuf: " + tb.toString());
            break;
          case NO_CODE:
            // this should never occur
            totalTraceBufsFailed++;
            LOGGER.warn("No error/success code: " + tb.toString());
            break;
          case ERROR_HELICORDER:
            LOGGER.warn("Cannot write heli: " + tb.toString());
            break;
          case ERROR_INPUT:
            LOGGER.warn("Error writing tb: " + tb.toString());
            break;
          case ERROR_NO_WINSTON:
            LOGGER.warn("Cannot find winston: " + tb.toString());
            break;
          case ERROR_TIME_SPAN:
            LOGGER.warn("Timespan error: " + tb.toString());
            break;
          case SUCCESS_HELICORDER:
            break;
          case SUCCESS_TIME_SPAN:
            break;
          default:
            break;
        }
        if (repair) {
          final String dt = winstonDateFormat.format(J2kSec.asDate(tb.getStartTimeJ2K()));
          final Runnable repairTask = getRepairRunnable(code, code + "$$" + dt);
          if (repairTask != null)
            fixer.submit(repairTask);
        }
      }

      final InputEW.InputResult timeSpanResult = results.get(results.size() - 1);
      if (timeSpanResult.code == InputEW.InputResult.Code.ERROR_TIME_SPAN) {
        LOGGER.warn("Time span error.");
        final Runnable repairTask = getRepairRunnable("ROOT", "channels");
        if (repairTask != null)
          fixer.submit(repairTask);
      }
      final InputEW.InputResult heliResult = results.get(results.size() - 2);
      if (heliResult.code == InputEW.InputResult.Code.ERROR_HELICORDER) {
        final String dt = winstonDateFormat.format(J2kSec.asDate(heliResult.failedHeliJ2K));
        final String table = code + "$$H" + dt;
        LOGGER.warn("Error writing helicorder data to table " + table + ".");
        final Runnable repairTask = getRepairRunnable(code, table);
        if (repairTask != null)
          fixer.submit(repairTask);
      }
    }
  }

  private void importMetadata(final String channel, final Map<String, String> m) {

    if (underRepair.contains("channelmetadata")) {
      System.out.println("underRepair: " + underRepair.contains("channelmetadata"));
    } else if (!m.isEmpty()) {
      inputTimer.start();
      System.out.println("importing metadata " + channel);
      input.inputMetadata(channel, m);
      inputTimer.stop();
    }
  }

  @Override
  public void run() {
    // Register our interest in tracebuf messages.
    final TraceBufHandler tbh = new TraceBufHandler();
    importGeneric.addListener(MessageType.TYPE_TRACEBUF, tbh);
    importGeneric.addListener(MessageType.TYPE_TRACEBUF2, tbh);

    boolean connected = false;
    while (!connected) {
      connected = importGeneric.connect();
      if (!connected) {
        try {
          Thread.sleep(100);
        } catch (final InterruptedException e) {
          Thread.currentThread().interrupt();
        }
      }
    }
    while (!quit) {
      try {
        cycle(true);
        Thread.sleep(10); // avoid busy-waiting when importing few
                          // channels
      } catch (final OutOfMemoryError e) {
        handleOutOfMemoryError(e);
      } catch (final Throwable e) {
        LOGGER.error("Main loop exception: {}", e.getLocalizedMessage());
        e.printStackTrace();
      }
    }
    try {
      cycle(true);
    } catch (final Throwable e) {
      LOGGER.error("Exception during final cycle: {}", e);
    }
  }

  protected Options getOptions(final TraceBuf tb) {
    final String channel = tb.toWinstonString();
    Options ip = channelOptions.get(channel);
    if (ip == null) {
      ip = defaultOptions;
      for (final OptionsFilter tf : optionFilters) {
        if (tf.match(tb)) {
          ip = tf.getOptions();
          break;
        }
      }
      channelOptions.put(channel, ip);
    }
    return ip;
  }

  public void quit() {
    importGeneric.shutdown();
    try {
      importGeneric.join();
    } catch (final Throwable e) {
      LOGGER.error("Failed to cleanly shutdown SeedLink Collector {}", e);
    }

    LOGGER.info("Quitting cleanly.");
    quit = true;
  }

  public void printStatus() {
    final Date now = new Date(CurrentTime.getInstance().now());
    final long nowST = System.currentTimeMillis();
    final double uptime = (now.getTime() - importStartTime.getTime()) / 1000.0;
    final List<String> strings = new ArrayList<String>(4);

    strings.add("------- ImportEW --------");
    strings.add("Status time: " + dateFormat.format(now));
    strings.add("Start time:  " + dateFormat.format(importStartTime));
    strings.add("Up time:     " + Time.secondsToString(uptime));

    long ht = importGeneric.getLastHeartbeatTime();
    double dt = (double) (nowST - ht) / 1000;
    if (ht == 0)
      strings.add("Last HB RX:  (never)");
    else
      strings.add("Last HB RX:  " + dateFormat.format(new Date(ht)) + ", "
          + Time.secondsToString(dt));
    ht = importGeneric.getLastHeartbeatSentTime();
    dt = (double) (nowST - ht) / 1000;
    if (ht == 0)
      strings.add("Last HB TX:  (never)");
    else
      strings.add("Last HB TX:  " + dateFormat.format(new Date(ht)) + ", "
          + Time.secondsToString(dt));

    final Runtime rt = Runtime.getRuntime();
    strings.add(String.format("Memory (used / max): %.1fMB / %.1fMB",
        (rt.totalMemory() - rt.freeMemory()) / 1024.0 / 1024.0, rt.maxMemory() / 1024.0 / 1024.0));

    strings.add("---- TraceBufs");
    strings.add("Accepted: " + totalTraceBufsAccepted);
    strings.add("Written:  " + totalTraceBufsWritten);
    strings.add("Failed:   " + totalTraceBufsFailed);
    strings.add("Rejected: " + totalTraceBufsRejected);
    strings.add("Dropped:  " + totalTraceBufsDropped);
    final int pending = totalTraceBufsAccepted - totalTraceBufsWritten - totalTraceBufsFailed
        - totalTraceBufsRejected - totalTraceBufsDropped;
    strings.add("Pending:  " + pending);

    // by each filter
    strings.add("---- Timing");
    strings.add(String.format("Total input time:        %s",
        Time.secondsToString(inputTimer.getTotalTimeMillis() / 1000)));
    strings.add(String.format("Input time per TraceBuf: %.2fms",
        inputTimer.getTotalTimeMillis() / totalTraceBufsWritten));

    for (final String s : strings)
      System.out.println(s);
  }

  public void printChannels(final String s) {
    char col = 'C';
    if (s.length() > 1)
      col = s.charAt(1);
    final boolean desc = s.endsWith("-");

    System.out.println("------- Channels --------");
    System.out.println(ChannelStatus.getHeaderString());
    final List<ChannelStatus> channels = new ArrayList<ChannelStatus>(channelStatus.size());
    channels.addAll(channelStatus.values());

    Collections.sort(channels,
        ChannelStatus.getComparator(ChannelStatus.SortOrder.parse(col), desc));

    for (final ChannelStatus channel : channels) {
      System.out.println(channel.toString());
    }
    System.out.println(ChannelStatus.getHeaderString());
  }

  /**
   * Find and parse the command line arguments.
   * 
   * @param args
   *          The command line arguments.
   * @throws JSAPException 
   */
  public static JSAPResult getArguments(final String[] args) throws JSAPException {
    JSAPResult config = null;
    final SimpleJSAP jsap = new SimpleJSAP(JSAP_PROGRAM_NAME,
        JSAP_EXPLANATION_PREFACE + DEFAULT_JSAP_EXPLANATION, DEFAULT_JSAP_PARAMETERS);

    config = jsap.parse(args);

    if (jsap.messagePrinted()) {
      // The following error message is useful for catching the case
      // when args are missing, but help isn't printed.
      if (!config.getBoolean("help")) {
        throw new RuntimeException("Try using the --help flag.");
      }
    }
    return config;
  }

  public static void printKeys() {
    final StringBuffer sb = new StringBuffer();
    sb.append("Keys:\n");
    sb.append(" 0-3: logging level. Bigger number, bigger logs.\n");
    sb.append("   i: stop accepting console commands\n");
    sb.append("   s: print status\n");
    sb.append("   c: print channels\n");
    sb.append("   q: quit\n");
    sb.append("   ?: display keys\n");

    System.out.println(sb);
  }

  /**
   * Manage the console input.
   * 
   * @param im
   *          The Winston importer.
   */
  public static void consoleInputManager(final ImportEW im) {
    boolean acceptCommands = true;
    final BufferedReader in = new BufferedReader(new InputStreamReader(System.in));
    LOGGER.info("Enter ? for console commands.");
    while (acceptCommands) {
      try {
        String s = null;
        try {
          s = in.readLine();
        } catch (final IOException ioex) {
          LOGGER.error("IOException encountered while attempting to read console input. {}", ioex);
        }

        if (s != null) {
          s = s.toLowerCase().trim();
          if (s.equals("q")) {
            acceptCommands = false;
            im.quit();
            // exit Importer quickly, don't wait to flush any pending writes
            // try {
            // im.join();
            // } catch (final Throwable e) {
            // LOGGER.error("Failed to quit cleanly. {}");
            // } finally {
            // im.printStatus();
            // }
            System.exit(0);
          } else if (s.equals("s")) {
            im.printStatus();
          } else if (s.startsWith("c")) {
            im.printChannels(s);
          } else if (s.equals("0")) {
            org.apache.log4j.Logger.getRootLogger().setLevel(Level.OFF);
            System.out.println("Logging disabled");
          } else if (s.equals("1")) {
            org.apache.log4j.Logger.getRootLogger().setLevel(Level.ERROR);
            System.out.println("Logging errors only");
          } else if (s.equals("2")) {
            org.apache.log4j.Logger.getRootLogger().setLevel(Level.INFO);
            System.out.println("Logging info and above");
          } else if (s.equals("3")) {
            org.apache.log4j.Logger.getRootLogger().setLevel(Level.ALL);
            System.out.println("Logging everything I can");
          } else if (s.equals("i")) {
            acceptCommands = false;
            LOGGER.error("No longer accepting console commands.");
          } else if (s.equals("?"))
            ImportEW.printKeys();
          else
            ImportEW.printKeys();
        }
      } catch (final OutOfMemoryError e) {
        im.handleOutOfMemoryError(e);
      }
    }
  }

  public static void main(final String[] args) throws IOException, JSAPException {
    final JSAPResult config = getArguments(args);
    Log.addFileAppender(new File(config.getString("log-dir"), "ImportEW.log").toString());

    if (!config.getBoolean("help")) {
      String configFileName = config.getString("configFilename");
      final String fn = StringUtils.stringToString(configFileName, DEFAULT_CONFIG_FILENAME);
      final ImportEW im = new ImportEW(fn);

      im.start();

      if (!(System.console() == null || config.getBoolean("noinput"))) {
        consoleInputManager(im);
      } else {
        LOGGER.info("Not listening for console commands.");
      }
    }
  }
}
