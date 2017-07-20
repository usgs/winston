package gov.usgs.volcanoes.winston.client;

import java.io.IOException;

import io.netty.channel.Channel;

public interface WWSCommandHandler {
	public void handle(Object msg) throws IOException;
	public void setChannel(Channel channel);
}
