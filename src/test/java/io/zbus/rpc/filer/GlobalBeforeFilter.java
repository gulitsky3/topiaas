package io.zbus.rpc.filer;

import io.zbus.rpc.RpcFilter;
import io.zbus.transport.Message;

//@FilterDef(type=FilterType.GlobalBefore)
public class GlobalBeforeFilter implements RpcFilter {

	@Override
	public boolean doFilter(Message request, Message response) { 
		System.out.println("[Filter=GlobalBefore]: " + request);
		return true;
	} 
}
