package org.zbus.examples.rpc.mq;

import org.zbus.broker.Broker;
import org.zbus.broker.ZbusBroker;
import org.zbus.net.Sync.ResultCallback;
import org.zbus.net.http.Message.MessageInvoker;
import org.zbus.rpc.RpcCodec.Request;
import org.zbus.rpc.RpcCodec.Response;
import org.zbus.rpc.RpcInvoker;
import org.zbus.rpc.mq.MqInvoker;

public class RpcClientRaw { 
	
	public static void main(String[] args) throws Exception {  
		
		Broker broker = new ZbusBroker(); 
		MessageInvoker mqInvoker = new MqInvoker(broker, "MyRpc");  
		 
		RpcInvoker rpc = new RpcInvoker(mqInvoker); 
		
		rpc.invokeSync(String.class, "echo", "test");
		
		Request request = new Request(); 
		request.setMethod("echo");
		request.setParams(new Object[]{"test"});
		
		Response response = rpc.invokeSync(request);
		System.out.println(response.getResult());
		
		rpc.invokeAsync(request, new ResultCallback<Response>() { 
			@Override
			public void onReturn(Response result) { 
				System.out.println(result.getResult());
			}
		});
		
		broker.close();
	}  
}
