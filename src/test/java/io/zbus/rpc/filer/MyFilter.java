package io.zbus.rpc.filer;

import io.zbus.rpc.RpcFilter;
import io.zbus.transport.Message;

public class MyFilter implements RpcFilter {

	@Override
	public boolean doFilter(Message request, Message response) { 
		System.out.println("In Filter: " + request);
		return false;
	}

}
