package io.zbus.proxy.tcp;

import java.io.Closeable;
import java.io.IOException;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import io.netty.bootstrap.ServerBootstrap;
import io.netty.channel.Channel;
import io.netty.channel.ChannelInitializer;
import io.netty.channel.ChannelOption;
import io.netty.channel.ChannelPipeline;
import io.netty.channel.EventLoopGroup;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.SocketChannel;
import io.netty.channel.socket.nio.NioServerSocketChannel;
import io.netty.handler.logging.LogLevel;
import io.netty.handler.logging.LoggingHandler;
import io.zbus.kit.ConfigKit;
import io.zbus.kit.NetKit;

public final class TcpProxy implements Closeable {
	private static final Logger logger = LoggerFactory.getLogger(TcpProxy.class); 
	
	private EventLoopGroup bossGroup;
	private EventLoopGroup workerGroup;
	private boolean ownBossGroup;
	private boolean ownWorkerGroup;
	
	private String localHost = "0.0.0.0";
	private int localPort;
	private String remoteHost;
	private int remotePort;
	
	private boolean logEnabled = false;
	
	private Channel serverChannel;
	
	public TcpProxy() { 
	
	}
	
	public TcpProxy(String localAddress, String remoteAddress) {
		Object[] hp = NetKit.hostPort(localAddress, 80);
		this.localHost = (String)hp[0];
		this.localPort = (int)hp[1];
		
		hp = NetKit.hostPort(remoteAddress); 
		this.remoteHost = (String)hp[0];
		this.remotePort = (int)hp[1];
	}
	
	public TcpProxy(String localHost, int localPort, String remoteHost, int remotePort) {
		this.localHost = localHost;
		this.localPort = localPort;
		this.remoteHost = remoteHost;
		this.remotePort = remotePort;
	}
	
	public TcpProxy(int localPort, String remoteHost, int remotePort) {
		this("0.0.0.0", localPort, remoteHost, remotePort);
	}
	
	public synchronized Channel start() throws Exception{
		if(remoteHost == null) {
			throw new IllegalStateException("remoteHost not set");
		}
		if(remotePort == 0) {
			throw new IllegalStateException("remotePort not set");
		}
		if(localPort == 0) {
			throw new IllegalStateException("localPort not set");
		}
		if(bossGroup == null) {
			bossGroup = new NioEventLoopGroup(1);
			ownBossGroup = true;
		}
		if(workerGroup == null) {
			workerGroup = new NioEventLoopGroup();
			ownWorkerGroup = true;
		}
		 
		logger.info("Proxying " + localHost + ":" + localPort + " to " + remoteHost + ':' + remotePort + " ...");
        ServerBootstrap b = new ServerBootstrap();
        serverChannel = b.group(bossGroup, workerGroup)
         .channel(NioServerSocketChannel.class)
         .handler(new LoggingHandler(LogLevel.INFO))
         .childHandler(new ChannelInitializer<SocketChannel>() { 
			@Override
			protected void initChannel(SocketChannel ch) throws Exception { 
				ChannelPipeline p = ch.pipeline();
				if(logEnabled) {
					p.addLast(new LoggingHandler(LogLevel.INFO));
				}
				p.addLast( 
		            new TcpFrontendHandler(remoteHost, remotePort)
		        );
			} 
         })
         .childOption(ChannelOption.AUTO_READ, false)
         .bind(localHost, localPort).sync().channel();
        
        return serverChannel; 
	}
	
	@Override
	public void close() throws IOException {
		if(serverChannel != null) {
			serverChannel.close();
			serverChannel = null;
		}
		if(ownBossGroup && bossGroup != null) {
			this.bossGroup.shutdownGracefully();
			bossGroup = null;
		}
		if(ownWorkerGroup && workerGroup != null) {
			this.workerGroup.shutdownGracefully();
			workerGroup = null;
		}
	}  
	
	

    public EventLoopGroup getBossGroup() {
		return bossGroup;
	}

	public void setBossGroup(EventLoopGroup bossGroup) {
		this.bossGroup = bossGroup;
	}

	public EventLoopGroup getWorkerGroup() {
		return workerGroup;
	}

	public void setWorkerGroup(EventLoopGroup workerGroup) {
		this.workerGroup = workerGroup;
	}

	public String getLocalHost() {
		return localHost;
	}

	public void setLocalHost(String localHost) {
		this.localHost = localHost;
	}

	public int getLocalPort() {
		return localPort;
	}

	public void setLocalPort(int localPort) {
		this.localPort = localPort;
	}

	public String getRemoteHost() {
		return remoteHost;
	}

	public void setRemoteHost(String remoteHost) {
		this.remoteHost = remoteHost;
	}

	public int getRemotePort() {
		return remotePort;
	}

	public void setRemotePort(int remotePort) {
		this.remotePort = remotePort;
	}

	public boolean isLogEnabled() {
		return logEnabled;
	}

	public void setLogEnabled(boolean logEnabled) {
		this.logEnabled = logEnabled;
	}

	public static void main(String[] args) throws Exception { 
    	String local = ConfigKit.option(args, "-local", "0.0.0.0:3307");
    	String remote = ConfigKit.option(args, "-remote", "localhost:3306"); 
    	TcpProxy proxy = new TcpProxy(local, remote);
    	proxy.setLogEnabled(true);
    	
    	Channel channel = proxy.start(); 
    	channel.closeFuture().sync();
    	proxy.close();
    }
}
