package io.zbus.rpc;

import java.io.Closeable;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import io.zbus.kit.HttpKit;
import io.zbus.kit.JsonKit;
import io.zbus.mq.MqClient;
import io.zbus.mq.MqServer;
import io.zbus.mq.Protocol;
import io.zbus.transport.Message;

public class RpcServer implements Closeable {
	private static final Logger logger = LoggerFactory.getLogger(RpcServer.class);

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
	private RpcProcessor rpcProcessor;
	
	private ExecutorService runner;
	
	private boolean routeDisabled = false;
	private Boolean started = false;

	public RpcServer(RpcProcessor processor) {
		this.rpcProcessor = processor;
	}
	
	public RpcServer() {
		
	}
	
	public RpcProcessor getRpcProcessor() {
		return rpcProcessor;
	}
	
	public void setRpcProcessor(RpcProcessor processor) {
		this.rpcProcessor = processor;
	}

	@Override
	public void close() throws IOException {
		for(MqClient client : clients) {
			client.close();
		} 
		started = false;
	}

	public boolean isStarted() {
		return started;
	}

	public void start() {  
		synchronized(started) {
			if(started) return;
			started = true;
		}
		if(runner == null) {
			runner = Executors.newFixedThreadPool(poolSize);
		} 
		rpcProcessor.mount(); //make sure internal moduleTable mounted if no mount(module, object) called
		if(this.mq != null) {
			this.rpcProcessor.setRootUrl(HttpKit.joinPath("/", this.mq));
			this.rpcProcessor.mountDoc();
		}
		
		if(mqServer != null) {  
			if(!mqServer.isStarted()) {
				mqServer.start();
			} 
			//embedded in MqServer
			if(mq == null) {
				mqServer.setRpcProcessor(rpcProcessor); 
				return;
			}
		}   
		
		//Inproc or remote MqServer
		for(int i=0;i<clientCount;i++) {
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
					url = HttpKit.joinPath("/", url); 
					request.setUrl(url);
				}
			}
			
			runner.submit(()->{
				Message response = new Message(); 
				rpcProcessor.process(request, response);   
				if(response.getStatus() == null) {
					response.setStatus(200);
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
			req.setBody(rpcProcessor.rpcMethodList());
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
	
	public void setMqMask(Integer mqMask) {
		this.mqMask = mqMask;
	}
	
	public Integer getMqMask() {
		return mqMask;
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
}
