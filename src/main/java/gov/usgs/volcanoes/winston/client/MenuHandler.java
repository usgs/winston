package gov.usgs.volcanoes.winston.client;

import java.io.IOException;
import java.nio.charset.Charset;
import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import io.netty.buffer.ByteBuf;
import io.netty.channel.Channel;

public class MenuHandler extends WWSCommandHandler {
	private static final Logger LOGGER = LoggerFactory.getLogger(MenuHandler.class);

	private int linesTotal;
	private int linesRead;
	private final List<gov.usgs.volcanoes.winston.Channel> channels;
	private StringBuffer menu;

	public MenuHandler(List<gov.usgs.volcanoes.winston.Channel> channels) {
		super();
		linesTotal = -Integer.MAX_VALUE;
		linesRead = 0;
		menu = new StringBuffer();
		this.channels = channels;
	}

	@Override
	public void handle(Object msg) throws IOException {
		ByteBuf msgBuf = (ByteBuf) msg;

		if (linesTotal < 0) {
			String header = ClientUtils.readResponseHeader(msgBuf);
			if (header == null) {
				LOGGER.debug("Still waiting for full response line.");
				return;
			} else {
				linesTotal = Integer.parseInt(header.split(" ")[1]);
				LOGGER.debug("Server has {} channels.", linesTotal);
			}
		}

		String chunk = msgBuf.toString(Charset.forName("US-ASCII"));
		linesRead += countLines(chunk);
		menu.append(chunk);
		if (linesRead == linesTotal) {
			for (String line : menu.toString().split("\n")) {
				channels.add(new gov.usgs.volcanoes.winston.Channel(line));
			}
			sem.release();
		} else {
			LOGGER.debug("Read {} of {} channels", linesRead, linesTotal);
		}
		
	}

	private int countLines(String buf) {
		int lines = 0;
		for (int pos = 0; pos < buf.length(); pos++) {
			if (buf.charAt(pos) == '\n') {
				lines++;
			}
		}
		return lines;
	}
}