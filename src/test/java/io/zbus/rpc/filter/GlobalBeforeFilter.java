package io.zbus.rpc.filter;

import java.util.HashMap;
import java.util.Map;

import io.zbus.rpc.RpcFilter;
import io.zbus.transport.Message;

//@FilterDef(type=FilterType.GlobalBefore)
public class GlobalBeforeFilter implements RpcFilter {

	@Override
	public boolean doFilter(Message request, Message response, Throwable exception) { 
		System.out.println("[Filter=GlobalBefore]: " + request);
		Map<String, Object> ctx = new HashMap<>();
		ctx.put("accessTime", System.currentTimeMillis());
		request.setContext(ctx);
		return true;
	} 
}
