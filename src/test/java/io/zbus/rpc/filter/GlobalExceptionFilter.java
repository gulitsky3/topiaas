package io.zbus.rpc.filter;

import io.zbus.rpc.RpcFilter;
import io.zbus.rpc.annotation.FilterDef;
import io.zbus.rpc.annotation.FilterType;
import io.zbus.transport.Message;

@FilterDef(type=FilterType.Exception)
public class GlobalExceptionFilter implements RpcFilter {

	@Override
	public boolean doFilter(Message request, Message response, Throwable exception) {  
		exception.printStackTrace();
		response.setStatus(500);
		response.setBody("global exception filter: " + exception.getMessage());
		return false;
	} 
}
