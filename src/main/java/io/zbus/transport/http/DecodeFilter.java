package io.zbus.transport.http;

import java.util.List;

import io.netty.channel.ChannelHandlerContext;

public interface DecodeFilter {
	/**
	 * 
	 * @param ctx
	 * @param obj
	 * @param out
	 * @return false to continue decode
	 */
	boolean decode(ChannelHandlerContext ctx, Object obj, List<Object> out);
}
