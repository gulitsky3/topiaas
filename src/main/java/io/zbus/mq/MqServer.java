package io.zbus.mq;
 

import java.util.ArrayList;
import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import io.netty.handler.codec.http.HttpMethod;
import io.netty.handler.codec.http.cors.CorsConfigBuilder;
import io.netty.handler.ssl.SslContext;
import io.zbus.kit.ConfigKit;
import io.zbus.kit.StrKit;
import io.zbus.mq.MqServerConfig.CorsConfig;
import io.zbus.mq.MqServerConfig.ServerConfig;
import io.zbus.proxy.http.HttpDecodeFilter;
import io.zbus.proxy.http.HttpProxyHandler;
import io.zbus.proxy.http.ProxyUrlMatcher;
import io.zbus.rpc.RpcProcessor;
import io.zbus.rpc.StaticResource;
import io.zbus.transport.Ssl;
import io.zbus.transport.http.DecodeFilter;
import io.zbus.transport.http.HttpWsServer; 

public class MqServer extends HttpWsServer {
	private static final Logger logger = LoggerFactory.getLogger(MqServer.class); 
	
	private ServerConfig publicServerConfig; 
	private ServerConfig privateServerConfig;
	private ServerConfig monitorServerConfig;

	private MqServerAdaptor publicServerAdaptor; 
	private MqServerAdaptor privateServerAdaptor; 
	private MonitorServerAdaptor monitorServerAdaptor;
	
	private final MqServerConfig config; 
	
	private StaticResource staticResource = new StaticResource();
	private RpcProcessor rpcProcessor; 
	
	public MqServer(MqServerConfig config) {  
		super(corsConfig(config.getCors()));
		
		this.config = config;
		this.maxSocketCount = config.maxSocketCount;
		
		publicServerConfig = config.publicServer;
		if(publicServerConfig != null) {
			publicServerAdaptor = new MqServerAdaptor(this.config);
			if(publicServerConfig.auth != null) {
				publicServerAdaptor.setRequestAuth(publicServerConfig.auth);
			}
			
			publicServerAdaptor.onInit(); 
		}
		
		privateServerConfig = config.privateServer;
		if(privateServerConfig != null) {
			if(publicServerAdaptor != null) {
				privateServerAdaptor = publicServerAdaptor.clone(); //share internal state
			} else {
				privateServerAdaptor = new MqServerAdaptor(this.config);
			} 
			privateServerAdaptor.setRequestAuth(null);//clear auth by default
			if(privateServerConfig.auth != null) {
				privateServerAdaptor.setRequestAuth(privateServerConfig.auth);
			}
			
			privateServerAdaptor.onInit();
		}  
		
		MqServerAdaptor adaptor = this.publicServerAdaptor;
		if(adaptor == null) {
			adaptor = this.privateServerAdaptor;
		}
		if(adaptor == null) {
			adaptor = publicServerAdaptor = new MqServerAdaptor(this.config); 
		}
		
		monitorServerConfig = config.monitorServer;
		if(monitorServerConfig != null) {
			monitorServerAdaptor = new MonitorServerAdaptor(adaptor); 
		}
		staticResource.setBasePath(config.getStaticFileDir());
		staticResource.setCacheEnabled(config.isStaticFileCacheEnabled());
		
		//Setup HTTP proxy
		if(config.httpProxyConfig != null) {  
			ProxyUrlMatcher urlMatcher = new ProxyUrlMatcher(config.httpProxyConfig.buildProxyTable());
			DecodeFilter decodeFilter = new HttpDecodeFilter(urlMatcher);
			setDecodeFilter(decodeFilter);
			
			HttpProxyHandler httpProxyHandler = new HttpProxyHandler(urlMatcher);
			adaptor.setHttpProxyHandler(httpProxyHandler);
		}
	}  
	
	public MqServer(String configFile){
		this(new MqServerConfig(configFile));
	} 
	
	public MqServer(int port) {
		this(new MqServerConfig("0.0.0.0", port));
	}
	
	public MqServer() {
		this(new MqServerConfig());
	}
	
	protected static io.netty.handler.codec.http.cors.CorsConfig corsConfig(CorsConfig cfg){
		if(cfg == null) return null;
		CorsConfigBuilder builder;
		if(cfg.origin.equals("*")) {
			builder = CorsConfigBuilder.forAnyOrigin();
		} else {
			builder = CorsConfigBuilder.forOrigins(StrKit.split(cfg.origin));
		} 
		
		String[] methods = StrKit.split(cfg.allowedRequestMethods);
		List<HttpMethod> httpMethods = new ArrayList<>();
		for(String method : methods) {
			httpMethods.add(HttpMethod.valueOf(method));
		}
		builder.allowedRequestMethods(httpMethods.toArray(new HttpMethod[0]));
		builder.allowedRequestHeaders(StrKit.split(cfg.allowedRequestHeaders));
		builder.exposeHeaders(StrKit.split(cfg.exposeHeaders));
		
		builder.allowCredentials()
				.allowNullOrigin();
		return builder.build();   
	} 
	
	public RpcProcessor getRpcProcessor() {
		return rpcProcessor;
	}
	
	public void setRpcProcessor(RpcProcessor rpcProcessor) {
		this.rpcProcessor = rpcProcessor;
		if(this.publicServerAdaptor != null) {
			this.publicServerAdaptor.setRpcProcessor(rpcProcessor);
		}
		if(this.privateServerAdaptor != null) {
			this.privateServerAdaptor.setRpcProcessor(rpcProcessor);
		}
		this.rpcProcessor.mountDoc(); //TODO remove mountDoc
	}
	
	public MqServerAdaptor getServerAdaptor() {
		return publicServerAdaptor;
	}
	
	public void setVerbose(boolean value) {
		this.config.setVerbose(value);
	}
	
	public void start() {
		if(publicServerAdaptor != null && publicServerConfig != null) { 
			SslContext sslContext = null;
			if (publicServerConfig.sslEnabled){  
				try{  
					sslContext = Ssl.buildServerSsl(publicServerConfig.sslCertFile, publicServerConfig.sslKeyFile); 
				} catch (Exception e) { 
					logger.error("SSL init error: " + e.getMessage());
					throw new IllegalStateException(e.getMessage(), e.getCause());
				} 
			}
			logger.info("Starting public server @" + publicServerConfig.address);
			this.start(publicServerConfig.address, publicServerAdaptor, sslContext); 
		} 
		
		if(privateServerAdaptor != null && privateServerConfig != null) { 
			SslContext sslContext = null;
			if (privateServerConfig.sslEnabled){  
				try{  
					sslContext = Ssl.buildServerSsl(privateServerConfig.sslCertFile, privateServerConfig.sslKeyFile); 
				} catch (Exception e) { 
					logger.error("SSL init error: " + e.getMessage());
					throw new IllegalStateException(e.getMessage(), e.getCause());
				} 
			}
			logger.info("Starting private server @" + privateServerConfig.address);
			this.start(privateServerConfig.address, privateServerAdaptor, sslContext); 
		}  
		
		if(monitorServerAdaptor != null & monitorServerConfig != null) {
			SslContext sslContext = null;
			if (monitorServerConfig.sslEnabled){  
				try{  
					sslContext = Ssl.buildServerSsl(monitorServerConfig.sslCertFile, monitorServerConfig.sslKeyFile); 
				} catch (Exception e) { 
					logger.error("SSL init error: " + e.getMessage());
					throw new IllegalStateException(e.getMessage(), e.getCause());
				} 
			}
			logger.info("Starting mointor server @" + monitorServerConfig.address);
			this.start(monitorServerConfig.address, monitorServerAdaptor, sslContext); 
		}   
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