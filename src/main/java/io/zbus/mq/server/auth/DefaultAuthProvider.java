package io.zbus.mq.server.auth;

import io.zbus.kit.StrKit;
import io.zbus.mq.Message;
import io.zbus.mq.Protocol;
import io.zbus.mq.server.auth.Token.TopicResource;

/**
 * DefaultAuthProvider authenticates on Token's Operation(cmd) and Resource(topic/consume_group)
 * 
 * Subclass may only need to load the TokenTable, such as from database or file system. 
 * 
 * @author Rushmore
 *
 */
public class DefaultAuthProvider implements AuthProvider {  
	protected TokenTable tokenTable = new TokenTable(); //default to empty, disabled
	
	@Override
	public boolean auth(Message message) {   
		if(!tokenTable.isEnabled()){ 
			return true;
		}
		String tokenStr = message.getToken();
		if(tokenStr == null){ //treat null as ""
			tokenStr = "";
		}
		
		Token token = tokenTable.get(tokenStr);
		if(token == null) { //No token found
			return false;
		}
		 
		if(Operation.isEnabled(token.operation, Operation.ADMIN)){ //no need to check resource
			return true;
		}  	
		
		String cmd = message.getCommand(); 
		if(!authOperation(cmd, token)) return false;
		 
		return authResource(message, token); 
	}    
	
	public boolean authOperation(String cmd, Token token){ 
		if(Protocol.PRODUCE.equals(cmd) || Protocol.RPC.equals(cmd)){
			if(!Operation.isEnabled(token.operation, Operation.PRODUCE)){
				return false;
			}
		}
		
		if(Protocol.CONSUME.equals(cmd) || Protocol.UNCONSUME.equals(cmd)){
			if(!Operation.isEnabled(token.operation, Operation.CONSUME)){
				return false;
			}
		}
		
		if(Protocol.ROUTE.equals(cmd)){
			if(!Operation.isEnabled(token.operation, Operation.ROUTE)){
				return false;
			}
		}  
		
		if(Protocol.DECLARE.equals(cmd)){
			if(!Operation.isEnabled(token.operation, Operation.DECLARE)){
				return false;
			}
		}  
		
		if(Protocol.EMPTY.equals(cmd)){
			if(!Operation.isEnabled(token.operation, Operation.EMPTY)){
				return false;
			}
		}  
		
		if(Protocol.REMOVE.equals(cmd)){
			if(!Operation.isEnabled(token.operation, Operation.REMOVE)){
				return false;
			}
		}  
		
		return true;  
	} 
	
	public boolean authResource(Message message, Token token){ 
		if(token.allTopics) return true; 
		String topic = message.getTopic();
		TopicResource topicResource = token.topics.get(topic);
		if(topicResource == null){ //topic not in token's list
			return false;
		} 
		 
		if(topicResource.allGroups) return true;  
		
		String cmd = message.getCommand();
		if(!needCheckConsumeGroup(cmd)) return true; //some commands like produce, no need to check
		
		String consumeGroup = message.getConsumeGroup();
		if(StrKit.isEmpty(consumeGroup)){ 
			consumeGroup = topic; //default to topic
		}
		
		if(!topicResource.consumeGroups.contains(consumeGroup)) return false; 
		
		return true; 
	} 
	
	protected boolean needCheckConsumeGroup(String cmd){
		if(Protocol.PRODUCE.equals(cmd)) return false; //shortcut
		
		if(Protocol.CONSUME.equals(cmd)) return true;
		if(Protocol.UNCONSUME.equals(cmd)) return true;
		if(Protocol.DECLARE.equals(cmd)) return true;
		if(Protocol.REMOVE.equals(cmd)) return true;
		if(Protocol.EMPTY.equals(cmd)) return true;
		
		//otherwise, no need to check by default
		return false;
	}
}
