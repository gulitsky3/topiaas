package io.zbus.rpc.filer;

import io.zbus.rpc.RpcFilter;
import io.zbus.rpc.annotation.FilterDef;
import io.zbus.transport.Message;

@FilterDef("logger")
public class LoggerFilter implements RpcFilter {

	@Override
	public boolean doFilter(Message request, Message response) { 
		System.out.println("[Filter=logger]: " + request);
		return true;
	} 
}
