package io.zbus.rpc.server;

import java.io.IOException;
import java.util.Map;
import java.util.Map.Entry;

import io.zbus.kit.HttpKit;
import io.zbus.kit.JsonKit;
import io.zbus.rpc.RpcProcessor;
import io.zbus.transport.Message;
import io.zbus.transport.ServerAdaptor;
import io.zbus.transport.Session;
import io.zbus.transport.http.HttpWsServer;

public class HttpRpcServer extends HttpWsServer {
	private HttpRpcServerAdaptor httpRpcAdaptor;

	public HttpRpcServer(RpcProcessor processor) {
		this.httpRpcAdaptor = new HttpRpcServerAdaptor(processor);
	} 
	
	public HttpRpcServerAdaptor getHttpRpcAdaptor() {
		return httpRpcAdaptor;
	}  

	public void start(String host, int port) {
		if (host == null) {
			host = "0.0.0.0";
		}
		start(host, port, httpRpcAdaptor);
	}

	public static class HttpRpcServerAdaptor extends ServerAdaptor {
		protected final RpcProcessor processor;

		public HttpRpcServerAdaptor(RpcProcessor processor) {
			this.processor = processor;
		}
 
		@Override
		public void onMessage(Object msg, Session sess) throws IOException {
			Message request = null;  
			if (msg instanceof Message) {
				request = (Message) msg;  
			} else if (msg instanceof byte[]) {
				request = JsonKit.parseObject((byte[]) msg, Message.class);  
			} else if (msg instanceof String) {
				request = JsonKit.parseObject((String) msg, Message.class);  
			} else {
				throw new IllegalStateException("Not support message type");
			}
			
			handleUrlMessage(request);

			Message response = processor.process(request);
			if(response.getStatus() == null) {
				response.setStatus(200);
			}
			sess.write(response);
		}

		private void  handleUrlMessage(Message msg) { //TODO remove it
			String url = msg.getUrl();
			if (url == null || "/".equals(url)) {
				return;
			}
			if (msg.getBody() != null)
				return;

			Map<String, Object> headers = HttpKit.parseRpcUrl(url, false);
			for(Entry<String, Object> e : headers.entrySet()) {
				msg.addHeader(e.getKey(), e.getValue());
			}
		}
	}
}
