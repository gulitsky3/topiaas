
package io.zbus.rpc;

public class RpcException extends RuntimeException {  
	private static final long serialVersionUID = 8445590420018236422L;
	
	private int status = 500; 
	
	public RpcException(int status, String message) {
		super(message);
		this.status = status;
	}
	
	public RpcException(String message) {
		super(message); 
	}

	public RpcException() { 
	}

	public RpcException(String message, Throwable cause) {
		super(message, cause); 
	}

	public RpcException(Throwable cause) {
		super(cause); 
	}
	
	public RpcException(String message, Throwable cause, boolean enableSuppression, boolean writableStackTrace) {
		super(message, cause, enableSuppression, writableStackTrace); 
	}
	
	public int getStatus() {
		return status;
	}
	
	public void setStatus(int status) {
		this.status = status;
	}
}
