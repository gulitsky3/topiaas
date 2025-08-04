package io.zbus.mq;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;


public class Protocol {  
	public static final String VERSION_VALUE = "080";          //start from 0.8.0
	public static final String VERSION       = "version";
	
	public static final String PRODUCE       = "produce";   //produce message
	public static final String CONSUME       = "consume";   //consume message
	public static final String ROUTE   	     = "route";     //route back message to sender
 
	public static final String DECLARE_TOPIC = "declare_topic"; //create topic
	public static final String QUERY_TOPIC   = "query_topic"; 
	public static final String REMOVE_TOPIC  = "remove_topic"; //remove topic  
	 
	public static final String PING          = "ping"; 
	public static final String DATA          = "data"; 
	public static final String JQUERY        = "jquery";   
	

	public static final String COMMAND  = "cmd";     
	public static final String TOPIC    = "topic";
	public static final String FLAG     = "flag";
	public static final String TAG   	= "tag";  
	public static final String OFFSET   = "offset";
	
	public static final String CONSUME_GROUP        = "consume_group";  
	public static final String CONSUME_BASE_GROUP   = "consume_base_group";  
	public static final String CONSUME_START_OFFSET = "consume_start_offset";
	public static final String CONSUME_START_MSGID  = "consume_start_msgid";
	public static final String CONSUME_START_TIME   = "consume_start_time";  
	public static final String CONSUME_WINDOW       = "consume_window";  
	public static final String CONSUME_FILTER_TAG   = "consume_filter_tag";  
	
	public static final String SENDER   = "sender"; 
	public static final String RECVER   = "recver";
	public static final String ID      	= "id";	   
	
	public static final String SERVER   = "server";  
	public static final String ACK      = "ack";	  
	public static final String ENCODING = "encoding";
	public static final String DELAY    = "delay";
	public static final String TTL      = "ttl";  
	public static final String EXPIRE   = "expire"; 
	
	public static final String ORIGIN_ID     = "rawid";      //original id
	public static final String ORIGIN_URL    = "origin_url"; //original URL  
	public static final String ORIGIN_STATUS = "reply_code"; //original Status  
	
	//auth
	public static final String APPID   = "appid";
	public static final String TOKEN   = "token";
	
	
	public static final int FLAG_RPC    	    = 1<<0; 
	public static final int FLAG_EXCLUSIVE 	    = 1<<1;  
	public static final int FLAG_DELETE_ON_EXIT = 1<<2; 
	
	 
	public static class BrokerInfo{
		public String broker;
		public long lastUpdatedTime = System.currentTimeMillis(); 
		public Map<String, MqInfo> mqTable = new HashMap<String, MqInfo>(); 
		
		public boolean isObsolete(long timeout){
			return (System.currentTimeMillis()-lastUpdatedTime)>timeout;
		}
	}
	 
	public static class MqInfo { 
		public String name;
		public int mode;
		public String creator;
		public long lastUpdateTime;
		public int consumerCount;
		public long unconsumedMsgCount;
		public List<ConsumerInfo> consumerInfoList = new ArrayList<ConsumerInfo>();
	}
	
	public static class ConsumerInfo {
		public String remoteAddr;
		public String status; 
	} 
}