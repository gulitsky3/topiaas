package io.zbus.rpc.auth;

import java.util.Map;

import io.zbus.auth.JsonToken;
import io.zbus.auth.JsonToken.TokenExpiredException;
import io.zbus.rpc.RpcFilter;
import io.zbus.rpc.auth.UserService.DefaultUserService;
import io.zbus.transport.Message;
 

public class AuthFilter implements RpcFilter {  
	private String cookieKey = "token"; 
	private String headerKey = "token"; 
	private String secretKey = "461277322-943d-4b2f-b9b6-3f860d746ffd";
	
	private boolean cookieEnabled = true;
	private boolean headerEnabled = true;
	
	private UserService userService = new DefaultUserService();

	@Override
	public boolean doFilter(Message request, Message response, Throwable exception) {    
		
		String token = null;
		if(cookieEnabled) {
			token = (String)request.getCookie(cookieKey);  
		}
		if(token == null && headerEnabled) { //cookie first
			token = request.getHeader(headerKey);
		}
		
		if(token == null) {
			response.setStatus(401); 
			response.setBody("Missing " + cookieKey + " in cookie, or " + headerKey + " in header");
			return false;
		} 
		
		try {
			
			Map<String, Object> claim = JsonToken.parser()
					.setSecretKey(secretKey)
					.parse(token);
			
			if(userService != null) {
				if(!userService.validateUserInfo(claim, request, response)) {
					if(response.getStatus() == null) {
						response.setStatus(403); 
					}
					String cookie = cookieKey + "=; max-age=0; path=/;";
					response.setHeader("Set-Cookie", cookie);
					
					if(response.getBody() == null) {
						response.setBody("Invalid user in token");
					}
					return false;
				}
			} 
			
		} catch (TokenExpiredException e) { 
			response.setStatus(401);
			String cookie = cookieKey + "=; max-age=0; path=/;";
			response.setHeader("Set-Cookie", cookie);
			response.setBody("Token expired");
			return false;
		} catch (Exception e) { 
			response.setStatus(500); 
			response.setBody(e.getMessage());
			return false;
		}   
		
		return true;
	}


	public String getCookieKey() {
		return cookieKey;
	}


	public void setCookieKey(String cookieKey) {
		this.cookieKey = cookieKey;
	}


	public String getHeaderKey() {
		return headerKey;
	}


	public void setHeaderKey(String headerKey) {
		this.headerKey = headerKey;
	}


	public String getSecretKey() {
		return secretKey;
	}


	public void setSecretKey(String secretKey) {
		this.secretKey = secretKey;
	}


	public boolean isCookieEnabled() {
		return cookieEnabled;
	}


	public void setCookieEnabled(boolean cookieEnabled) {
		this.cookieEnabled = cookieEnabled;
	}


	public boolean isHeaderEnabled() {
		return headerEnabled;
	}


	public void setHeaderEnabled(boolean headerEnabled) {
		this.headerEnabled = headerEnabled;
	}


	public UserService getUserService() {
		return userService;
	}


	public void setUserService(UserService userService) {
		this.userService = userService;
	}  
	
}
