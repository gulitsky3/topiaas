package io.zbus.rpc;

import java.io.Closeable;
import java.io.IOException;

import io.netty.handler.ssl.SslContext;
import io.zbus.rpc.server.HttpRpcServerAdaptor;
import io.zbus.rpc.server.MqRpcServer;
import io.zbus.transport.IoAdaptor;
import io.zbus.transport.Ssl;
import io.zbus.transport.http.HttpWsServer; 
 

public class RpcServer implements Closeable {   
	private RpcProcessor processor;   
	private RpcStartInterceptor onStart;
	
	//RPC over HTTP/WS
	private Integer port;
	private String host = "0.0.0.0"; 
	private String certFile;
	private String keyFile;
	
	private HttpWsServer httpWsServer; 
	private HttpRpcServerAdaptor httpRpcServerAdaptor; 
	
	//RPC over MQ
	private String mq;
	private String mqType;
	private String channel;         //Default to MQ
	private String mqServerAddress; //Support MQ based RPC
	private Integer mqClientCount;
	private Integer mqHeartbeatInterval;
	private MqRpcServer mqRpcServer;
	
	public RpcServer() {
		this.processor = new RpcProcessor();
	}
	public RpcServer(RpcProcessor processor) {
		this.processor = processor;
	}
	
	public RpcServer setProcessor(RpcProcessor processor) {
		this.processor = processor;
		return this;
	}
	
	public RpcProcessor getProcessor() {
		return this.processor;
	}
	
	public RpcServer setPort(Integer port){ 
		this.port = port;
		return this;
	} 
	 
	public RpcServer setHost(String host){ 
		this.host = host;
		return this;
	}    
	
	public RpcServer setMq(String mq){ 
		this.mq = mq;
		return this;
	}    
	
	public RpcServer setMqType(String mqType){ 
		this.mqType = mqType;
		return this;
	}    
	
	public RpcServer setMqClientCount(Integer count){ 
		this.mqClientCount = count;
		return this;
	}    
	
	public RpcServer setMqHeartbeatInterval(Integer mqHeartbeatInterval) {
		this.mqHeartbeatInterval = mqHeartbeatInterval;
		return this;
	}
	
	public RpcServer setChannel(String channel) {
		this.channel = channel;
		return this;
	}
	
	public RpcServer setAddress(String address){ 
		this.mqServerAddress = address;
		return this;
	} 
	
	public RpcServer setCertFile(String certFile){ 
		this.certFile = certFile; 
		return this;
	}  
	
	public RpcServer setKeyFile(String keyFile){ 
		this.keyFile = keyFile;
		return this;
	}  
	   
	public IoAdaptor getHttpRpcServerAdaptor() { 
		return httpRpcServerAdaptor;
	}

	public void setMqServerAddress(String mqServerAddress) {
		this.mqServerAddress = mqServerAddress;
	}

	public void setOnStart(RpcStartInterceptor onStart) {
		this.onStart = onStart;
	} 
	 
	public RpcServer start() throws Exception{ 
		if(this.processor == null) {
			this.processor = new RpcProcessor();
		}
		if(onStart != null) {
			onStart.onStart(processor);
		} 
		
		if(port != null) {
			this.httpWsServer = new HttpWsServer(); 
			this.httpRpcServerAdaptor = new HttpRpcServerAdaptor(processor);
			SslContext context = null;
			if(keyFile != null && certFile != null) {
				context = Ssl.buildServerSsl(certFile, keyFile); 
			}   
			httpWsServer.start(String.format("%s:%d",this.host, this.port), httpRpcServerAdaptor, context); 
		} 
		
		if(mqServerAddress != null && mq != null) { 
			this.processor.setDocUrlPrefix("/"+this.mq);
			
			this.mqRpcServer = new MqRpcServer(this.processor);
			mqRpcServer.setAddress(mqServerAddress);
			mqRpcServer.setMq(this.mq);
			if(this.mqType != null) {
				mqRpcServer.setMqType(mqType);
			}
			if(this.channel != null) {
				mqRpcServer.setChannel(this.channel);
			}
			if(this.mqClientCount != null) {
				mqRpcServer.setClientCount(mqClientCount);
			}
			if(this.mqHeartbeatInterval != null) {
				mqRpcServer.setHeartbeatInterval(mqHeartbeatInterval);
			}
			
			mqRpcServer.start();
		} 
		
		//Doc URL root generated
		if(processor.isDocEnabled()) {
			processor.enableDoc();
		}
		
		return this;
	}     
	
	@Override
	public void close() throws IOException {  
		if(httpWsServer != null) {
			httpWsServer.close();
			httpWsServer = null;
		} 
		if(mqRpcServer != null) {
			mqRpcServer.close();
			mqRpcServer = null;
		}
	}   
}
