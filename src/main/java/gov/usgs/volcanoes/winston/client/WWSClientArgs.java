/**
 * I waive copyright and related rights in the this work worldwide
 * through the CC0 1.0 Universal public domain dedication.
 * https://creativecommons.org/publicdomain/zero/1.0/legalcode
 */

package gov.usgs.volcanoes.winston.client;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.martiansoftware.jsap.FlaggedOption;
import com.martiansoftware.jsap.JSAP;
import com.martiansoftware.jsap.JSAPResult;
import com.martiansoftware.jsap.Parameter;
import com.martiansoftware.jsap.Switch;

import gov.usgs.volcanoes.core.args.Args;
import gov.usgs.volcanoes.core.args.Arguments;
import gov.usgs.volcanoes.core.args.decorator.ScnlArg;
import gov.usgs.volcanoes.core.args.decorator.TimeSpanArg;
import gov.usgs.volcanoes.core.args.decorator.VerboseArg;
import gov.usgs.volcanoes.core.data.Scnl;
import gov.usgs.volcanoes.core.time.TimeSpan;
import gov.usgs.volcanoes.core.util.StringUtils;

/**
 * Argument processor for WWSClient.
 *
 * @author Tom Parker
 */
public class WWSClientArgs {
	private static final Logger LOGGER = LoggerFactory.getLogger(WWSClientArgs.class);

	/** format of time on cmd line */
	public static final String INPUT_TIME_FORMAT = "yyyyMMddHHmm";

	private static final String PROGRAM_NAME = "java -jar gov.usgs.volcanoes.winston.WWSClient";
	private static final String EXPLANATION = "I am the Winston Wave Server client.\n";

	private static final Parameter[] PARAMETERS = new Parameter[] {
			new FlaggedOption("server", JSAP.STRING_PARSER, JSAP.NO_DEFAULT, JSAP.REQUIRED, 's', "server", "Remote server."),
			new FlaggedOption("port", JSAP.INTEGER_PARSER, "16022", JSAP.NOT_REQUIRED, 'p', "port", "Remote port."),
			new Switch("sac", JSAP.NO_SHORTFLAG, "sac", "Write SAC output."),
			new Switch("txt", JSAP.NO_SHORTFLAG, "txt", "Write samples as text output."),
			new Switch("rsam", JSAP.NO_SHORTFLAG, "rsam", "Write RSAM output."),
			new FlaggedOption("rsamPeriod", JSAP.INTEGER_PARSER, "300", JSAP.NOT_REQUIRED, JSAP.NO_SHORTFLAG, "rsamPeriod", "RSAM period."),
			new Switch("heli", JSAP.NO_SHORTFLAG, "heli", "Write Helicorder output."),
			new Switch("menu", JSAP.NO_SHORTFLAG, "menu", "Retrieve server menu.") };

	/** If true, log more. */
	public final boolean verbose;

	/** Remote server. */
	public final String server;

	/** Remote server port. */
	public final int port;

	/** Time span to retrieve. */
	public final TimeSpan timeSpan;

	/** channel to query */
	public final Scnl channel;

	/** If true, print server menu */
	public final boolean menu;
	
	/** If true, write SAC output. */
	public final boolean sacOutput;

	/** If true, write TXT output. */
	public final boolean txtOutput;

	/** If true, write RSAM output. */
	public final boolean rsamOutput;

	/** RSAM period in seconds*/
	public final int rsamPeriod;
	
	/** If true, write Heli output. */
	public final boolean heliOutput;

	/**
	 * Class constructor.
	 * 
	 * @param commandLineArgs
	 *            the command line arguments
	 * @throws Exception
	 *             when things go wrong
	 */
	public WWSClientArgs(final String[] commandLineArgs) throws Exception {
		Arguments args = null;
		args = new Args(PROGRAM_NAME, EXPLANATION, PARAMETERS);
		args = new ScnlArg(false, args);
		args = new TimeSpanArg(INPUT_TIME_FORMAT, false, args);
		args = new VerboseArg(args);

		JSAPResult jsapResult = null;
		jsapResult = args.parse(commandLineArgs);

		verbose = jsapResult.getBoolean("verbose");
		timeSpan = (TimeSpan) jsapResult.getObject("timeSpan");
		server = jsapResult.getString("server");
		port = jsapResult.getInt("port");
		channel = (Scnl) jsapResult.getObject("channel");
		menu = jsapResult.getBoolean("menu");
		sacOutput = jsapResult.getBoolean("sac");
		txtOutput = jsapResult.getBoolean("txt");
		rsamOutput = jsapResult.getBoolean("rsam");
		rsamPeriod = jsapResult.getInt("rsamPeriod");
		heliOutput = jsapResult.getBoolean("heli");
		
		if (!jsapResult.getBoolean("help")) {
			LOGGER.debug("Setting: verbose={}", verbose);
			LOGGER.debug("Setting: timeSpan={}", timeSpan);
			LOGGER.debug("Setting: server={}", server);
			LOGGER.debug("Setting: port={}", port);
			LOGGER.debug("Setting menu={}", menu);
			LOGGER.debug("Setting sacOutput={}", sacOutput);
			LOGGER.debug("Setting txtOutput={}", txtOutput);
			LOGGER.debug("Setting rsamOutput={}", rsamOutput);
			LOGGER.debug("Setting heliOutput={}", heliOutput);
		}

		if (sacOutput || txtOutput || rsamOutput || heliOutput) {
			if (channel == null) {
				throw new RuntimeException("No channel provided.");
			}
			if (timeSpan == null) {
				throw new RuntimeException("No time span provided.");
			}
		}
		
	}
}
