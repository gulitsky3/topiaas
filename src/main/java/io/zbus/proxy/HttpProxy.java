package io.zbus.proxy;

import java.io.Closeable;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import io.netty.channel.EventLoopGroup;
import io.netty.channel.nio.NioEventLoopGroup;
import io.zbus.kit.HttpKit;
import io.zbus.kit.JsonKit;
import io.zbus.mq.MqClient;
import io.zbus.mq.MqServer;
import io.zbus.mq.Protocol;
import io.zbus.transport.Message;

public class HttpProxy implements Closeable {
	private static final Logger logger = LoggerFactory.getLogger(HttpProxy.class);

	private MqServer mqServer; //InProc or Embedded
	
	private String mqServerAddress;
	private String mq;
	private String mqType = Protocol.MEMORY;
	private Integer mqMask = Protocol.MASK_DELETE_ON_EXIT;
	private String channel;
	private boolean authEnabled = false;
	private String apiKey = "";
	private String secretKey = "";
	
	private int clientCount = 1;
	private int heartbeatInterval = 30; // seconds
	private int poolSize = 64;

	private List<MqClient> clients = new ArrayList<>();  
	private ExecutorService runner;
	
	private boolean routeDisabled = false;
	
	protected String targetAddress; 
	private String targetUrlPrefix;
	private int timeoutInSeconds = 10;
	private int maxProxyClient = 32;
	private BlockingQueue<HttpClient> proxyClients; 
	
	
	private EventLoopGroup group = new NioEventLoopGroup();
 
	public HttpProxy(String targetAddress) {
		this.targetAddress = targetAddress;  
	}
	
	@Override
	public void close() throws IOException {
		for(MqClient client : clients) {
			client.close();
		} 
		group.shutdownGracefully();
		group = null;
	}
	
	public void start() {  
		if(runner == null) {
			runner = Executors.newFixedThreadPool(poolSize);
		}   
		
		proxyClients = new ArrayBlockingQueue<>(maxProxyClient);
		
		for(int i=0;i<maxProxyClient;i++) {
			HttpClient client = new HttpClient(this.targetAddress, group);
			this.mq = client.getUrlPrefix();
			this.targetUrlPrefix = client.getUrlPrefix();
			proxyClients.add(client); 
		}
		
		for (int i = 0; i < clientCount; i++) {
			MqClient client = startClient();
			clients.add(client);
			
		}
	} 

	protected MqClient startClient() {
		MqClient client = null;
		if (mqServer != null) {
			client = new MqClient(mqServer);
		} else if (mqServerAddress != null) {
			client = new MqClient(mqServerAddress);
		} else {
			throw new IllegalStateException("Can not create MqClient, missing address or mqServer?");
		}
		
		if (this.channel == null) this.channel = this.mq;  
		
		if(this.authEnabled) {
			client.setAuthEnabled(this.authEnabled);
			client.setApiKey(apiKey);
			client.setSecretKey(secretKey);
		}
		final MqClient mqClient = client;
		mqClient.heartbeat(heartbeatInterval, TimeUnit.SECONDS);
		 
		final String urlPrefix = HttpKit.joinPath("/", this.mq);
		mqClient.addMqHandler(mq, channel, request -> {
			String source = (String)request.getHeader(Protocol.SOURCE);
			String id = (String)request.getHeader(Protocol.ID); 
			 
			String url = request.getUrl();
			if(url != null) { 
				if(url.startsWith(urlPrefix)) {
					url = url.substring(urlPrefix.length());
					url = HttpKit.joinPath(targetUrlPrefix, url); 
					request.setUrl(url);
				}
			}
			runner.submit(()->{
				Message response = new Message();   
				HttpClient proxyClient = null;
				try {
					boolean connectionOk = true;
					proxyClient = proxyClients.take(); 
					if(!proxyClient.isConnected()) {
						try {
							proxyClient.connect(); 
						} catch (Exception e) {
							connectionOk = false;
							response.setStatus(500);
							response.setBody(e);
						}
					} 
					
					try {
						if(connectionOk) { 
							response = proxyClient.request(request, timeoutInSeconds, TimeUnit.SECONDS);
						}
						//TODO
					} catch (Exception e) {
						response.setStatus(500);
						response.setBody(e);
						proxyClient.close(); //invalid close and put back
					} finally {
						proxyClients.offer(proxyClient);
					}  
				} catch (Exception e) {
					response.setStatus(500);
					response.setBody(e);
				} 
				
				if(!routeDisabled) {
					response.setHeader(Protocol.CMD, Protocol.ROUTE);
					response.setHeader(Protocol.TARGET, source);
					response.setHeader(Protocol.ID, id);
	
					mqClient.sendMessage(response); 
				}
			}); 
		});

		mqClient.onOpen(() -> {
			Message req = new Message();
			req.setHeader(Protocol.CMD, Protocol.CREATE); // create MQ/Channel
			req.setHeader(Protocol.MQ, mq);
			req.setHeader(Protocol.MQ_MASK, mqMask);
			req.setHeader(Protocol.MQ_TYPE, mqType);
			req.setHeader(Protocol.CHANNEL, channel); 
			mqClient.invoke(req, res -> { 
				logger.info(JsonKit.toJSONString(res));
				
				Message subMessage = new Message();
				subMessage.setHeader(Protocol.CMD, Protocol.SUB); // Subscribe on MQ/Channel
				subMessage.setHeader(Protocol.MQ, mq);
				subMessage.setHeader(Protocol.CHANNEL, channel); 
				mqClient.invoke(subMessage, data -> {
					logger.info(JsonKit.toJSONString(data)); 
				});  
			}); 
			
		});

		mqClient.connect();
		
		return mqClient;
	}

	public MqServer getMqServer() {
		return mqServer;
	}

	public void setMqServer(MqServer mqServer) {
		this.mqServer = mqServer;
	}

	public String getMqServerAddress() {
		return mqServerAddress;
	}

	public void setMqServerAddress(String address) {
		this.mqServerAddress = address;
	}

	public String getMq() {
		return mq;
	}

	public void setMq(String mq) {
		this.mq = mq;
	}

	public String getMqType() {
		return mqType;
	}

	public void setMqType(String mqType) {
		this.mqType = mqType;
	}

	public String getChannel() {
		return channel;
	}

	public void setChannel(String channel) {
		this.channel = channel;
	}

	public int getClientCount() {
		return clientCount;
	}

	public void setClientCount(int clientCount) {
		this.clientCount = clientCount;
	}

	public int getHeartbeatInterval() {
		return heartbeatInterval;
	}

	public void setHeartbeatInterval(int heartbeatInterval) {
		this.heartbeatInterval = heartbeatInterval;
	}

	public void setAuthEnabled(boolean authEnabled) {
		this.authEnabled = authEnabled;
	}
	
	public boolean isRouteDisabled() {
		return routeDisabled;
	}
	
	public void setRouteDisabled(boolean routeDisabled) {
		this.routeDisabled = routeDisabled;
	}

	public void setApiKey(String apiKey) {
		this.apiKey = apiKey;
	}

	public void setSecretKey(String secretKey) {
		this.secretKey = secretKey;
	}

	public int getPoolSize() {
		return poolSize;
	}

	public void setPoolSize(int poolSize) {
		this.poolSize = poolSize;
	}   
	
	@SuppressWarnings("resource")
	public static void main(String[] args) throws Exception {
		HttpProxy proxy = new HttpProxy("http://localhost:8080/nginx");
		proxy.setMqServerAddress("localhost:15555");
		proxy.start();
	}
}
