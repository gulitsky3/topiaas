                /\\\       
                \/\\\        
                 \/\\\    
     /\\\\\\\\\\\ \/\\\         /\\\    /\\\  /\\\\\\\\\\     
     \///////\\\/  \/\\\\\\\\\  \/\\\   \/\\\ \/\\\//////     
           /\\\/    \/\\\////\\\ \/\\\   \/\\\ \/\\\\\\\\\\    
          /\\\/      \/\\\  \/\\\ \/\\\   \/\\\ \////////\\\  
         /\\\\\\\\\\\ \/\\\\\\\\\  \//\\\\\\\\\   /\\\\\\\\\\  
         \///////////  \/////////    \/////////   \//////////       QQ Group: 467741880

# ZBUS = MQ + RPC  
zbus strives to make Message Queue and Remote Procedure Call fast, light-weighted and easy to build your own service-oriented architecture for many different platforms. Simply put, zbus = mq + rpc.

zbus carefully designed on its protocol and components to embrace KISS(Keep It Simple and Stupid) principle, but in all it delivers power and elasticity. 

## Features
- Fast MQ of disk|memory|db, capable of unicast, multicast and broadcast messaging models
- Easy RPC support out of box 
- HTTP/WebSocket/InProc + JSON simple format, multiple languages support
- SSL + API Auth secured
- Extremely light-weighted (z---bus)
 
## Getting started  
In zbus-dist directory, just run zbus.bat/sh, JDK8+ required. 

Maven

	<dependency>
		<groupId>io.zbus</groupId>
		<artifactId>zbus</artifactId>
		<version>1.0.0-SNAPSHOT</version>
	</dependency>


## MQ Protocol

zbus MQ protocol is pretty simple, just use websocket or http connect to zbus, send out the followsing json format data

### Common format [JSON Key-Value]

	{
		cmd:       pub|sub|create|remove|query|ping //Request required,
		status:    200|400|404|403|500 ...          //Response required,
		body:      <data>,

		id:        <message_id>,
		apiKey:    <apid_key>,
		signature: <signature>
	} 

All requests to zbus should have id field (optional), when auth required, both apiKey and signature are required.

Signature generation algorithm

	1) sort key ascending in request (recursively on both key and value), and generate json string
	2) Init HmacSHA256 with secretKey, do encrypt on 1)'s json string to generate bytes
	3) signature = Hex format in upper case on the 2)'s bytes

### Publish Message 

Request

	{
		cmd:         pub,         //required
		mq:          <mq_name>,   //required 
		body:        <business data> 
	}

Response

	{
		status:      200|400|403|500,    
		body:        <string_response> 
	}

### Subscribe Message 

Request

	{
		cmd:         sub,           //required
		mq:          <mq_name>,     //required  
		channel:     <channel_name> //required
		window:      <window_size>
	}

Response

	First message: indicates subscribe success or failure
	{
		status:      200|400|403|500,    
		body:        <string_response> 
	}

	Following messages:
	{
		mq:          <mq_name>,     //required  
		channel:     <channel_name> //required
		sender:      <message from>
		id:          <message id>
		body:        <business_data>
	}

### Take Message 

Request

	{
		cmd:         take,          //required
		mq:          <mq_name>,     //required 
		channel:     <channel_name> //required
		window:      <batch_size>
	}

Response

	{
		status:      200|400|403|500|604, //604 stands for NO data   
		body:        <data> 
	}


### Create MQ/Channel 

Request

	{
		cmd:         create,         //required
		mq:          <mq_name>,      //required

		mqType:      memory|disk|db, //default to memory
		mqMask:      <mask_integer>,
		channel:     <channel_name>,
		channelMask: <mask_integer>,
		offset:      <channel_offset>,
		checksum:    <offset_checksum>
		topic:       <channel_topic>, 
	}

Response

	{
		status:      200|400|403|500,    
		body:        <message_response> 
	}

### Remove MQ/Channel 

Request

	{
		cmd:         remove,         //required
		mq:          <mq_name>,      //required 
		channel:     <channel_name> 
	}

Response

	{
		status:      200|400|403|500,    
		body:        <message_response> 
	}


### Query MQ/Channel 

Request

	{
		cmd:         query,          //required
		mq:          <mq_name>,      //required 
		channel:     <channel_name> 
	}

Response

	{
		status:      200|400|403|500,    
		body:        <mq_channel_info> 
	}
    
	Example
	{
		body: {
			channels: [ ],
			mask: 0,
			name: "DiskQ",
			size: 200000,
			type: "disk"
		},
		status: 200
	}



## RPC Protocol 

### Request

	{ 
		method:     <method_name>, //required 
		params:     [param_array],
		module:     <module_name>,

		id:        <message_id>,
		apiKey:    <apid_key>,
		signature: <signature>
	}

### Response

	{
		status:      200|400|403|500|604   //required
		body:        <data_or_exception>,
		id:          <message_id>
	}


 ## Performance
Fast performance test (Apache Benchmark)

	Create MQ: http://localhost:15555?cmd=create&mq=MyMQ&mqType=disk
	
	Produce 
	ab -k -c 32 -n 1000000 http://localhost:15555/?cmd=pub&mq=MyMQ

	Create MQ: http://localhost:15555?cmd=create&mq=MyMQ&channel=MyChannel
	Consume 
	ab -k -c 32 -n 1000000 http://localhost:15555?cmd=take&mq=MyMQ&channel=MyChannel

	Single Mac i7 box with SSD 
	Produce:  ~80,000/s
	Consume:  ~90,000/s 