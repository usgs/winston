package gov.usgs.volcanoes.winston.client;

import java.io.IOException;
import java.nio.charset.Charset;
import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import io.netty.buffer.ByteBuf;

/**
 * Receive and process response from a winston GETMENU request.
 *
 * @author Tom Parker
 */
public class VersionHandler extends WWSCommandHandler {
	private static final Logger LOGGER = LoggerFactory.getLogger(VersionHandler.class);

	private final VersionHolder version;
	
	public VersionHandler(VersionHolder version) {
		super();
		this.version = version;
	}

	@Override
	public void handle(Object msg) throws IOException {
		ByteBuf msgBuf = (ByteBuf) msg;

		String header = ClientUtils.readResponseHeader(msgBuf);
		if (header == null) {
			LOGGER.debug("Still waiting for full response line.");
			return;
		} else {
			version.version = Integer.parseInt(header.split(" ")[1]);
			sem.release();
		}
	}

}