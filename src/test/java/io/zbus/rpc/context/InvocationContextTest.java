package io.zbus.rpc.context;

import io.zbus.rpc.InvocationContext;
import io.zbus.transport.Message;

public class InvocationContextTest {
	
	public static void main(String[] args) {
		for(int i=0;i<10;i++) {
			final String threadName = "thread"+i;
			Message request = new Message();
			request.setBody(threadName);
			Message response = new Message();
			response.setBody(threadName);
			Thread thread = new Thread(()->{
				InvocationContext.set(request, response);
				Message value = InvocationContext.getRequest();
				
				System.out.println(value.getBody());
			});
			thread.start();
		} 
	}
}
