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
			new FlaggedOption("port", JSAP.STRING_PARSER, "16022", false, 'p', "port", "Remote port."),
			new FlaggedOption("server", JSAP.STRING_PARSER, JSAP.NO_DEFAULT, true, 's', "server", "Remote server."),
			new Switch("sac", 's', "sac", "Write SAC output."), new Switch("txt", 't', "txt", "Write TEXT output.") };

	/** If true, log more. */
	public final boolean verbose;

	/** Remote server. */
	public final String server;

	/** Remote server port. */
	public final int port;

	/** Timespan to retrieve. */
	public final TimeSpan timeSpan;

	/** channel to query */
	public final Scnl channel;
	
	/** If true, write SAC output. */
	public final boolean sacOutput;
	
	/** If true, write TXT output. */
	public final boolean txtOutput;
	
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
		args = new ScnlArg(true, args);
		args = new TimeSpanArg(INPUT_TIME_FORMAT, true, args);
		args = new VerboseArg(args);

		JSAPResult jsapResult = null;
		jsapResult = args.parse(commandLineArgs);

		verbose = jsapResult.getBoolean("verbose");
		LOGGER.debug("Setting: verbose={}", verbose);

		timeSpan = (TimeSpan) jsapResult.getObject("timeSpan");
		LOGGER.debug("Setting: timeSpan={}", timeSpan);
		
		server = jsapResult.getString("server");
		LOGGER.debug("Setting: server={}", server);

		port = jsapResult.getInt("port");
		LOGGER.debug("Setting: port={}", port);
		
		channel = (Scnl) jsapResult.getObject("channel");
		
		sacOutput = jsapResult.getBoolean("sac");
		LOGGER.debug("Setting sacOutput={}", sacOutput);
		
		txtOutput = jsapResult.getBoolean("txt");
		LOGGER.debug("Setting txtOutput={}", txtOutput);
	}
}
