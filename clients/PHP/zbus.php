﻿<?php 
class Protocol {  
	const VERSION_VALUE = "0.8.0";       //start from 0.8.0 
	
	//=============================[1] Command Values================================================
	//MQ Produce/Consume
	const PRODUCE       = "produce";   
	const CONSUME       = "consume";  
	const ROUTE   	    = "route";     //route back message to sender, designed for RPC 
	const RPC   	    = "rpc";       //the same as produce command except rpc set ack false by default
	
	//Topic control
	const DECLARE = "declare";  
	const QUERY   = "query"; 
	const REMOVE  = "remove";
	const EMPTY   = "empty";   
	
	//High Availability (HA) 
	const TRACK_PUB   = "track_pub"; 
	const TRACK_SUB   = "track_sub";  
	const TRACKER     = "tracker";    
	
	//=============================[2] Parameter Values================================================
	const COMMAND       	   = "cmd";     
	const TOPIC         	   = "topic";
	const TOPIC_MASK           = "topic_mask"; 
	const TAG   	     	   = "tag";  
	const OFFSET        	   = "offset";
	
	const CONSUME_GROUP        = "consume_group";  
	const GROUP_START_COPY     = "group_start_copy";  
	const GROUP_START_OFFSET   = "group_start_offset";
	const GROUP_START_MSGID    = "group_start_msgid";
	const GROUP_START_TIME     = "group_start_time";   
	const GROUP_FILTER         = "group_filter";  
	const GROUP_MASK           = "group_mask"; 
	const CONSUME_WINDOW       = "consume_window";  
	
	const SENDER   			= "sender"; 
	const RECVER   			= "recver";
	const ID      		    = "id";	   
	
	const HOST   		    = "host";  
	const ACK      			= "ack";	  
	const ENCODING 			= "encoding"; 
	
	const ORIGIN_ID         = "origin_id";
	const ORIGIN_URL   		= "origin_url";
	const ORIGIN_STATUS     = "origin_status";
	
	//Security 
	const TOKEN   		    = "token"; 
	
	
	const MASK_PAUSE    	  = 1<<0; 
	const MASK_RPC    	      = 1<<1; 
	const MASK_EXCLUSIVE 	  = 1<<2;  
	const MASK_DELETE_ON_EXIT = 1<<3; 
}

class ServerAddress {
	public $address;
	public $sslEnabled;

	function __construct($address, $sslEnabled = false) {
		$this->address = $address;
		$this->sslEnabled = $sslEnabled; 
	}
	
	public function __toString(){
		if($this->sslEnabled){
			return "[SSL]".$this->address;
		}
		return $this->address;
	}  
} 

class Message {
	public $status;          //integer
	public $method = "GET";
	public $url = "/";
	public $headers = array();
	public $body;  
	 
	
	public function set_json_body($value){
		$this->headers['content-type'] = 'application/json';
		$this->body = $value; 
	}
	
	public function __set($name, $value){
		if($value == null) return;
		$this->headers[$name] = $value;
	}
	
	public function __get($name){
		return $this->headers[$name];
	}
	
	public function __toString(){
		return $this->encode();
	}
	
	
	public function encode(){
		$res = "";
		$desc = "unknown status";
		if($this->status){
			if(array_key_exists($this->status, Message::HTTP_STATUS_TABLE)){
				$desc = Message::HTTP_STATUS_TABLE[$this->status];
			}
			$res .= sprintf("HTTP/1.1 %s %s\r\n",$this->status, $desc);
		} else {
			$res .= sprintf("%s %s HTTP/1.1\r\n",$this->method?:"GET", $this->url?:"/");
		}
		foreach($this->headers as $key=>$value){
			if($key == 'content-length'){
				continue;
			}
			$res .= sprintf("%s: %s\r\n", $key, $value);
		}
		$body_len = 0;
		if($this->body){
			$body_len = strlen($this->body);
		}
		$res .= sprintf("content-length: %d\r\n", $body_len);
		$res .= sprintf("\r\n");
		if($this->body){
			$res .= $this->body;
		}
		
		return $res;
	}
	
	public static function decode($buf, $start=0){
		$p = strpos($buf, "\r\n\r\n", $start);
		if($p < 0) return array(null, $start);
		
		$head = substr($buf, $start, $p);
		$msg = Message::decode_headers($head);
		$body_len = 0;
		if(array_key_exists('content-length', $msg->headers)){
			$body_len = $msg->headers['content-length'];
			$body_len = intval($body_len);
		} 
		if( $body_len == 0) return array($msg, $p); 
		
		if(strlen($buf)-$p < $body_len){
			return array(null, $start);
		}
		$msg->body = substr($buf, $p+4, $body_len); 
		return array($msg, $p + $body_len); 
	}
	 
	private static function decode_headers($buf){
		$msg = new Message();
		$lines = preg_split('/\r\n?/', $buf); 
		$meta = $lines[0];
		$blocks = explode(' ', $meta); 
		if(substr(strtoupper($meta), 0, 4 ) == "HTTP"){
			$msg->status = intval($blocks[0]);
		} else {
			$msg->method = strtoupper($blocks[0]);
			if(count($blocks) > 1){
				$msg->url = $blocks[1];
			}
		} 
		for($i=1; $i<count($lines); $i++){
			$line = $lines[$i]; 
			$kv = explode(':', $line); 
			if(count($kv) < 2) continue;
			$key = strtolower(trim($kv[0]));
			$val = trim($kv[1]); 
			$msg->headers[$key] = $val;
		} 
		return $msg;
	}
	
	private const HTTP_STATUS_TABLE = array( 
		200 => "OK",
		201 =>"Created",
		202 =>"Accepted",
		204 =>"No Content",
		206 =>"Partial Content",
		301 =>"Moved Permanently",
		304 =>"Not Modified",
		400 =>"Bad Request",
		401 =>"Unauthorized",
		403 =>"Forbidden",
		404 =>"Not Found",
		405 =>"Method Not Allowed",
		416 =>"Requested Range Not Satisfiable",
		500 =>"Internal Server Error",
	);
}




?> 