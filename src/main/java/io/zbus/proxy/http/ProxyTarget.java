package io.zbus.proxy.http;

import io.netty.handler.ssl.SslContext;

public class ProxyTarget {
	public String urlPrefix;
	public String urlRewrite;
	
	public String remoteHost;
	public int remotePort;  
	
	public SslContext sslCtx;
}
