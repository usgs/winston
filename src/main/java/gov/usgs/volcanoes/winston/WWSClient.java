package gov.usgs.volcanoes.winston;

import java.io.IOException;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Locale;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import gov.usgs.net.ReadListener;
import gov.usgs.plot.data.HelicorderData;
import gov.usgs.plot.data.RSAMData;
import gov.usgs.plot.data.Wave;
import gov.usgs.plot.data.file.FileType;
import gov.usgs.plot.data.file.SeismicDataFile;
import gov.usgs.volcanoes.core.data.Scnl;
import gov.usgs.volcanoes.core.time.J2kSec;
import gov.usgs.volcanoes.core.time.TimeSpan;
import gov.usgs.volcanoes.winston.client.GetScnlHeliRawHandler;
import gov.usgs.volcanoes.winston.client.GetScnlRsamRawHandler;
import gov.usgs.volcanoes.winston.client.GetWaveHandler;
import gov.usgs.volcanoes.winston.client.MenuHandler;
import gov.usgs.volcanoes.winston.client.VersionHandler;
import gov.usgs.volcanoes.winston.client.VersionHolder;
import gov.usgs.volcanoes.winston.client.WWSClientArgs;
import gov.usgs.volcanoes.winston.client.WWSClientHandler;
import gov.usgs.volcanoes.winston.client.WWSCommandHandler;
import io.netty.bootstrap.Bootstrap;
import io.netty.channel.ChannelFuture;
import io.netty.channel.ChannelInitializer;
import io.netty.channel.ChannelOption;
import io.netty.channel.EventLoopGroup;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.SocketChannel;
import io.netty.channel.socket.nio.NioSocketChannel;
import io.netty.handler.codec.string.StringEncoder;
import io.netty.util.AttributeKey;

/**
 * A class that extends the Earthworm Wave Server to include a get helicorder
 * function for WWS.
 *
 * @author Dan Cervelli
 * @author Tom Parker
 */
public class WWSClient {
	private static final Logger LOGGER = LoggerFactory.getLogger(WWSClient.class);

	protected ReadListener readListener;
	private final String server;
	private final int port;

	public WWSClient(final String server, final int port) {
		this.server = server;
		this.port = port;
	}

	/**
	 * Send a request to Winston and block until the response has been
	 * processed.
	 * 
	 * @param req
	 *            Request string
	 * @param handler
	 *            Object to handle server response
	 */
	private void sendRequest(String req, WWSCommandHandler handler) {
		EventLoopGroup workerGroup = new NioEventLoopGroup();

		try {
			Bootstrap b = new Bootstrap();
			b.group(workerGroup);
			b.channel(NioSocketChannel.class);
			b.option(ChannelOption.SO_KEEPALIVE, true);
			b.handler(new ChannelInitializer<SocketChannel>() {
				@Override
				public void initChannel(SocketChannel ch) throws Exception {
					ch.pipeline().addLast(new StringEncoder()).addLast(new WWSClientHandler());
				}
			});

			AttributeKey<WWSCommandHandler> handlerKey = WWSClientHandler.handlerKey;
			// Start the client.
			io.netty.channel.Channel ch = b.connect(server, port).sync().channel();
			ch.attr(handlerKey).set(handler);
			System.err.println("Sending: " + req);

			@SuppressWarnings("unused")
			ChannelFuture lastWriteFuture = ch.writeAndFlush(req);

			// wait until response has been received
			handler.responseWait();
			ch.close();
		} catch (InterruptedException ex) {
			Thread.currentThread().interrupt();
			throw new RuntimeException(ex);
		} finally {
			workerGroup.shutdownGracefully();
		}
	}

	
	/**
	 * Return protocol version used by remote winston.
	 * 
	 * @return protocol version
	 */
	public int getProtocolVersion() {
		VersionHolder version = new VersionHolder();
		sendRequest("VERSION", new VersionHandler(version));

		return version.version;
	}

	
	/**
	 * Request RSAM from winston.
	 * 
	 * @param scnl
	 *            channel to request
	 * @param timeSpan
	 *            time span to request
	 * @param period
	 *            RSAM period
	 * @param doCompress
	 *            if true, compress data transmitted over the network
	 * @return
	 */
	public RSAMData getRSAMData(final Scnl scnl, final TimeSpan timeSpan, final int period, final boolean doCompress) {
		RSAMData rsam = new RSAMData();
		double st = J2kSec.fromEpoch(timeSpan.startTime);
		double et = J2kSec.fromEpoch(timeSpan.endTime);
		final String req = String.format(Locale.US, "GETSCNLRSAMRAW: GS %s %f %f %d %s\n", scnl.toString(" "), st, et,
				period, (doCompress ? "1" : "0"));
		sendRequest(req, new GetScnlRsamRawHandler(rsam, doCompress));

		return rsam;

	}

	/**
	 * Fetch a wave data from a Winston.
	 * 
	 * @param station
	 * @param comp
	 * @param network
	 * @param location
	 * @param start
	 * @param end
	 * @param doCompress
	 * @return
	 */
	public Wave getWave(final String station, final String comp, final String network, final String location,
			final double start, final double end, final boolean doCompress) {

		Scnl scnl = new Scnl(station, comp, network, location);
		TimeSpan timeSpan = new TimeSpan(J2kSec.asEpoch(start), J2kSec.asEpoch(end));

		return getWave(scnl, timeSpan, doCompress);
	}

	/**
	 * Fetch a wave data from a Winston.
	 * 
	 * @param scnl
	 *            channel to query
	 * @param timeSpan
	 *            time span to query
	 * @param doCompress
	 *            if true, compress data over the network
	 * @return wave data, empty if no data is avilable
	 */
	public Wave getWave(final Scnl scnl, final TimeSpan timeSpan, final boolean doCompress) {
		Wave wave = new Wave();
		double st = J2kSec.fromEpoch(timeSpan.startTime);
		double et = J2kSec.fromEpoch(timeSpan.endTime);
		final String req = String.format(Locale.US, "GETWAVERAW: GS %s %f %f %s\n", scnl.toString(" "), st, et,
				(doCompress ? "1" : "0"));
		sendRequest(req, new GetWaveHandler(wave, doCompress));
		wave.setStartTime(st);
		return new Wave(wave);
	}

	/**
	 * Fetch helicorder data from Winston.
	 * 
	 * @param station
	 * @param comp
	 * @param network
	 * @param location
	 * @param start
	 * @param end
	 * @param doCompress
	 * @return
	 */
	public HelicorderData getHelicorder(final String station, final String comp, final String network,
			final String location, final double start, final double end, final boolean doCompress) {
		Scnl scnl = new Scnl(station, comp, network, location);
		TimeSpan timeSpan = new TimeSpan(J2kSec.asEpoch(start), J2kSec.asEpoch(end));

		return getHelicorder(scnl, timeSpan, doCompress);
	}

	/**
	 * Fetch helicorder data from Winston.
	 * 
	 * @param scnl
	 *            channel to query
	 * @param timeSpan
	 *            time span to query
	 * @param doCompress
	 *            if true, compress data before sending
	 * @return one second max/min values
	 */
	public HelicorderData getHelicorder(final Scnl scnl, final TimeSpan timeSpan, final boolean doCompress) {
		HelicorderData heliData = new HelicorderData();
		double st = J2kSec.fromEpoch(timeSpan.startTime);
		double et = J2kSec.fromEpoch(timeSpan.endTime);
		final String req = String.format(Locale.US, "GETSCNLHELIRAW: GS %s %f %f %s\n", scnl.toString(" "), st, et,
				(doCompress ? "1" : "0"));
		sendRequest(req, new GetScnlHeliRawHandler(heliData, doCompress));

		return heliData;
	}

	/**
	 * Retrieve a wave and write to a SAC file.
	 * 
	 * @param server
	 *            Winston address
	 * @param port
	 *            Winston port
	 * @param timeSpan
	 *            time span to request
	 * @param scnl
	 *            SCNL to request
	 */
	private static void outputSac(final String server, final int port, final TimeSpan timeSpan, final Scnl scnl) {
		final DateFormat df = new SimpleDateFormat("yyyyMMddHHmmss");
		final String date = df.format(new Date(timeSpan.startTime)) + "-" + df.format(new Date(timeSpan.endTime));

		String filename = scnl.toString("_") + "_" + date + ".sac";
		System.out.println("Writing wave to SAC\n");
		final WWSClient wws = new WWSClient(server, port);
		Wave wave = wws.getWave(scnl, timeSpan, true);
		System.err.println("Date: " + J2kSec.toDateString(wave.getStartTime()));
		final SeismicDataFile file = SeismicDataFile.getFile(filename, FileType.SAC);
		file.putWave(scnl.toString("$"), wave);
		try {
			file.write();
		} catch (final IOException e) {
			System.err.println("Couldn't write file: " + e.getLocalizedMessage());
			e.printStackTrace();
		}
	}

	/**
	 * Retrieve a wave and write to STDOUT.
	 * 
	 * @param server
	 *            Winston address
	 * @param port
	 *            Winston port
	 * @param timeSpan
	 *            time span to request
	 * @param scnl
	 *            SCNL to request
	 */
	private static void outputText(final String server, final int port, final TimeSpan timeSpan, final Scnl scnl) {
		System.out.println("dumping samples as text\n");
		final WWSClient wws = new WWSClient(server, port);
		Wave wave = wws.getWave(scnl, timeSpan, true);

		for (final int i : wave.buffer) {
			System.out.println(i);
		}
	}

	/**
	 * Retrieve Heli and write to STDOUT.
	 * 
	 * @param server
	 *            Winston address
	 * @param port
	 *            Winston port
	 * @param timeSpan
	 *            time span to request
	 * @param scnl
	 *            SCNL to request
	 */
	private static void outputHeli(final String server, final int port, final TimeSpan timeSpan, final Scnl scnl) {
		System.out.println("dumping Heli data as text\n");
		final WWSClient wws = new WWSClient(server, port);
		HelicorderData heliData = wws.getHelicorder(scnl, timeSpan, true);

		System.out.println(heliData.toCSV());
	}

	/**
	 * Retrieve RSAM and write to STDOUT.
	 * 
	 * @param server
	 *            Winston address
	 * @param port
	 *            Winston port
	 * @param timeSpan
	 *            time span to request
	 * @param scnl
	 *            SCNL to request
	 */
	private static void outputRsam(final String server, final int port, final TimeSpan timeSpan, final int period,
			final Scnl scnl) {
		System.out.println("dumping RSAM as text\n");
		final WWSClient wws = new WWSClient(server, port);
		RSAMData rsam = wws.getRSAMData(scnl, timeSpan, period, true);

		System.out.println(rsam.toCSV());
	}

	/**
	 * Retrieve a list of channels from a remote Winston.
	 * 
	 * @return List of channels
	 */
	public List<Channel> getChannels() {
		return getChannels(false);
	}

	/**
	 * Retrieve a list of channels from Winston.
	 * 
	 * @param meta
	 *            if true, request metadata
	 * @return List of channels
	 */
	public List<Channel> getChannels(final boolean meta) {
		List<Channel> channels = new ArrayList<Channel>();
		String req = String.format("GETCHANNELS: GC%s\r\n", meta ? " METADATA" : "");

		sendRequest(req, new MenuHandler(channels));
		return channels;
	}

	/**
	 * Print server menu to STDOUT.
	 * 
	 * @param server
	 *            Winston to query
	 * @param port
	 *            Winston port
	 */
	private static void displayMenu(final String server, final int port) {
		WWSClient wws = new WWSClient(server, port);
		List<Channel> channels = wws.getChannels();
		System.out.println("GOT channels: " + channels.size());
		for (Channel chan : channels) {
			System.out.println(chan.toMetadataString());
		}

	}

	/**
	 * Here's where it all begins
	 * 
	 * @param args
	 * @see client.WWSClientArgs
	 */
	public static void main(final String[] args) {
		try {
			final WWSClientArgs config = new WWSClientArgs(args);

			if (config.menu) {
				LOGGER.debug("Requesting menu from {}:{}.", config.server, config.port);
				displayMenu(config.server, config.port);
			}

			if (config.sacOutput) {
				LOGGER.debug("Requesting {} from {}:{} for {} and writing to SAC.", config.channel, config.server,
						config.port, config.timeSpan);
				outputSac(config.server, config.port, config.timeSpan, config.channel);
			}

			if (config.txtOutput) {
				LOGGER.debug("Requesting {} from {}:{} for {} and writing to TXT.", config.channel, config.server,
						config.port, config.timeSpan);
				outputText(config.server, config.port, config.timeSpan, config.channel);
			}

			if (config.rsamOutput) {
				LOGGER.debug("Requesting RSAM {} from {}:{} for {} and writing to TXT.", config.channel, config.server,
						config.port, config.timeSpan);
				outputRsam(config.server, config.port, config.timeSpan, config.rsamPeriod, config.channel);
			}

			if (config.heliOutput) {
				LOGGER.debug("Requesting helicorder data {} from {}:{} for {}.", config.channel, config.server,
						config.port, config.timeSpan);
				outputHeli(config.server, config.port, config.timeSpan, config.channel);
			}

		} catch (Exception e) {
			LOGGER.error(e.getLocalizedMessage());
		}

	}

}
