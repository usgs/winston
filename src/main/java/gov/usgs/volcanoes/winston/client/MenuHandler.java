package gov.usgs.volcanoes.winston.client;

import java.io.IOException;
import java.nio.charset.Charset;
import java.util.List;

import io.netty.buffer.ByteBuf;
import io.netty.channel.Channel;

public class MenuHandler implements WWSCommandHandler {

	private int linesTotal;
	private int linesRead;
	private Channel channel;
	private List<gov.usgs.volcanoes.winston.Channel> channels;
	private StringBuffer menu;

	public MenuHandler(Channel channel, List<gov.usgs.volcanoes.winston.Channel> channels) {
		linesTotal = -Integer.MAX_VALUE;
		linesRead = 0;
		menu = new StringBuffer();
		this.channel = channel;
		this.channels = channels;
	}

	@Override
	public void handle(Object msg) throws IOException {
		ByteBuf msgBuf = (ByteBuf) msg;
		if (linesTotal < 0) {
			linesTotal = readLineCount(msgBuf);
		}

		String chunk = msgBuf.toString(Charset.forName("US-ASCII"));
		linesRead += countLines(chunk);
		menu.append(chunk);
		if (linesRead == linesTotal) {
			for (String line : menu.toString().split("\n")) {
				channels.add(new gov.usgs.volcanoes.winston.Channel(line));
			}
			channel.close();
		}
	}

	private int readLineCount(ByteBuf msgBuf) {
		byte ch = msgBuf.readByte();
		while (ch != ' ') {
			ch = msgBuf.readByte();
		}

		StringBuffer lenBuf = new StringBuffer();
		ch = msgBuf.readByte();
		while (ch != '\n') {
			lenBuf.append((char) ch);
			ch = msgBuf.readByte();
		}
		return Integer.parseInt(lenBuf.toString());

	}

	private int countLines(String buf) {
		int lines = 0;
		for (int pos = 0; pos < buf.length(); pos++) {
			char c = buf.charAt(pos);
			if (buf.charAt(pos) == '\n') {
				lines++;
			}
		}
		return lines;
	}
}
