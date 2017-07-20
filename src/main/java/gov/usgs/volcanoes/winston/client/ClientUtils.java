package gov.usgs.volcanoes.winston.client;

import java.io.ByteArrayOutputStream;
import java.io.IOException;

import io.netty.buffer.ByteBuf;

public class ClientUtils {
	public static String readResponseHeader(ByteBuf msgBuf) throws IOException {
		int eol = msgBuf.indexOf(msgBuf.readerIndex(), msgBuf.writerIndex(), (byte) '\n');
		if (eol == -1) {
			return null;
		} else {
			ByteArrayOutputStream bos = new ByteArrayOutputStream();
			msgBuf.readBytes(bos, eol - msgBuf.readerIndex());
			byte newLine = msgBuf.readByte();
			return bos.toString();
		}
	}
}
