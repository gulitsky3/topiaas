package io.zbus.rpc;

import java.util.Map;
import java.util.Map.Entry;

import org.springframework.context.ApplicationContext;
import org.springframework.context.support.ClassPathXmlApplicationContext;

import io.zbus.kit.StrKit;
import io.zbus.mq.MqServer;
import io.zbus.rpc.annotation.FilterDef;
import io.zbus.rpc.annotation.FilterType;
import io.zbus.rpc.annotation.Route;

public class Spring {   
	 
	public static ApplicationContext run(String xmlConfig) {  
		ApplicationContext ctx = new ClassPathXmlApplicationContext(xmlConfig);    
		RpcProcessor rpcProcessor = null;
		try {
			rpcProcessor = ctx.getBean(RpcProcessor.class);
		} catch (Exception e) { 
			rpcProcessor = new RpcProcessor();
		}  
		
		//build filer 
		Map<String, Object> table = ctx.getBeansWithAnnotation(FilterDef.class); 
		for(Entry<String, Object> e : table.entrySet()) { 
			Object service = e.getValue();
			if(!(service instanceof RpcFilter)) {
				continue; //ignore
			}
			RpcFilter filter = (RpcFilter) service;
			FilterDef anno = service.getClass().getAnnotation(FilterDef.class);
			if(anno == null) continue;
			String name = anno.name(); 
			if(StrKit.isEmpty(name)) name = anno.value(); 
			
			FilterType type = anno.type();
			if(type == FilterType.GlobalBefore) {
				rpcProcessor.setBeforeFilter(filter);
			} else if(type == FilterType.GlobalAfter) {
				rpcProcessor.setAfterFilter(filter);
			} else if(type == FilterType.Exception) {
				rpcProcessor.setExceptionFilter(filter);
			} 
			
			if(!StrKit.isEmpty(name)) {
				rpcProcessor.getAnnotationFilterTable().put(name, filter);
			}
		}  
		
		//mount service with Route annotation
		table = ctx.getBeansWithAnnotation(Route.class); 
		for(Entry<String, Object> e : table.entrySet()) { 
			Object service = e.getValue();
			
			Route anno = service.getClass().getAnnotation(Route.class);
			if(anno == null) continue;
			String urlPrefix = anno.path();
			if(StrKit.isEmpty(urlPrefix)) urlPrefix = anno.value();
			if(!StrKit.isEmpty(urlPrefix)) {
				rpcProcessor.mount(urlPrefix, service); 
			}
		} 
		
		RpcServer rpcServer = null;
		try {
			rpcServer = ctx.getBean(RpcServer.class);
			if(rpcServer.getRpcProcessor() == null) { //if missing set it
				rpcServer.setRpcProcessor(rpcProcessor);
			}
		} catch (Exception e) { 
			rpcServer = new RpcServer();
			MqServer mqServer = new MqServer(8080); //TODO default port
			rpcServer.setRpcProcessor(rpcProcessor);
			rpcServer.setMqServer(mqServer); 
			rpcServer.start();
		} finally {
			rpcServer.start();
		}
		return ctx;
	}
}
