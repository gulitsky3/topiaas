package io.zbus.rpc.filter;

import io.zbus.rpc.RpcFilter;
import io.zbus.rpc.annotation.FilterDef;
import io.zbus.transport.Message;

@FilterDef("admin")
public class AdminLoginFilter implements RpcFilter {

	@Override
	public boolean doFilter(Message request, Message response, Throwable exception) { 
		System.out.println("[Filter=admin]: " + request); 
		return true;
	} 
}
