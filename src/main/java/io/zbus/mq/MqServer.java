package io.zbus.mq;
 

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import io.zbus.kit.ConfigKit;
import io.zbus.transport.Ssl;
import io.zbus.transport.http.HttpWsServer; 

public class MqServer extends HttpWsServer {
	private static final Logger logger = LoggerFactory.getLogger(MqServer.class); 
	private MqServerAdaptor serverAdaptor; 
	private final MqServerConfig config; 
	
	public MqServer(MqServerConfig config) { 
		
		this.config = config;
		this.maxSocketCount = config.maxSocketCount; 
		serverAdaptor = new MqServerAdaptor(this.config);
		if(config.requestAuth != null) {
			serverAdaptor.setRequestAuth(config.requestAuth);
		}
		
		boolean sslEnabled = config.isSslEnabled(); 
		if (sslEnabled){  
			try{  
				this.sslContext = Ssl.buildServerSsl(config.getSslCertFile(), config.getSslKeyFile()); 
			} catch (Exception e) { 
				logger.error("SSL init error: " + e.getMessage());
				throw new IllegalStateException(e.getMessage(), e.getCause());
			} 
		}
	} 
	public MqServer(String configFile){
		this(new MqServerConfig(configFile));
	}
	
	public MqServerAdaptor getServerAdaptor() {
		return serverAdaptor;
	}
	
	public void start() {
		if(config.port == null) {
			logger.info("Networking disabled, zbus work as InProc mode");
			return;
		}
		this.start(config.port, serverAdaptor);
	}
	 
	
	public static void main(String[] args) {
		String configFile = ConfigKit.option(args, "-conf", "conf/zbus.xml"); 
		
		final MqServer server;
		try{
			server = new MqServer(configFile);
			server.start(); 
		} catch (Exception e) { 
			e.printStackTrace(System.err);
			logger.warn(e.getMessage(), e); 
			return;
		} 
		
		Runtime.getRuntime().addShutdownHook(new Thread(){ 
			public void run() { 
				try { 
					server.close();
					logger.info("MqServer shutdown completed");
				} catch (Exception e) {
					logger.error(e.getMessage(), e);
				}
			}
		});    
	}
}
