package io.zbus.rpc.filer;

import io.zbus.rpc.RpcFilter;
import io.zbus.rpc.annotation.FilterDef;
import io.zbus.transport.Message;

@FilterDef("user")
public class SimpleUserLoginFilter implements RpcFilter {

	@Override
	public boolean doFilter(Message request, Message response, Throwable exception) { 
		System.out.println("[Filter=user]: " + request);
		return true;
	} 
}