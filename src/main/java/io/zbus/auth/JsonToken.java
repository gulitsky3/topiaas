package io.zbus.auth;

import java.util.Base64;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONObject;
import com.alibaba.fastjson.serializer.SerializerFeature;

import io.zbus.kit.CryptoKit;
import io.zbus.kit.JsonKit;

/**
 * 
 * TODO make it compatible to JWT 
 * 
 * @author Rushmore
 *
 */
public class JsonToken { 
	public static final String ISSUED_AT = "iat";
	public static final String EXPIRATION = "exp";
	
	public static class Builder{
		private Map<String, Object> payload = new HashMap<>();  
		private JSONObject header = new JSONObject();
		private String secretKey="";
		
		public Builder() { //Follow JWT standard header
			header.put("alg", "HS256");
			header.put("typ", "JWT"); 
		}

		public Map<String, Object>  getPayload() {
			return payload;
		}

		public Builder setPayload(Map<String, Object>  claims) {
			this.payload = claims;
			return this;
		}

		public Long getIssuedAt() { 
			return (Long)this.payload.get(ISSUED_AT);
		}

		public Builder setIssuedAt(Long value) {
			this.payload.put(ISSUED_AT, value);
			return this;
		}

		public Long getExpiration() { 
			return (Long)this.payload.get(EXPIRATION);
		}

		public Builder setExpiration(Long value) {
			this.payload.put(EXPIRATION, value);
			return this;
		}
		
		public Builder signWith(String secretKey) {
			this.secretKey = secretKey;
			return this;
		}

		public String getSecretKey() {
			return secretKey;
		}

		public Builder setSecretKey(String secretKey) {
			this.secretKey = secretKey;
			return this;
		}  
		
		public String build() {
			String headerString = JSON.toJSONString(header, SerializerFeature.MapSortField); //Sort map by key 
			String headerBase64 = Base64.getEncoder().encodeToString(headerString.getBytes());
			
			String payloadString = JSON.toJSONString(payload, SerializerFeature.MapSortField); //Sort map by key  
			String payloadBase64 = Base64.getEncoder().encodeToString(payloadString.getBytes());
			
			String signMessage = headerBase64 + "." + payloadBase64; //follow JWT standard format
			String sign = CryptoKit.hmacSign(signMessage, secretKey); 
			return headerBase64+"." + payloadBase64 + "." + sign;
		}
	}
	
	
	public static class Parser{
		private String secretKey="";
		
		public Parser setSecretKey(String secretKey) {
			this.secretKey = secretKey;
			return this;
		}  
		
		public Map<String, Object> parse(String token) throws TokenExpiredException {
			String[] parts = token.split("[.]");
			if(parts.length != 3) {
				throw new IllegalArgumentException("Illegal token format: " + token);
			}
			
			String signMessage = parts[0] + "." + parts[1]; //follow JWT standard format
			String sign = CryptoKit.hmacSign(signMessage, secretKey); 
			if(!sign.equals(parts[2])) {
				throw new TokenMismatchedException(token);
			} 
			
			JSONObject payload = JsonKit.parseObject(Base64.getDecoder().decode(parts[1]), JSONObject.class);
			
			Long expiration = payload.getLong(EXPIRATION); 
			if(expiration != null) { 
				if(expiration < System.currentTimeMillis()) {
					throw new TokenExpiredException("Expired at: " + new Date(expiration) + ", token=" + token);
				}
			}  
			return payload;
		}
	}
	
	public static Builder builder() {
		return new Builder();
	}
	
	public static Parser parser() {
		return new Parser();
	}
	
	
	public static class TokenExpiredException extends Exception { 
		private static final long serialVersionUID = -7725932459979995559L;   
		
		public TokenExpiredException(String message) {
			super(message); 
		}

		public TokenExpiredException() { 
		}

		public TokenExpiredException(String message, Throwable cause) {
			super(message, cause); 
		}

		public TokenExpiredException(Throwable cause) {
			super(cause); 
		}
	}
	
	public static class TokenMismatchedException extends RuntimeException {   
		private static final long serialVersionUID = -5890953149815145086L;

		public TokenMismatchedException(String message) {
			super(message); 
		}

		public TokenMismatchedException() { 
		}

		public TokenMismatchedException(String message, Throwable cause) {
			super(message, cause); 
		}

		public TokenMismatchedException(Throwable cause) {
			super(cause); 
		}
	}
}
