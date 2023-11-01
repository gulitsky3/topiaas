package org.zbus.mq;

import org.zbus.broker.Broker;
import org.zbus.broker.BrokerConfig;
import org.zbus.broker.SingleBroker;
import org.zbus.mq.Producer;
import org.zbus.net.http.Message;

public class ProducerSync {
	public static void main(String[] args) throws Exception { 
		//创建Broker代理
		BrokerConfig config = new BrokerConfig();
		config.setServerAddress("127.0.0.1:15555");
		final Broker broker = new SingleBroker(config);
 
		Producer producer = new Producer(broker, "MyMQ");
		producer.createMQ(); // 如果已经确定存在，不需要创建

		//创建消息，消息体可以是任意binary，应用协议交给使用者
		
		long total = 0;
		for(int i=0;i<1;i++){
			long start = System.currentTimeMillis();
			Message msg = new Message();
			msg.setId("123456");
			msg.setBody("hello world"+i);
			msg = producer.sendSync(msg);   
			long end = System.currentTimeMillis();
			total += (end-start);
			if(i%10000 == 0){
				System.out.format("Time: %.2f\n", total*1.0/(i+1));
			}
		}
		
		broker.close();
	}
}
