package io.zbus.rpc.auth;

import java.util.Map;

import com.alibaba.fastjson.JSONObject;

import io.zbus.auth.JsonToken;
import io.zbus.auth.JsonToken.Builder;
import io.zbus.rpc.InvocationContext;
import io.zbus.rpc.annotation.Param;
import io.zbus.rpc.annotation.Route;
import io.zbus.rpc.auth.UserService.DefaultUserService;
import io.zbus.transport.Message;

@Route(exclude=true)
public class LoginApi {  
	private String cookieKey = "token";  
	private String secretKey = "461277322-943d-4b2f-b9b6-3f860d746ffd";
	private int expireTimeInSeconds = -1;
	
	private UserService userService = new DefaultUserService();

	
    @Route("/login")
    public Message login (@Param("user") JSONObject user) { 
    	if(user == null) {
    		throw new IllegalArgumentException("Missing user to login");
    	}
    	//validate user  
    	Map<String, Object> createdUserInfo = userService.createUserInfo(user, 
    			InvocationContext.getRequest(), 
    			InvocationContext.getResponse()); 

        Builder b = JsonToken.builder()
            .setPayload(createdUserInfo)
            .setIssuedAt(System.currentTimeMillis());
        if(expireTimeInSeconds > 0) {
        	b.setExpiration(System.currentTimeMillis() + expireTimeInSeconds*1000);
        }
        b.signWith(secretKey);
           
        String token = b.build(); 

        Message response = new Message();
        response.setStatus(200);
        String cookie = cookieKey + "=" + token+"; max-age=; path=/;";
		response.setHeader("Set-Cookie", cookie);    
        
        return response;
    }
    
    @Route("/logout")
    public Message logout () {  
        Message response = new Message();
        response.setStatus(200);
        String cookie = cookieKey + "=; max-age=0; path=/;";
		response.setHeader("Set-Cookie", cookie);   
		
        return response;
    }

	public String getCookieKey() {
		return cookieKey;
	}

	public void setCookieKey(String cookieKey) {
		this.cookieKey = cookieKey;
	}

	public String getSecretKey() {
		return secretKey;
	}

	public void setSecretKey(String secretKey) {
		this.secretKey = secretKey;
	}

	public int getExpireTimeInSeconds() {
		return expireTimeInSeconds;
	}

	public void setExpireTimeInSeconds(int expireTimeInSeconds) {
		this.expireTimeInSeconds = expireTimeInSeconds;
	}

	public UserService getUserService() {
		return userService;
	}

	public void setUserService(UserService userService) {
		this.userService = userService;
	}  
}
