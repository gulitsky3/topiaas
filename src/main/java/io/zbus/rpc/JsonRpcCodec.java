/**
 * The MIT License (MIT)
 * Copyright (c) 2009-2015 HONG LEIMING
 * 
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 * 
 * The above copyright notice and this permission notice shall be included in
 * all copies or substantial portions of the Software.
 * 
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN
 * THE SOFTWARE.
 */
package io.zbus.rpc;
 

import io.zbus.kit.JsonKit;
import io.zbus.mq.Message; 
 

public class JsonRpcCodec implements RpcCodec {  
	private static final String DEFAULT_ENCODING = "UTF-8"; 
	
	public Message encodeRequest(Request request, String encoding) {
		Message msg = new Message();  
		if(encoding == null) encoding = DEFAULT_ENCODING;  
		msg.setEncoding(encoding);
		msg.setBody(JsonKit.toJSONBytesWithType(request, encoding)); 
		return msg;
	}
	
	public Request decodeRequest(Message msg) {
		String encoding = msg.getEncoding();
		if(encoding == null){
			encoding = DEFAULT_ENCODING;
		}
		String jsonString = msg.getBodyString(encoding);
		Request req = JsonKit.parseObject(jsonString, Request.class);
		if(req != null){
			Request.normalize(req);
		}
		return req;
	} 
	
	public Message encodeResponse(Object response, String encoding) {
		Message msg = new Message();   
		if(encoding == null) encoding = DEFAULT_ENCODING;  
		msg.setEncoding(encoding);  
		msg.setJsonBody(JsonKit.toJSONBytes(response, encoding));
		return msg; 
	}
	
 
	public Object decodeResponse(Message msg){ 
		String encoding = msg.getEncoding();
		if(encoding == null){
			encoding = DEFAULT_ENCODING;
		}
		String jsonString = msg.getBodyString(encoding);
		Object res = null; 
		try{
			res = JsonKit.parseObject(jsonString, Object.class);
		} catch (Exception e){  
			try{
				jsonString = jsonString.replace("@type", "@class"); //trick: disable desearialization by class name
				res = JsonKit.parseObject(jsonString); 
			} catch(Exception ex){
				String prefix = "";
				if(msg.getStatus() == 200){ 
					prefix = "JSON format invalid: ";
				}
				throw new RpcException(prefix + jsonString);
			}  
		} 
		return res;
	} 
	
	@Override
	public <T> T convert(Object value, Class<T> clazz) {
		return JsonKit.convert(value, clazz);
	}
}
