package io.zbus.rpc.server;

import java.io.Closeable;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.TimeUnit;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import io.zbus.kit.JsonKit;
import io.zbus.mq.MqClient;
import io.zbus.mq.MqServer;
import io.zbus.mq.Protocol;
import io.zbus.rpc.RpcProcessor;
import io.zbus.transport.Message;

public class MqRpcServer implements Closeable {
	private static final Logger logger = LoggerFactory.getLogger(MqRpcServer.class);

	private MqServer mqServer; //Only for InprocClient
	private String address;
	private String mq;
	private String mqType = Protocol.MEMORY;
	private String channel;
	private int clientCount = 1;
	private int heartbeatInterval = 30; // seconds

	private List<MqClient> clients = new ArrayList<>();
	private RpcProcessor processor;

	public MqRpcServer(RpcProcessor processor) {
		this.processor = processor;
	}

	@Override
	public void close() throws IOException {
		for(MqClient client : clients) {
			client.close();
		} 
	}
	
	public void start() {
		for(int i=0;i<clientCount;i++) {
			MqClient client = startClient();
			clients.add(client);
		}
	}

	protected MqClient startClient() {
		MqClient client = null;
		if (mqServer != null) {
			client = new MqClient(mqServer);
		} else if (address != null) {
			client = new MqClient(address);
		} else {
			throw new IllegalStateException("Can not create MqClient, missing address or mqServer?");
		}
		
		if (this.channel == null) this.channel = this.mq;  
		
		final MqClient mqClient = client;
		mqClient.heartbeat(heartbeatInterval, TimeUnit.SECONDS);

		mqClient.addMqHandler(mq, channel, request -> {
			String source = (String)request.getHeader(Protocol.SOURCE);
			String id = (String)request.getHeader(Protocol.ID); 
			
			String url = request.getUrl();
			if(request.getUrl() != null) {
				String prefix = "/"+mq;
				if(url.startsWith(prefix)) {
					url = url.substring(prefix.length());
					if(!url.startsWith("/")) url = "/"+url;
					request.setUrl(url);
				}
			}
			
			Message response = processor.process(request);
			
			if(response.getStatus() == null) {
				response.setStatus(200);
			}
			
			response.addHeader(Protocol.CMD, Protocol.ROUTE);
			response.addHeader(Protocol.TARGET, source);
			response.addHeader(Protocol.ID, id);

			mqClient.sendMessage(response);
		});

		mqClient.onOpen(() -> {
			Message req = new Message();
			req.addHeader(Protocol.CMD, Protocol.CREATE); // create MQ/Channel
			req.addHeader(Protocol.MQ, mq);
			req.addHeader(Protocol.MQ_TYPE, mqType);
			req.addHeader(Protocol.CHANNEL, channel);

			Message res = mqClient.invoke(req);
			logger.info(JsonKit.toJSONString(res));

			Message sub = new Message();
			sub.addHeader(Protocol.CMD, Protocol.SUB); // Subscribe on MQ/Channel
			sub.addHeader(Protocol.MQ, mq);
			sub.addHeader(Protocol.CHANNEL, channel);
			mqClient.invoke(sub, data -> {
				logger.info(JsonKit.toJSONString(data));
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

	public String getAddress() {
		return address;
	}

	public void setAddress(String address) {
		this.address = address;
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
}
