package io.zbus.proxy.http;

import java.util.ArrayList;
import java.util.List;

import io.netty.channel.Channel;
import io.netty.handler.codec.http.HttpRequest;

public class ProxyTargetChannel { 
	public Channel outboundChannel;  
	public List<HttpRequest> blockedInboundMessages = new ArrayList<>(); 
}
