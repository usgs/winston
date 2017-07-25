package gov.usgs.volcanoes.winston;

import java.io.IOException;
import java.net.SocketTimeoutException;
import java.nio.ByteBuffer;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Locale;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import gov.usgs.earthworm.WaveServer;
import gov.usgs.net.ReadListener;
import gov.usgs.plot.data.HelicorderData;
import gov.usgs.plot.data.RSAMData;
import gov.usgs.plot.data.Wave;
import gov.usgs.plot.data.file.FileType;
import gov.usgs.plot.data.file.SeismicDataFile;
import gov.usgs.volcanoes.core.Zip;
import gov.usgs.volcanoes.core.data.Scnl;
import gov.usgs.volcanoes.core.time.J2kSec;
import gov.usgs.volcanoes.core.time.TimeSpan;
import gov.usgs.volcanoes.core.util.Retriable;
import gov.usgs.volcanoes.core.util.UtilException;
import gov.usgs.volcanoes.winston.client.GetScnlRsamRawHandler;
import gov.usgs.volcanoes.winston.client.GetWaveHandler;
import gov.usgs.volcanoes.winston.client.MenuHandler;
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
public class WWSClient extends WaveServer {
	private static final Logger LOGGER = LoggerFactory.getLogger(WWSClient.class);

	protected ReadListener readListener;
	private final String server;
	private final int port;

	public WWSClient(final String server, final int port) {
		super(server, port);
		setTimeout(60000);
		//
		this.server = server;
		this.port = port;
	}

	public void setReadListener(final ReadListener rl) {
		readListener = rl;
	}

	public int getProtocolVersion() {
		int version = 1;
		try {
			if (!connected())
				connect();

			socket.setSoTimeout(1000);
			writeString("VERSION\n");
			final String result = readString();
			version = Integer.parseInt(result.split(" ")[1]);
		} catch (final Exception e) {
		} finally {
			try {
				socket.setSoTimeout(timeout);
			} catch (final Exception e) {
			}
		}
		return version;
	}

	protected byte[] getData(final String req, final boolean compressed) {
		byte[] ret = null;
		final Retriable<byte[]> rt = new Retriable<byte[]>("WWSClient.getData()", maxRetries) {
			@Override
			public void attemptFix() {
				close();
			}

			@Override
			public boolean attempt() throws UtilException {
				try {
					if (!connected())
						connect();

					writeString(req);
					final String info = readString();
					if (info.startsWith("ERROR")) {
						logger.warning("Sent: " + req);
						logger.warning("Got: " + info);
						return false;
					}

					final String[] ss = info.split(" ");
					final int bytes = Integer.parseInt(ss[1]);
					if (bytes == 0)
						return true;

					byte[] buf = readBinary(bytes, readListener);
					if (compressed)
						buf = Zip.decompress(buf);

					result = buf;
					return true;
				} catch (final SocketTimeoutException e) {
					logger.warning("WWSClient.getData() timeout.");
				} catch (final IOException e) {
					logger.warning("WWSClient.getData() IOException: " + e.getMessage());
				} catch (final NumberFormatException e) {
					logger.warning(
							"WWSClent.getData() couldn't parse server response. Is remote server a Winston Wave Server?");
				}
				return false;
			}
		};
		try {
			ret = rt.go();
		} catch (final UtilException e) {
			// Do nothing
		}
		return ret;
	}


	public HelicorderData getHelicorder(final String station, final String comp, final String network,
			final String location, final double start, final double end, final boolean compress) {
		final String req = String.format(Locale.US, "GETSCNLHELIRAW: GS %s %s %s %s %f %f %s\n", station, comp, network,
				location, start, end, (compress ? "1" : "0"));
		final byte[] buf = getData(req, compress);
		if (buf == null)
			return null;

		return new HelicorderData(ByteBuffer.wrap(buf));
	}

	public String[] getStatus() throws UtilException {
		return getStatus(0d);
	}

	public String[] getStatus(final Double d) throws UtilException {
		final double ageThreshold = d;
		final Retriable<String[]> rt = new Retriable<String[]>("WWSClient.getStatus()", maxRetries) {
			@Override
			public void attemptFix() {
				close();
			}

			@Override
			public boolean attempt() {
				try {
					if (!connected())
						connect();

					final String cmd = "STATUS: GC " + ageThreshold;
					writeString(cmd + "\n");

					final String info = readString();
					String[] ss = info.split(": ");
					final int lines = Integer.parseInt(ss[1]);
					if (lines == 0)
						return true;

					ss = new String[lines];
					for (int i = 0; i < ss.length; i++)
						ss[i] = readString();

					result = ss;
					return true;
				} catch (final SocketTimeoutException e) {
					logger.warning("WWSClient.getStatus() timeout.");
				} catch (final IOException e) {
					logger.warning("WWSClient.getChannels() IOException: " + e.getMessage());
				}
				return false;
			}
		};

		return rt.go();
	}

	
	/**
	 * Send a request to Winston and block until the response has been processed.
	 * 
	 * @param req Request string
	 * @param handler Object to handle server response
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
			io.netty.channel.Channel ch = b.connect(host, port).sync().channel();
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
	 * Request RSAM from winston.
	 * 
	 * @param scnl channel to request
	 * @param timeSpan time span to request
	 * @param period RSAM period
	 * @param doCompress if true, compress data transmitted over the network
	 * @return
	 */
	public RSAMData getRSAMData(final Scnl scnl, final TimeSpan timeSpan,final int period, final boolean doCompress) {
		RSAMData rsam = new RSAMData();
		double st = J2kSec.fromEpoch(timeSpan.startTime);
		double et = J2kSec.fromEpoch(timeSpan.endTime);
		final String req = String.format(Locale.US, "GETSCNLRSAMRAW: GS %s %f %f %d %s\n", scnl.toString(" "), st, et, period, (doCompress ? "1" : "0"));
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
	 * Retrieve a wave and write to a SAC file.
	 * 
	 * @param server Winston address
	 * @param port Winston port
	 * @param timeSpan time span to request
	 * @param scnl SCNL to request
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
	 * @param server Winston address
	 * @param port Winston port
	 * @param timeSpan time span to request
	 * @param scnl SCNL to request
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
	 * Retrieve RSAM and write to STDOUT.
	 * 
	 * @param server Winston address
	 * @param port Winston port
	 * @param timeSpan time span to request
	 * @param scnl SCNL to request
	 */
	private static void outputRsam(final String server, final int port, final TimeSpan timeSpan, final int period, final Scnl scnl) {
		System.out.println("dumping RSAM as text\n");
		final WWSClient wws = new WWSClient(server, port);
		RSAMData rsam = wws.getRSAMData(scnl, timeSpan, period, true);

		System.out.println(rsam.toCSV());
//		for (final int i : wave.buffer) {
//			System.out.println(i);
//		}
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
	 * @param meta if true, request metadata
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
	 * @param server Winston to query
	 * @param port Winston port
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
	 * @param args @see client.WWSClientArgs
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

		} catch (Exception e) {
			LOGGER.error(e.getLocalizedMessage());
		}

	}

}