package io.zbus.rpc.bootstrap;

import java.io.Closeable;
import java.io.IOException;

import io.zbus.rpc.RpcConfig;
import io.zbus.rpc.RpcInvoker;
import io.zbus.rpc.transport.http.RpcHttpInvoker;
import io.zbus.transport.ServerAddress;
import io.zbus.transport.http.MessageClientPool;

public class HttpClientBootstrap implements Closeable {   
	protected MessageClientPool clientPool;
	protected String token;
	protected ServerAddress serverAddress;
	protected int clientPoolSize = 32; 
	protected RpcConfig rpcConfig = new RpcConfig(); 
	 
	public HttpClientBootstrap serviceToken(String token){  
		this.token = token;
		return this;
	}  
	
	public HttpClientBootstrap serviceAddress(ServerAddress serverAddress){
		this.serverAddress = serverAddress;
		return this;
	}
	
	public HttpClientBootstrap serviceAddress(String serverAddress){
		this.serverAddress = new ServerAddress(serverAddress);
		return this;
	}
	
	public HttpClientBootstrap clientPoolSize(int clientPoolSize){
		this.clientPoolSize = clientPoolSize;
		return this;
	}
	
	public RpcInvoker invoker(){
		if(clientPool == null){
			 clientPool = new MessageClientPool(serverAddress, clientPoolSize, null);
		} 
		RpcHttpInvoker messageInvoker = new RpcHttpInvoker(clientPool);
		messageInvoker.setToken(this.token);
		rpcConfig.setMessageInvoker(messageInvoker);
		return new RpcInvoker(rpcConfig); 
	}
	
	public <T> T createProxy(Class<T> clazz){  
		return invoker().createProxy(clazz); 
	}     
	
	@Override
	public void close() throws IOException { 
		if(clientPool != null){
			clientPool.close();
			clientPool = null;
		}
	} 
}
