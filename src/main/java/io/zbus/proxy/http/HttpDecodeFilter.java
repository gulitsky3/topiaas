package io.zbus.proxy.http;

import java.util.List;

import io.netty.channel.ChannelHandlerContext;
import io.netty.handler.codec.http.HttpRequest;
import io.netty.util.ReferenceCountUtil;
import io.zbus.transport.http.DecodeFilter;

public class HttpDecodeFilter implements DecodeFilter{ 
	private ProxyUrlMatcher urlMatcher;
	
	public HttpDecodeFilter(ProxyUrlMatcher urlMatcher) {
		this.urlMatcher = urlMatcher;
	}
	
	@Override
	public boolean decode(ChannelHandlerContext ctx, Object obj, List<Object> out) {
		if(obj instanceof HttpRequest) {
			HttpRequest req = (HttpRequest)obj;
			ProxyTarget target = urlMatcher.match(req.uri());
			if(target != null) {
				ReferenceCountUtil.retain(req);
				out.add(req);
				return true;
			} 
		}
		return false;
	} 
}
