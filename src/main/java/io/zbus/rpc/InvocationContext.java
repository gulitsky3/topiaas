package io.zbus.rpc;

import io.zbus.transport.Message;

public class InvocationContext {
	private static final ThreadLocal<Message> request = new ThreadLocal<>();
	private static final ThreadLocal<Message> response = new ThreadLocal<>();
	 
	public static Message getRequest() {
		return request.get();
	}
	
	public static Message getResponse() {
		return response.get();
	}
	
	public static void set(Message request, Message response) {
		InvocationContext.request.set(request);
		InvocationContext.response.set(response);
	}
	
	public static void setRequest(Message request) {
		InvocationContext.request.set(request);
	}
	
	public static void setResponse(Message response) {
		InvocationContext.response.set(response);
	}
	
}
