package io.zbus.proxy.http;

import java.io.IOException;

import io.netty.bootstrap.Bootstrap;
import io.netty.buffer.Unpooled;
import io.netty.channel.Channel;
import io.netty.channel.ChannelFuture;
import io.netty.channel.ChannelFutureListener;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelInitializer;
import io.netty.channel.ChannelPipeline;
import io.netty.channel.socket.SocketChannel;
import io.netty.handler.codec.http.HttpClientCodec;
import io.netty.handler.codec.http.HttpObjectAggregator;
import io.netty.handler.codec.http.HttpRequest;
import io.netty.handler.ssl.SslContext;
import io.netty.util.Attribute;
import io.netty.util.AttributeKey;
import io.zbus.proxy.http.RequestInterceptor.DefaultRequestInterceptor;
import io.zbus.transport.Session; 
 
public class HttpProxyHandler {   
	public static final AttributeKey<ProxyTargetChannel> OutboundKey = AttributeKey.valueOf("outbound");
	private ProxyUrlMatcher urlMatcher;
	private RequestInterceptor requestInterceptor = new DefaultRequestInterceptor();
	
	public HttpProxyHandler(ProxyUrlMatcher urlMatcher) {  
		this.urlMatcher = urlMatcher;
	}  
	
	public void onMessage(Object obj, Session sess) throws IOException {
		if (!(obj instanceof HttpRequest)) {
			return;
		} 
		ChannelHandlerContext ctx = (ChannelHandlerContext) sess.channelContext();
		if (ctx == null) return;

		HttpRequest msg = (HttpRequest) obj;
		final ProxyTarget target = urlMatcher.match(msg.uri());
		if (target == null) {
			return; //TODO logger?
		}

		Attribute<ProxyTargetChannel> outbound = ctx.channel().attr(OutboundKey);
		ProxyTargetChannel targetChannel = outbound.get();
		Channel inboundChannel = ctx.channel();

		if (targetChannel == null) {
			targetChannel = new ProxyTargetChannel();
			targetChannel.blockedInboundMessages.add(msg);
			outbound.set(targetChannel);
			final ProxyTargetChannel theTarget = targetChannel;

			Bootstrap b = new Bootstrap(); 
			b.group(inboundChannel.eventLoop())
					.channel(ctx.channel().getClass())
					.handler(new ChannelInitializer<SocketChannel>() {
						@Override
						protected void initChannel(SocketChannel ch) throws Exception {
							ChannelPipeline p = ch.pipeline();
							// Enable HTTPS if necessary.
							SslContext sslCtx = target.sslCtx;
							if (sslCtx != null) {
								p.addLast(sslCtx.newHandler(ch.alloc()));
							}

							p.addLast(new HttpClientCodec());
							p.addLast(new HttpObjectAggregator(1024 * 1024 * 64)); //TODO
							p.addLast(new TargetClientHandler(inboundChannel));
						}
					});
			
			ChannelFuture f = b.connect(target.remoteHost, target.remotePort);
			targetChannel.outboundChannel = f.channel();
			f.addListener(new ChannelFutureListener() {
				@Override
				public void operationComplete(ChannelFuture future) {
					if (future.isSuccess()) {
						synchronized (theTarget.blockedInboundMessages) {
							for (HttpRequest msg : theTarget.blockedInboundMessages) {
								boolean next = true;
								if (requestInterceptor != null) {
									next = requestInterceptor.intercept(msg, target, inboundChannel);
								}
								if (next) {
									theTarget.outboundChannel.writeAndFlush(msg);
								}
							}
							theTarget.blockedInboundMessages.clear();
						}
					} else {
						inboundChannel.close();
					}
				}
			});
		} else {
			if (targetChannel.outboundChannel == null || !targetChannel.outboundChannel.isActive()) {
				synchronized (targetChannel.blockedInboundMessages) {
					targetChannel.blockedInboundMessages.add(msg);
				}
			} else {
				boolean next = true;
				if (requestInterceptor != null) {
					next = requestInterceptor.intercept(msg, target, inboundChannel);
				}
				if (next) {
					targetChannel.outboundChannel.writeAndFlush(msg);
				}
			}
		}
	}
	
    public static void closeOnFlush(Channel ch) {
        if (ch.isActive()) {
            ch.writeAndFlush(Unpooled.EMPTY_BUFFER).addListener(ChannelFutureListener.CLOSE);
        }
    }
    
    public static Channel outboundChannel(Session sess) {
    	Object ctxObject = sess.channelContext();
		if(ctxObject != null && ctxObject instanceof ChannelHandlerContext) {
			ChannelHandlerContext ctx = (ChannelHandlerContext)ctxObject;
			Attribute<ProxyTargetChannel> outbound = ctx.channel().attr(OutboundKey);
			if(outbound.get() != null) {
				return outbound.get().outboundChannel;
			} 
		}  
		return null;
    }
} 
