package io.zbus.rpc.auth;

import java.util.Map;

import io.zbus.transport.Message;

public interface UserService {
	/**
	 * Validate user info in cookie or header (decoded)
	 * 
	 * @param cookieUserInfo
	 * @param request
	 * @param response
	 * @return true if check passed, false otherwise
	 */
	boolean validateUserInfo(Map<String, Object> cookieUserInfo, Message request, Message response);
	
	
	/**
	 * 
	 * Create user info to sign in cookie or header based on login user
	 * 
	 * @param userLoginInfo
	 * @param request
	 * @param response
	 * @return
	 */
	Map<String, Object> createUserInfo(Map<String, Object> userLoginInfo, Message request, Message response);
	
	
	public static class DefaultUserService implements UserService{ 
		@Override
		public boolean validateUserInfo(Map<String, Object> cookieUserInfo, Message request, Message response) { 
			return true;
		}

		@Override
		public Map<String, Object> createUserInfo(Map<String, Object> userBaseInfo, Message request, Message response) { 
			return userBaseInfo;
		} 
	}
}
