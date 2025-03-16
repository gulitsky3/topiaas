package org.zbus.net;

import java.io.Closeable;
import java.io.File;
import java.io.IOException;

import org.zbus.kit.log.Logger;
import org.zbus.kit.log.LoggerFactory;

import io.netty.channel.EventLoopGroup;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.handler.ssl.SslContext;
import io.netty.handler.ssl.SslContextBuilder;
import io.netty.handler.ssl.util.SelfSignedCertificate;

public class EventDriver implements Closeable {
	private static final Logger log = LoggerFactory.getLogger(EventDriver.class);

	private EventLoopGroup bossGroup;  
	private EventLoopGroup workerGroup;  
	private SslContext sslContext;

	private boolean ownBossGroup = true;
	private boolean ownWorkerGroup = true; 

	public EventDriver() {
		try {
			bossGroup = new NioEventLoopGroup();
			workerGroup = new NioEventLoopGroup();
		} catch (Exception e) {
			throw new IllegalStateException(e.getMessage(), e);
		}
	}
	
	public EventDriver(EventLoopGroup group){
		this.bossGroup = group;
		this.workerGroup = group;
		this.ownBossGroup = false;
		this.ownWorkerGroup = false;
	}

	public EventDriver(EventLoopGroup bossGroup, EventLoopGroup workerGroup) {
		this.bossGroup = bossGroup;
		this.workerGroup = workerGroup;
		this.ownBossGroup = false;
		this.ownWorkerGroup = false;
	}

	public EventLoopGroup getBossGroup() {
		return bossGroup;
	}

	public void setBossGroup(EventLoopGroup bossGroup) {
		if (this.bossGroup != null && ownBossGroup) {
			this.bossGroup.shutdownGracefully();
		}
		this.bossGroup = bossGroup;
	}

	public EventLoopGroup getWorkerGroup() {
		return workerGroup;
	}

	public void setWorkerGroup(EventLoopGroup workerGroup) {
		if (this.workerGroup != null && ownWorkerGroup) {
			this.workerGroup.shutdownGracefully();
		}
		this.workerGroup = workerGroup;
	}

	public EventLoopGroup getGroup() {
		// try bossGroup first
		if (bossGroup != null)
			return bossGroup;
		//then workerGroup
		return workerGroup;
	}

	public SslContext getSslContext() {
		return sslContext;
	}
	
	public boolean isSslEnabled() {
		return sslContext != null;
	} 

	public void setSslContext(SslContext sslContext) {
		log.info("SSL: Enabled");
		this.sslContext = sslContext;
	}

	public void setSslContext(File certFile, File privateKeyFile) { 
		try {
			SslContextBuilder builder = SslContextBuilder.forServer(certFile, privateKeyFile);
			this.sslContext = builder.build();
		} catch (Exception e) {
			throw new IllegalStateException(e.getMessage(), e);
		}
	}

	public void setSslContextOfSelfSigned() { 
		try {
			SelfSignedCertificate cert = new SelfSignedCertificate(); 
			File certFile = cert.certificate();
			File privateKeyFile = cert.privateKey();
			setSslContext(certFile, privateKeyFile);
		} catch (Exception e) {
			log.warn(e.getMessage(), e);
		}
	}

	@Override
	public void close() throws IOException {
		if (ownBossGroup && bossGroup != null) {
			bossGroup.shutdownGracefully(); 
			bossGroup = null;
		}
		if (ownWorkerGroup && workerGroup != null) {
			workerGroup.shutdownGracefully();
			workerGroup = null;
		}
	} 
}