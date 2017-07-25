/**
 * I waive copyright and related rights in the this work worldwide through the CC0 1.0 Universal
 * public domain dedication. https://creativecommons.org/publicdomain/zero/1.0/legalcode
 */

package gov.usgs.volcanoes.winston.server.wws.cmd;

import java.nio.ByteBuffer;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import gov.usgs.plot.data.Wave;
import gov.usgs.volcanoes.core.Zip;
import gov.usgs.volcanoes.core.util.UtilException;
import gov.usgs.volcanoes.winston.db.Data;
import gov.usgs.volcanoes.winston.db.WinstonDatabase;
import gov.usgs.volcanoes.winston.server.MalformedCommandException;
import gov.usgs.volcanoes.winston.server.wws.WinstonConsumer;
import gov.usgs.volcanoes.winston.server.wws.WwsBaseCommand;
import gov.usgs.volcanoes.winston.server.wws.WwsCommandString;
import io.netty.channel.ChannelHandlerContext;

/**
 * Return Channel details.
 * 
 * @author Dan Cervelli
 * @author Tom Parker
 */
public class GetWaveRawCommand extends WwsBaseCommand {
	@SuppressWarnings("unused")
	private static final Logger LOGGER = LoggerFactory.getLogger(GetWaveRawCommand.class);

	/**
	 * Constructor.
	 */
	public GetWaveRawCommand() {
		super();
	}

	public void doCommand(ChannelHandlerContext ctx, WwsCommandString cmd)
			throws MalformedCommandException, UtilException {

		if (!cmd.isLegalSCNLTT(9))
			return; // malformed command

		final double et = cmd.getT2(true);
		final double st = cmd.getT1(true);
		final String scnl = cmd.getWinstonSCNL();

		if (st >= et) {
			throw new MalformedCommandException();
		}

		WinstonDatabase winston = null;
		Wave wave;
		try {
			wave = databasePool.doCommand(new WinstonConsumer<Wave>() {

				public Wave execute(WinstonDatabase winston) throws UtilException {
					Data data = new Data(winston);
					return data.getWave(scnl, st, et, 0);
				}
			});
		} catch (Exception e1) {
			throw new UtilException(e1.getMessage());
		}

		//
		// try {
		// winston = databasePool.borrowObject();
		// if (!winston.checkConnect()) {
		// LOGGER.error("WinstonDatabase unable to connect to MySQL.");
		// } else {
		// Data data = new Data(winston);
		// wave = data.getWave(cmd.getWinstonSCNL(), st, et, 0);
		// }
		// } catch (Exception e) {
		// LOGGER.error("Unable to fulfill command.", e);
		// } finally {
		// if (winston != null) {
		// databasePool.returnObject(winston);
		// }
		// }

		ByteBuffer bb = null;
		if (wave != null && wave.numSamples() > 0)
			bb = (ByteBuffer) wave.toBinary().flip();
		else
			bb = ByteBuffer.allocate(0);

		String id = cmd.getID();

		if (cmd.getInt(8) == 1)
			bb = ByteBuffer.wrap(Zip.compress(bb.array()));

		if (bb != null) {
			ctx.write(id + " " + bb.limit() + "\n");
			ctx.writeAndFlush(bb.array());
		} else {
			throw new UtilException("Unable to compress results.");
		}
	}
}
