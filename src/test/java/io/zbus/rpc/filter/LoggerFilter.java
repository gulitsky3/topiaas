package io.zbus.rpc.filter;

import io.zbus.rpc.RpcFilter;
import io.zbus.rpc.annotation.FilterDef;
import io.zbus.transport.Message;

@FilterDef("logger")
public class LoggerFilter implements RpcFilter {

	@Override
	public boolean doFilter(Message request, Message response, Throwable exception) { 
		System.out.println("[Filter=logger]: " + request);
		return true;
	} 
}
