package io.zbus.mq;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

import io.zbus.auth.AuthResult;
import io.zbus.auth.RequestAuth;
import io.zbus.kit.FileKit;
import io.zbus.kit.StrKit;
import io.zbus.mq.Protocol.MqInfo;
import io.zbus.mq.commands.MsgKit;
import io.zbus.mq.model.MessageQueue;
import io.zbus.rpc.RpcException;
import io.zbus.rpc.RpcProcessor;
import io.zbus.rpc.StaticResource;
import io.zbus.rpc.annotation.RequestMapping;
import io.zbus.transport.Message;
import io.zbus.transport.ServerAdaptor;
import io.zbus.transport.Session;
import io.zbus.transport.http.Http;

public class MonitorServerAdaptor extends ServerAdaptor { 
	private RpcProcessor rpcProcessor = new RpcProcessor();
	private MessageQueueManager mqManager;
	protected SubscriptionManager subManager;
	
	private RequestAuth requestAuth;  
	
	public MonitorServerAdaptor(MqServerConfig config, MessageQueueManager mqManager, SubscriptionManager subManager) {
		if (config.monitorServer != null && config.monitorServer.auth != null) {
			requestAuth = config.monitorServer.auth; 
		} 
		this.mqManager = mqManager;
		this.subManager = subManager; 
		
		StaticResource staticResource = new StaticResource();
		staticResource.setCacheEnabled(false); // TODO turn if off in production
		
		rpcProcessor.mount("/", new MonitorService()); 
		rpcProcessor.mount("/static", staticResource, false);
		rpcProcessor.mountDoc(); 
	}

	protected void attachInfo(Message request, Session sess) { 
		request.setHeader(Protocol.REMOTE_ADDR, sess.remoteAddress());
		if(request.getHeader(Protocol.ID) == null) {
			request.setHeader(Protocol.ID, StrKit.uuid());
		}
	}
	
	public void onMessage(Object msg, Session sess) throws IOException {
		Message req = (Message) msg;   
		//check integrity 
		if(requestAuth != null) {
			AuthResult authResult = requestAuth.auth(req);
			if(!authResult.success) {
				MsgKit.reply(req, 403, authResult.message, sess); 
				return; 
			}
		}   
		
		attachInfo(req, sess);
		
		String cmd = req.getHeader(Protocol.CMD); 
		if(Protocol.PING.equals(cmd)) return; 
		
		if(rpcProcessor != null) {
			if(rpcProcessor.matchUrl(req.getUrl())) {
				Message res = new Message();
				rpcProcessor.process(req, res);
				sess.write(res); 
				return;
			} 
		} 
	}
	
	class MonitorService {
		private FileKit fileKit = new FileKit();  
		
		@RequestMapping(path = "/favicon.ico", docEnabled = false)
		public Message favicon() {
			return fileKit.loadResource("static/favicon.ico");
		}
		
		@RequestMapping("/")
		public Object home(Map<String, String> params) { 
			return query(params);
		} 
		
		public Object query(Map<String, String> params) { 
			if(params == null) {
				params = new HashMap<>(); 
			} 
			String mq = params.get(Protocol.MQ);
			String channel = params.get(Protocol.CHANNEL);
			if(mq == null) {
				return mqManager.mqInfoList();
			}
			MessageQueue q = mqManager.get(mq);
			if(channel == null) { 
				if(q == null) {
					return null;
				}
				return q.info();
			}
			return q.channel(channel);
		} 
		
		public MqInfo create(Map<String, String> params, Message ctx) throws IOException { 
			if(params == null) {
				throw new RpcException(400, "Missing query params");
			} 
			String mqName = params.get(Protocol.MQ);
			if(mqName == null) {
				throw new RpcException(400, "Missing mq in query"); 
			}
			
			String mqType = params.get(Protocol.MQ_TYPE);  
			Integer mqMask = StrKit.getInt(params, Protocol.MQ_MASK);
			String channel = params.get(Protocol.CHANNEL); 
			Integer channelMask = StrKit.getInt(params, Protocol.CHANNEL_MASK);
			Long offset = StrKit.getLong(params, Protocol.OFFSET); 
			String creator = ctx.getHeader(Protocol.REMOTE_ADDR);
			
			MessageQueue mq = mqManager.saveQueue(mqName, mqType, mqMask, channel, offset, channelMask, creator); 
			return mq.info();
		} 
		
		public Message remove(Map<String, String> params) throws IOException { 
			if(params == null) {
				throw new RpcException(400, "Missing query params");
			} 
			String mqName = params.get(Protocol.MQ);
			if(mqName == null) {
				throw new RpcException(400, "Missing mq in query"); 
			} 
			String channel = params.get(Protocol.CHANNEL);  
			
			mqManager.removeQueue(mqName, channel);
			
			Message res = new Message();
			res.setStatus(200);
			res.setHeader(Http.CONTENT_TYPE, "text/plain; charset=utf8");
			String msg = String.format("OK, REMOVE (mq=%s,channel=%s)", mqName, channel); 
			if(channel == null) {
				msg = String.format("OK, REMOVE (mq=%s)", mqName); 
			}
			res.setBody(msg);
			return res;
		} 
	} 
}

