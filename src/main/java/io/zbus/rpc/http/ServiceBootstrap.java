package io.zbus.rpc.http;

import java.io.Closeable;
import java.io.IOException;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;

import io.netty.handler.ssl.SslContext;
import io.zbus.kit.ClassKit;
import io.zbus.rpc.GenericInvocation;
import io.zbus.rpc.RegisterInterceptor;
import io.zbus.rpc.RpcFilter;
import io.zbus.rpc.RpcMethod;
import io.zbus.rpc.RpcProcessor;
import io.zbus.rpc.annotation.Remote;
import io.zbus.transport.Ssl;
import io.zbus.transport.http.HttpWsServer; 
 

public class ServiceBootstrap implements Closeable {  
	private RpcProcessor processor = new RpcProcessor(); 
	private boolean autoLoadService = false;
	private int port;
	private String host = "0.0.0.0";
	private String certFile;
	private String keyFile;
	private HttpWsServer server;    
	private RegisterInterceptor onStart;
	
	public ServiceBootstrap setPort(int port){ 
		this.port = port;
		return this;
	} 
	 
	public ServiceBootstrap setHost(String host){ 
		this.host = host;
		return this;
	}    
	
	public ServiceBootstrap setCertFile(String certFile){ 
		this.certFile = certFile; 
		return this;
	}  
	
	public ServiceBootstrap setKeyFile(String keyFile){ 
		this.keyFile = keyFile;
		return this;
	}  
	 
	public ServiceBootstrap setAutoLoadService(boolean autoLoadService) {
		this.autoLoadService = autoLoadService;
		return this;
	}  
	
	public ServiceBootstrap setStackTraceEnabled(boolean stackTraceEnabled) {
		this.processor.setStackTraceEnabled(stackTraceEnabled);
		return this;
	} 
	
	public ServiceBootstrap setMethodPageEnabled(boolean methodPageEnabled) {
		this.processor.setMethodPageEnabled(methodPageEnabled);
		return this;
	}  
	
	public ServiceBootstrap setMethodPageAuthEnabled(boolean methodPageAuthEnabled) {
		this.processor.setMethodPageAuthEnabled(methodPageAuthEnabled);
		return this;
	}
	
	public ServiceBootstrap setMethodPageModule(String monitorModuleName) {
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
	
	public void setOnStart(RegisterInterceptor onStart) {
		this.onStart = onStart;
	}

	private void validate(){ 
		
	}
	
	protected void initProcessor(){  
		Set<Class<?>> classes = ClassKit.scan(Remote.class);
		for(Class<?> clazz : classes){
			processor.addModule(clazz);
		}   
	}
	
	public RpcProcessor processor() {
		return this.processor;
	}
	 
	public ServiceBootstrap start() throws Exception{
		validate();  
		
		if(autoLoadService){
			initProcessor();
		} 
		if(processor.isMethodPageEnabled()) {
			processor.enableMethodPageModule();
		}
		
		if(onStart != null) {
			onStart.onStart(processor);
		}
		
		server = new HttpWsServer();    
		if(keyFile != null && certFile != null) {
			SslContext context = Ssl.buildServerSsl(certFile, keyFile);
			server.setSslContext(context);
		}  
		
		RpcServerAdaptor adaptor = new RpcServerAdaptor(this.processor); 
		server.start(this.host, this.port, adaptor); 
		
		return this;
	}  
	
	public ServiceBootstrap addModule(Class<?>... clazz){
		processor.addModule(clazz);
		return this;
	}  
	
	public ServiceBootstrap addModule(String module, Class<?>... clazz){
		processor.addModule(module, clazz);
		return this;
	}
	
	public ServiceBootstrap addModule(String module, Object... services){
		processor.addModule(module, services);
		return this;
	}
	
	public ServiceBootstrap addModule(Object... services){
		processor.addModule(services);
		return this;
	}
	
	public void setModuleTable(Map<String, Object> instances){
		if(instances == null) return;
		for(Entry<String, Object> e : instances.entrySet()){
			processor.addModule(e.getKey(), e.getValue());
		}
	}
	
	public ServiceBootstrap addMethod(RpcMethod spec, GenericInvocation genericInvocation){
		processor.addMethod(spec, genericInvocation);
		return this;
	}  
	
	@Override
	public void close() throws IOException {  
		if(server != null) {
			server.close();
		} 
	}   
}
