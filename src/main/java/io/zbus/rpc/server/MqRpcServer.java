package io.zbus.rpc.server;

import java.io.Closeable;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import io.zbus.kit.JsonKit;
import io.zbus.mq.MqClient;
import io.zbus.mq.MqServer;
import io.zbus.mq.Protocol;
import io.zbus.rpc.RpcProcessor;
import io.zbus.transport.http.HttpMessage;

public class MqRpcServer implements Closeable {
	private static final Logger logger = LoggerFactory.getLogger(MqRpcServer.class);

	private MqServer mqServer;
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

		mqClient.addListener(mq, channel, request -> {
			String sender = (String)request.get(Protocol.SENDER);
			String id = (String)request.get(Protocol.ID); 
			
			Map<String, Object> response = processor.process(request);
			
			Object body = response.get(Protocol.BODY);
			if (body != null && body instanceof HttpMessage) { //Special case when body is HTTP Message, make it browser friendly
				HttpMessage res = (HttpMessage)body;
				if(res.getStatus() == null) {
					res.setStatus(200);
				}
				res.setHeader(Protocol.ID, id);
				response.put(Protocol.BODY_HTTP, true);
				response.put(Protocol.BODY, new String(res.toBytes())); //TODO support binary data
			}
			
			if (response.get(Protocol.STATUS) == null) {
				response.put(Protocol.STATUS, 200);
			}
			response.put(Protocol.CMD, Protocol.ROUTE);
			response.put(Protocol.RECVER, sender);
			response.put(Protocol.ID, id);

			mqClient.sendMessage(response);
		});

		mqClient.onOpen(() -> {
			Map<String, Object> req = new HashMap<>();
			req.put(Protocol.CMD, Protocol.CREATE); // create MQ/Channel
			req.put(Protocol.MQ, mq);
			req.put(Protocol.MQ_TYPE, mqType);
			req.put(Protocol.CHANNEL, channel);

			Map<String, Object> res = mqClient.invoke(req);
			logger.info(JsonKit.toJSONString(res));

			Map<String, Object> sub = new HashMap<>();
			sub.put(Protocol.CMD, Protocol.SUB); // Subscribe on MQ/Channel
			sub.put(Protocol.MQ, mq);
			sub.put(Protocol.CHANNEL, channel);
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
