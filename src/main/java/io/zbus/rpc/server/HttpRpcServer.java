package io.zbus.rpc.server;

import java.io.IOException;

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
			if (!(msg instanceof Message)) { 
				throw new IllegalStateException("Not support message type");
			}
			request = (Message) msg;  
			Message response = new Message();
			processor.process(request, response);
			if(response.getStatus() == null) {
				response.setStatus(200);
			}
			sess.write(response);
		} 
	}
}
