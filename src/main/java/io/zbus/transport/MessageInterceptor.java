package io.zbus.transport;

public interface MessageInterceptor {  
	void intercept(Message message);
}
