package io.zbus.proxy.http;

import io.netty.channel.Channel;
import io.netty.handler.codec.http.HttpRequest;
import io.zbus.kit.StrKit;

public interface RequestInterceptor {
	/**
	 * 
	 * @param request
	 * @return true to continue
	 */
	boolean intercept(HttpRequest request, ProxyTarget target, Channel inboundChannel);
	
	
	public static class DefaultRequestInterceptor implements RequestInterceptor{

		@Override
		public boolean intercept(HttpRequest request, ProxyTarget target, Channel inboundChannel) { 
			if(!StrKit.isEmpty(target.urlRewrite)) {
				String uri = request.uri(); 
				uri = target.urlRewrite + uri.substring(target.urlPrefix.length()); 
				request.setUri(uri);
			} 
			return true;
		}
		
	}
}
