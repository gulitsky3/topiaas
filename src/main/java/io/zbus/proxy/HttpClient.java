package io.zbus.proxy;

import java.io.Closeable;
import java.io.UnsupportedEncodingException;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URLDecoder;
import java.util.Iterator;
import java.util.Map.Entry;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import java.util.concurrent.atomic.AtomicReference;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import io.netty.bootstrap.Bootstrap;
import io.netty.buffer.ByteBuf;
import io.netty.channel.Channel;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelInitializer;
import io.netty.channel.ChannelPipeline;
import io.netty.channel.EventLoopGroup;
import io.netty.channel.SimpleChannelInboundHandler;
import io.netty.channel.socket.SocketChannel;
import io.netty.channel.socket.nio.NioSocketChannel;
import io.netty.handler.codec.http.DefaultFullHttpRequest;
import io.netty.handler.codec.http.FullHttpMessage;
import io.netty.handler.codec.http.HttpClientCodec;
import io.netty.handler.codec.http.HttpMessage;
import io.netty.handler.codec.http.HttpMethod;
import io.netty.handler.codec.http.HttpObject;
import io.netty.handler.codec.http.HttpObjectAggregator;
import io.netty.handler.codec.http.HttpRequest;
import io.netty.handler.codec.http.HttpResponse;
import io.netty.handler.codec.http.HttpVersion;
import io.zbus.kit.HttpKit;
import io.zbus.transport.Message;
import io.zbus.transport.http.Http;

public final class HttpClient extends SimpleChannelInboundHandler<HttpObject> implements Closeable{
	private static final Logger logger = LoggerFactory.getLogger(HttpClient.class); 

	private String host;
	private int port;
	private String urlPrefix;
	private EventLoopGroup group; 
	private Channel channel;
	private CountDownLatch countDownLatch;
	private AtomicReference<Message> returnMessage = new AtomicReference<Message>();

	public HttpClient(String url, EventLoopGroup group) {
		URI uri;
		try {
			uri = new URI(url);
		} catch (URISyntaxException e) {
			throw new IllegalArgumentException(url);
		}
		
		String scheme = uri.getScheme() == null ? "http" : uri.getScheme();
		String host = uri.getHost() == null ? "127.0.0.1" : uri.getHost();
		this.urlPrefix = uri.getPath();
		int port = uri.getPort();
		if (port == -1) {
			if ("http".equalsIgnoreCase(scheme)) {
				port = 80;
			} else if ("https".equalsIgnoreCase(scheme)) {
				port = 443;
			}
		}

		if (!"http".equalsIgnoreCase(scheme) && !"https".equalsIgnoreCase(scheme)) {
			throw new IllegalArgumentException("Only HTTP(S) is supported."); 
		}

		this.host = host;
		this.port = port;
		this.group = group; 
	}
	
	public boolean isConnected() { 
		return channel != null && channel.isActive() && channel.isOpen();
	}
	
	public void close() {
		if(channel == null) return;
		channel.close();
		channel = null;
	}

	public synchronized void connect() throws InterruptedException {
		if (channel != null)
			return;

		Bootstrap b = new Bootstrap();
		b.group(group)
		.channel(NioSocketChannel.class)
		.handler(new ChannelInitializer<SocketChannel>() {
			@Override
			public void initChannel(SocketChannel ch) {
				ChannelPipeline p = ch.pipeline();
				p.addLast(new HttpClientCodec());
				p.addLast(new HttpObjectAggregator(1024 * 1024 * 16));
				p.addLast(HttpClient.this);
			}
		});

		channel = b.connect(host, port).sync().channel(); 
	} 
	
	public synchronized Message request(Message msg, long timeout, TimeUnit unit) 
			throws InterruptedException, TimeoutException  {

		
		FullHttpMessage httpMessage = new DefaultFullHttpRequest(HttpVersion.HTTP_1_1, 
				HttpMethod.valueOf(msg.getMethod()),
				msg.getUrl());  
		
		//httpMessage.headers().set("connection", "Keep-Alive");  
		for (Entry<String, String> e : msg.getHeaders().entrySet()) { 
			httpMessage.headers().set(e.getKey().toLowerCase(), e.getValue());
		}
		byte[] body = Http.body(msg);
		httpMessage.headers().add(Http.CONTENT_LENGTH, body.length+"");
		httpMessage.content().writeBytes(Http.body(msg)); 
		
	    countDownLatch = new CountDownLatch(1); 
		channel.writeAndFlush(httpMessage); 
		
		countDownLatch.await(timeout, unit);
		Message result = returnMessage.getAndSet(null);
		if(result != null) return result;   
		
		throw new TimeoutException("request timeout");
	}

	@Override
	public void channelRead0(ChannelHandlerContext ctx, HttpObject obj) {
		//2) HTTP mode
		if(!(obj instanceof HttpMessage)){
			throw new IllegalArgumentException("HttpMessage object required: " + obj);
		}
		 
		HttpMessage httpMsg = (HttpMessage) obj; 
		Message msg = decodeHeaders(httpMsg); 
		String contentType = msg.getHeader(Http.CONTENT_TYPE);
		
		//Body
		ByteBuf body = null;
		if (httpMsg instanceof FullHttpMessage) {
			FullHttpMessage fullReq = (FullHttpMessage) httpMsg;
			body = fullReq.content();
		} 
	 
		if (body != null) {
			int size = body.readableBytes();
			if (size > 0) {
				byte[] data = new byte[size];
				body.readBytes(data);
				if (HttpKit.isText(contentType)) {
					try {
						String charset = Http.charset(contentType);
						msg.setBody(new String(data, charset));
					} catch (Exception e) {
						logger.error(e.getMessage(), e);
						msg.setBody(new String(data));
					}
				} else {
					msg.setBody(data);
				}
			}
		}
		
		returnMessage.set(msg);
		countDownLatch.countDown();
	}
	
	private Message decodeHeaders(HttpMessage httpMsg){
		Message msg = new Message();
		Iterator<Entry<String, String>> iter = httpMsg.headers().iteratorAsString();
		while (iter.hasNext()) {
			Entry<String, String> e = iter.next();
			msg.setHeader(e.getKey().toLowerCase(), e.getValue()); 
		}  

		if (httpMsg instanceof HttpRequest) {
			HttpRequest req = (HttpRequest) httpMsg;
			msg.setMethod(req.method().name()); 
			String url = req.uri();
			try {
				url = URLDecoder.decode(req.uri(), "UTF-8");
			} catch (UnsupportedEncodingException e) {
				//ignore
			}
			msg.setUrl(url);
		} else if (httpMsg instanceof HttpResponse) {
			HttpResponse resp = (HttpResponse) httpMsg;
			int status = resp.status().code();
			msg.setStatus(status);
		}
		return msg;
	}
	

	@Override
	public void exceptionCaught(ChannelHandlerContext ctx, Throwable cause) {
		cause.printStackTrace();
		ctx.close();
	}
	
	public String getUrlPrefix() {
		return urlPrefix;
	} 
}
