package gov.usgs.volcanoes.winston.client;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.nio.ByteBuffer;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import gov.usgs.plot.data.Wave;
import gov.usgs.volcanoes.core.Zip;
import io.netty.buffer.ByteBuf;

/**
 * Receive and process response from a winston GETWAVE request.
 *
 * @author Tom Parker
 */
public class GetWaveHandler extends WWSCommandHandler {
	private static final Logger LOGGER = LoggerFactory.getLogger(GetWaveHandler.class);

	private final Wave wave;
	private int length;
	private final boolean isCompressed;
	private ByteArrayOutputStream buf;

	public GetWaveHandler(Wave wave, boolean isCompressed) {
		this.wave = wave;
		this.isCompressed = isCompressed;
		length = -Integer.MAX_VALUE;
		buf = null;
	}

	@Override
	public void handle(Object msg) throws IOException {
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
			sem.release();
		} else {
			LOGGER.debug("Still waiting for bytes. {}/{}", buf.size(), length);
		}

	}

}
