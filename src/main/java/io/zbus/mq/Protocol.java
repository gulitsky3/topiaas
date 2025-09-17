package io.zbus.mq;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;


public class Protocol {  
	public static final String VERSION_VALUE = "0.8.0";       //start from 0.8.0
	public static final String VERSION       = "version";
	
	//MQ Produce/Consume
	public static final String PRODUCE       = "produce";   
	public static final String CONSUME       = "consume";   
	public static final String ROUTE   	     = "route";     //route back message to sender, designed for RPC
 
	//Topic control
	public static final String DECLARE_TOPIC = "declare_topic";  
	public static final String QUERY_TOPIC   = "query_topic"; 
	public static final String REMOVE_TOPIC  = "remove_topic";  
	public static final String PAUSE_TOPIC   = "pause_topic";  
	public static final String RESUME_TOPIC  = "resume_topic";  
	public static final String EMPTY_TOPIC   = "empty_topic";  
	
	//ConsumeGroup control
	public static final String DECLARE_GROUP = "declare_group";  
	public static final String QUERY_GROUP   = "query_group"; 
	public static final String REMOVE_GROUP  = "remove_group";  
	public static final String PAUSE_GROUP   = "pause_group";  
	public static final String RESUME_GROUP  = "resume_group";  
	public static final String EMPTY_GROUP   = "empty_group";  
	 
	
	//High Availability (HA)
	public static final String TRACK_QUERY      = "track_query";  
	public static final String TRACK_PUB_TOPIC  = "track_pub_topic"; 
	public static final String TRACK_PUB_SERVER = "track_pub_server"; 
	public static final String TRACK_SUB        = "track_sub";   
	
	
	
	//Simple HTTP server command
	public static final String PING          = "ping"; //ping server, returning back server time
	public static final String INFO          = "info"; //version info
	public static final String JAVASCRIPT    = "js";   //serve javascript file
	public static final String CSS           = "css";  //serve css file 
	
	
	//Message parameters
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
	
	//Security
	public static final String APPID   = "appid";
	public static final String TOKEN   = "token";
	
	public static final int FLAG_RPC    	    = 1<<0; 
	public static final int FLAG_EXCLUSIVE 	    = 1<<1;  
	public static final int FLAG_DELETE_ON_EXIT = 1<<2; 
	
	  
	public static class ServerInfo extends TrackItem{   
		public Map<String, TopicInfo> topicMap = new ConcurrentHashMap<String, TopicInfo>(); 

		@Override
		public boolean equals(Object obj) {
			if (this == obj)
				return true;
			if (obj == null)
				return false;
			if (getClass() != obj.getClass())
				return false;
			ServerInfo other = (ServerInfo) obj;
			if (topicMap == null) {
				if (other.topicMap != null)
					return false;
			} else if (!topicMap.equals(other.topicMap))
				return false;
			
			if (serverAddress == null) {
				if (other.serverAddress != null)
					return false;
			} else if (!serverAddress.equals(other.serverAddress))
				return false;
			return true;
		}  
	}
	
	public static class TopicInfo extends TrackItem{ 
		public String topicName;
		public int flag; 
		
		public long messageCount;
		public int consumerCount;
		public int consumerGroupCount;
		public List<ConsumerGroupInfo> consumeGroupList = new ArrayList<ConsumerGroupInfo>();
		
		public String creator;
		public long createdTime;
		public long lastUpdatedTime; 
		
		@Override
		public boolean equals(Object obj) {
			if (this == obj)
				return true;
			if (obj == null)
				return false;
			if (getClass() != obj.getClass())
				return false;
			TopicInfo other = (TopicInfo) obj; 
			
			if (consumerCount != other.consumerCount)
				return false;
			if (consumerGroupCount != other.consumerGroupCount)
				return false; 
			
			if (flag != other.flag)
				return false; 
			if (messageCount != other.messageCount)
				return false;
			if (topicName == null) {
				if (other.topicName != null)
					return false;
			} else if (!topicName.equals(other.topicName))
				return false;
			
			if (serverAddress == null) {
				if (other.serverAddress != null)
					return false;
			} else if (!serverAddress.equals(other.serverAddress))
				return false;
			
			return true;
		} 
	}  
	
	public static class TrackItem{
		public String serverAddress;  
		public long publishTimestamp = System.currentTimeMillis(); 
		public boolean live = true;    
	}
	
	public static class ConsumerGroupInfo{ 
		public String topicName;
		public String groupName;
		public int flag; 
		public long messageCount;
		public int consumerCount;
		public List<String> consumerList = new ArrayList<String>();
		
		public String creator;
		public long createdTime;
		public long lastUpdatedTime;  
	} 
	
}
