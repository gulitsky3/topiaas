package io.zbus.transport;

import java.util.Map;

public interface MessageInterceptor {  
	void intercept(Map<String, Object> message);
}
