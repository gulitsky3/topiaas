package io.zbus.rpc;

import io.zbus.mq.MqServer;
import io.zbus.mq.MqServerConfig;

public class RpcServerBuilder {   
	/**
	 * Embed RPC server inside of MqServer, the fastest integration
	 * @return
	 */
	public static RpcServer embedded() {
		//Serve RPC embedded in MqServer  
		
		MqServerConfig config = new MqServerConfig();
		config.setPublicServer("0.0.0.0:15555");
		config.setMonitorServer("0.0.0.0:25555");
		MqServer mqServer = new MqServer(config);  
		mqServer.setVerbose(false);
		
		RpcServer rpcServer = new RpcServer(); 
		rpcServer.setMqServer(mqServer); //embedded in MqServer
		
		return rpcServer; 
	} 

	
	/**
	 * Key differs to Embedded: RPC is route through MQ
	 * @return
	 */
	public static RpcServer inProc() {
		//Serve RPC via MQ Server InProc 
		MqServer mqServer = new MqServer(15555);   
		
		RpcServer server = new RpcServer();   
		server.setMqServer(mqServer); //InProc MqServer
		server.setMq("/");            //Choose MQ to group Service physically
		 
		//server.setAuthEnabled(true);
		//server.setApiKey("2ba912a8-4a8d-49d2-1a22-198fd285cb06");
		//server.setSecretKey("461277322-943d-4b2f-b9b6-3f860d746ffd"); 
		
		return server; 
	}
	
	/**
	 * Distributed mount to remote MqServer
	 * 
	 * @return
	 */
	public static RpcServer remoteMq() {
		RpcServer server = new RpcServer();       
		server.setMqServerAddress("localhost:15555"); 
		server.setMq("/");
		
		//server.setAuthEnabled(true);
		//server.setApiKey("2ba912a8-4a8d-49d2-1a22-198fd285cb06");
		//server.setSecretKey("461277322-943d-4b2f-b9b6-3f860d746ffd"); 
		
		return server; 
	}
}
