                /\\\       
                \/\\\        
                 \/\\\    
     /\\\\\\\\\\\ \/\\\         /\\\    /\\\  /\\\\\\\\\\     
     \///////\\\/  \/\\\\\\\\\  \/\\\   \/\\\ \/\\\//////     
           /\\\/    \/\\\////\\\ \/\\\   \/\\\ \/\\\\\\\\\\    
          /\\\/      \/\\\  \/\\\ \/\\\   \/\\\ \////////\\\  
         /\\\\\\\\\\\ \/\\\\\\\\\  \//\\\\\\\\\   /\\\\\\\\\\  
         \///////////  \/////////    \/////////   \//////////  

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

[Protocol](./doc/Protocol.md)