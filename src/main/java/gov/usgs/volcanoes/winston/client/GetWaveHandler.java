package gov.usgs.volcanoes.winston.client;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.nio.ByteBuffer;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import gov.usgs.plot.data.Wave;
import gov.usgs.volcanoes.core.Zip;
import io.netty.buffer.ByteBuf;
import io.netty.channel.Channel;

public class GetWaveHandler implements WWSCommandHandler {
	private static final Logger LOGGER = LoggerFactory.getLogger(GetWaveHandler.class);

	private final Wave wave;
	private Channel channel;
	private int length;
	private final boolean isCompressed;
	private ByteArrayOutputStream buf;
	private int bytesRead;
	
	public GetWaveHandler(Wave wave, boolean isCompressed) {
		this.wave = wave;
		this.isCompressed = isCompressed;
		length = -Integer.MAX_VALUE;
		buf = null;
		bytesRead = 0;
	}

	@Override
	public void handle(Object msg) throws IOException {
		if (channel == null) {
			throw new RuntimeException("Channel not set. That's a bug.");
		}
		ByteBuf msgBuf = (ByteBuf) msg;
		if (length < 0) {
			String header = ClientUtils.readResponseHeader(msgBuf);
			if (header == null) {
				LOGGER.debug("Still waiting for full response line.");
				return;
			} else {
				String[] parts = header.split(" ");
				length = Integer.parseInt(parts[1]);
				buf = new ByteArrayOutputStream(length);
				LOGGER.debug("Response length: {}", length);
				LOGGER.debug("" + buf);
			}
		}

		msgBuf.readBytes(buf, msgBuf.readableBytes());
		if (buf.size() == length) {
			LOGGER.debug("Received all bytes.");
			byte[] bytes = buf.toByteArray();
			if (isCompressed) {
				bytes = Zip.decompress(bytes);
			} 
			wave.fromBinary(ByteBuffer.wrap(bytes));
			channel.close();
		} else {
			LOGGER.debug("Still waiting for bytes. {}/{}", buf.size(), length);
		}		
	}


	@Override
	public void setChannel(Channel channel) {
		this.channel = channel;
	}
}
