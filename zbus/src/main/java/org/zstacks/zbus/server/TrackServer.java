package org.zstacks.zbus.server;
 

import java.io.IOException;
import java.util.Iterator;
import java.util.Map;
import java.util.Map.Entry;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.zstacks.zbus.protocol.BrokerInfo;
import org.zstacks.zbus.protocol.Proto;
import org.zstacks.zbus.protocol.TrackTable;
import org.zstacks.znet.ClientDispatcherManager;
import org.zstacks.znet.Helper;
import org.zstacks.znet.Message;
import org.zstacks.znet.MessageHandler;
import org.zstacks.znet.RemotingClient;
import org.zstacks.znet.RemotingServer;
import org.zstacks.znet.nio.Session;

import com.alibaba.fastjson.JSON;
 
 
public class TrackServer extends RemotingServer {  
	private static final Logger log = LoggerFactory.getLogger(TrackServer.class); 

	private long publishInterval = 10000;
	private long probeInterval = 3000; 
	
	private final TrackTable trackTable = new TrackTable(); 
	
	private Map<String, Session> subscribers = new ConcurrentHashMap<String, Session>();
	private Map<String, RemotingClient> brokerProbes = new ConcurrentHashMap<String, RemotingClient>();
	
	private final ScheduledExecutorService scheduledService = Executors.newSingleThreadScheduledExecutor();
	private ExecutorService trackExecutor = new ThreadPoolExecutor(4,16, 120, TimeUnit.SECONDS, new LinkedBlockingQueue<Runnable>());
	
	private ClientDispatcherManager trackerClientDispatcherManager = new ClientDispatcherManager();
	
	public TrackServer(int serverPort)  throws IOException{
		this("0.0.0.0", serverPort);
	}
	
	public TrackServer(String serverHost, int serverPort) throws IOException {
		super(serverHost, serverPort);
		
		this.serverName = "TrackServer";
		this.scheduledService.scheduleAtFixedRate(new Runnable() {	
			public void run() {
				publishTrackTable();
			}
		}, 0, publishInterval, TimeUnit.MILLISECONDS);
		
		this.scheduledService.scheduleAtFixedRate(new Runnable() {	
			public void run() {
				probeBrokers();
			}
		}, 0, probeInterval, TimeUnit.MILLISECONDS);
	}
	
	
	private void probeBrokers(){ 
		Iterator<Entry<String, RemotingClient>> iter = brokerProbes.entrySet().iterator();
		while(iter.hasNext()){
			Entry<String, RemotingClient> entry = iter.next();
			String brokerAddress = entry.getKey();
			RemotingClient client = entry.getValue();
			if(!client.hasConnected()){
				trackTable.removeBroker(brokerAddress); 
				iter.remove();
			}
		}
		log.info("Track: "+ trackTable); 
	}
	 
	
	private void publishTrackTable(){ 
		if(subscribers.size()<1) return;  
		
		String json = JSON.toJSONString(this.trackTable);
		Message msg = new Message();
		msg.setCommand(Proto.TrackPub);
		msg.setBody(json);
		
		Iterator<Entry<String, Session>> iter = subscribers.entrySet().iterator();
		while(iter.hasNext()){
			Entry<String, Session> entry = iter.next();
			Session sess = entry.getValue();
			if(!sess.isActive()){
				iter.remove();
				continue;
			}
			try {
				sess.write(msg);
			} catch (IOException e) {  
				iter.remove(); 
			}
		}
		
	}
	
	
	@Override
	public void init() { 	
		this.registerHandler(Proto.TrackReport, new MessageHandler() {  
			public void handleMessage(Message msg, Session sess) throws IOException {  
				
				final BrokerInfo brokerInfo = JSON.parseObject(msg.getBodyString(), BrokerInfo.class);
				
				final String brokerAddress = brokerInfo.getBroker(); 
				if(!brokerProbes.containsKey(brokerAddress)){
					final RemotingClient client = new RemotingClient(brokerAddress, trackerClientDispatcherManager);
					trackExecutor.submit(new Runnable() {
						public void run() { 
							try {
								client.connectIfNeed();
								brokerProbes.put(brokerAddress, client);
							} catch (IOException e) {
								log.error(e.getMessage(), e);
							}
						}
					});
				}
				
				trackTable.addBroker(brokerAddress, brokerInfo);
				 
				publishTrackTable(); 
			}
		});
		
		this.registerHandler(Proto.TrackSub, new MessageHandler() { 
			public void handleMessage(Message msg, Session sess) throws IOException {
				subscribers.put(sess.id(), sess);
				String json = JSON.toJSONString(trackTable);
				msg.setStatus("200");
				msg.setBody(json);
				sess.write(msg);
			}
		}); 
	}     
	
	
	
	public static void main(String[] args) throws Exception{
		int serverPort = Helper.option(args, "-p", 16666);
		TrackServer track = new TrackServer(serverPort); 
		track.start(); 
	} 
	
}