package io.zbus.rpc;

import java.io.Closeable;
import java.io.IOException;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import io.netty.handler.ssl.SslContext;
import io.zbus.rpc.server.HttpRpcServer;
import io.zbus.rpc.server.MqRpcServer;
import io.zbus.transport.IoAdaptor;
import io.zbus.transport.Ssl; 
 

public class RpcServer implements Closeable {  
	private static final Logger log = LoggerFactory.getLogger(RpcServer.class);
	
	private RpcProcessor processor;   
	private RpcStartInterceptor onStart;
	
	//RPC over HTTP/WS
	private Integer port;
	private String host = "0.0.0.0"; 
	private String certFile;
	private String keyFile;
	private HttpRpcServer httpRpcServer; 
	
	
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
	  
	public RpcServer setStackTraceEnabled(boolean stackTraceEnabled) {
		this.processor.setStackTraceEnabled(stackTraceEnabled);
		return this;
	} 
	
	public RpcServer setMethodPageEnabled(boolean methodPageEnabled) {
		this.processor.setMethodPageEnabled(methodPageEnabled);
		return this;
	}  
	
	public RpcServer setMethodPageAuthEnabled(boolean methodPageAuthEnabled) {
		this.processor.setMethodPageAuthEnabled(methodPageAuthEnabled);
		return this;
	}
	
	public RpcServer setMethodPageModule(String monitorModuleName) {
		this.processor.setMethodPageModule(monitorModuleName);
		return this;
	}  
	
	
	public void setBeforeFilter(RpcFilter beforeFilter) {
		this.processor.setBeforeFilter(beforeFilter);
	}

	public void setAfterFilter(RpcFilter afterFilter) {
		this.processor.setAfterFilter(afterFilter);
	}

	public void setAuthFilter(RpcFilter authFilter) {
		this.processor.setAuthFilter(authFilter);
	} 
	
	public IoAdaptor getHttpRpcServerAdaptor() {
		if(httpRpcServer == null) return null;
		return httpRpcServer.getHttpRpcAdaptor();
	}

	public void setMqServerAddress(String mqServerAddress) {
		this.mqServerAddress = mqServerAddress;
	}

	public void setOnStart(RpcStartInterceptor onStart) {
		this.onStart = onStart;
	}

	private void validate(){ 
		
	} 
	
	public RpcProcessor processor() {
		return this.processor;
	}
	 
	public RpcServer start() throws Exception{
		validate();   
		
		if(onStart != null) {
			onStart.onStart(processor);
		} 
		
		if(port != null) {
			this.httpRpcServer = new HttpRpcServer(this.processor); 
			if(keyFile != null && certFile != null) {
				SslContext context = Ssl.buildServerSsl(certFile, keyFile);
				httpRpcServer.setSslContext(context);
			}  
			 
			httpRpcServer.start(this.host, this.port); 
		} 
		
		if(mqServerAddress != null && mq != null) { 
			this.processor.setDocUrlRoot(this.mq+"/");
			
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
		if(processor.isMethodPageEnabled()) {
			processor.enableMethodPageModule();
		}
		
		return this;
	}   
	  
	
	public RpcServer addModule(String module, Class<?> service){
		try {
			Object obj = service.newInstance();
			processor.addModule(module, obj);
		} catch (InstantiationException | IllegalAccessException e) {
			log.error(e.getMessage(),e);
		} 
		return this;
	}
	
	public RpcServer addModule(String module, Object service){
		processor.addModule(module, service);
		return this;
	}
	 
	
	public RpcServer addModule(String module, List<Object> services){
		for(Object svc : services) {
			processor.addModule(module, svc);
		}
		return this;
	}  
	
	
	@SuppressWarnings("unchecked")
	public void setModuleTable(Map<String, Object> instances){
		if(instances == null) return;
		for(Entry<String, Object> e : instances.entrySet()){
			Object svc = e.getValue();
			if(svc instanceof List) {
				addModule(e.getKey(), (List<Object>)svc); 
			} else {
				addModule(e.getKey(), svc);
			}
		}
	}
	
	public RpcServer addMethod(RpcMethod spec, MethodInvoker genericInvocation){
		processor.addMethod(spec, genericInvocation);
		return this;
	}  
	
	public RpcServer removeMethod(String path){
		processor.removeMethod(path);
		return this;
	}  
	
	@Override
	public void close() throws IOException {  
		if(httpRpcServer != null) {
			httpRpcServer.close();
			httpRpcServer = null;
		} 
		if(mqRpcServer != null) {
			mqRpcServer.close();
			mqRpcServer = null;
		}
	}   
}
