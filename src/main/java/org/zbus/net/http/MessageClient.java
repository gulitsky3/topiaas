package org.zbus.net.http;

import java.io.IOException;
import java.util.List;

import org.zbus.net.CodecInitializer;
import org.zbus.net.EventDriver;
import org.zbus.net.http.Message.MessageInvoker;
import org.zbus.net.tcp.TcpClient;

import io.netty.channel.ChannelHandler;
import io.netty.handler.codec.http.HttpObjectAggregator;
import io.netty.handler.codec.http.HttpRequestEncoder;
import io.netty.handler.codec.http.HttpResponseDecoder;

public class MessageClient extends TcpClient<Message, Message> implements MessageInvoker{
	
	public MessageClient(String address, EventDriver driver){
		super(address, driver); 
		
		codec(new CodecInitializer() {
			@Override
			public void initPipeline(List<ChannelHandler> p) {
				p.add(new HttpRequestEncoder()); 
				p.add(new HttpResponseDecoder());  
				p.add(new HttpObjectAggregator(1024*1024*32)); //maximum of 32M
				p.add(new MessageToHttpWsCodec());
			}
		});
		
		startHeartbeat(300000);//sending heartbeat every 5 minute
	}
	
	@Override
	public void heartbeat() {
		if(this.hasConnected()){
			Message hbt = new Message();
			hbt.setCmd(Message.HEARTBEAT);
			try {
				this.invokeAsync(hbt, null);
			} catch (IOException e) {  
				//ignore
			}
		}
	} 
	 
	@Override
	public String toString() {
		return "MessageClient" +  super.toString();
	}
	
}
 
