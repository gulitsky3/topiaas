package io.zbus.rpc.http;

import java.io.IOException;
import java.util.Map;

import io.zbus.kit.HttpKit;
import io.zbus.kit.JsonKit;
import io.zbus.rpc.Protocol;
import io.zbus.rpc.RpcProcessor;
import io.zbus.transport.ServerAdaptor;
import io.zbus.transport.Session;
import io.zbus.transport.Session.SessionType;
import io.zbus.transport.http.HttpMessage;

public class RpcServerAdaptor extends ServerAdaptor {
	protected final RpcProcessor processor; 

	public RpcServerAdaptor(RpcProcessor processor) {
		this.processor = processor;
	}

	@SuppressWarnings("unchecked")
	@Override
	public void onMessage(Object msg, Session sess) throws IOException { 
		Map<String, Object> request = null; 
		SessionType sessionType = SessionType.Websocket;
		if (msg instanceof HttpMessage) {
			HttpMessage reqMsg = (HttpMessage) msg;
			request = handleUrlMessage(reqMsg);
			if (request == null) {
				request = JsonKit.parseObject(reqMsg.bodyString());
			}
			sessionType = SessionType.HTTP;
		} else if (msg instanceof byte[]) {
			request = JsonKit.parseObject((byte[]) msg);
			sessionType = SessionType.Websocket; 
		} else if(msg instanceof Map) { 
			request =  (Map<String,Object>)msg;
			sessionType = SessionType.Inproc; 
		} else {
			throw new IllegalStateException("Not support message type");
		}
		
		Map<String, Object> response = processor.process(request); 
		Object body = response.get(Protocol.BODY);
		if (body != null && body instanceof HttpMessage) { //Special case when body is HTTP Message, make it browser friendly
			HttpMessage res = (HttpMessage)body;
			if (sessionType == SessionType.HTTP) {
				if (res.getStatus() == null) {
					res.setStatus(200);
				} 
				sess.write(res); 
				return; 
			} else {
				response.put(Protocol.BODY, res.toString());
			}
		}
		
		if(sessionType == SessionType.Websocket) {
			byte[] data = JsonKit.toJSONBytes(response, "utf8");
			sess.write(data);
			return;
		}
		
		if(sessionType == SessionType.HTTP) {
			HttpMessage resMsg = new HttpMessage();
			byte[] data = JsonKit.toJSONBytes(response, "utf8");
			resMsg.setStatus(200);
			resMsg.setEncoding("utf8");
			resMsg.setHeader("content-type", "application/json");
			resMsg.setBody(data);
			sess.write(resMsg);
			return;
		}
		
		if(sessionType == SessionType.Inproc) { 
			sess.write(response);
			return;
		} 
	}

	protected Map<String, Object> handleUrlMessage(HttpMessage msg) {
		String url = msg.getUrl();
		if (url == null || "/".equals(url)) {
			return null;
		}
		if (msg.getBody() != null)
			return null;

		return HttpKit.parseRpcUrl(url, false); 
	}
}
