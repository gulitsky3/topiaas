package org.zbus2.rpc_interface;

import java.util.List;


public class Order{ 
	private List<String> item;

	public List<String> getItem() {
		return item;
	}

	public void setItem(List<String> item) {
		this.item = item;
	}
	
	
	@Override
	public String toString() {
		return "Order [item=" + item + "]";
	}

}