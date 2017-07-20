package gov.usgs.volcanoes.winston.client;

import java.io.IOException;
import java.sql.Date;

import io.netty.buffer.ByteBuf;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelInboundHandlerAdapter;
import io.netty.util.AttributeKey;

public class WWSClientHandler extends ChannelInboundHandlerAdapter {

	public static final AttributeKey<WWSCommandHandler> handlerKey = AttributeKey.valueOf("commandHandler");

	@Override
	public void channelRead(ChannelHandlerContext ctx, Object msg) throws IOException {
		WWSCommandHandler handler = ctx.channel().attr(handlerKey).get();
		handler.handle(msg);
	}

	@Override
	public void exceptionCaught(ChannelHandlerContext ctx, Throwable cause) {
		cause.printStackTrace();
		ctx.close();
	}
}
