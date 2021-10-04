package org.zbus.common.json.parser;

import java.util.List;
import java.util.Map;

public interface ContainerFactory {
	/**
	 * @return A Map instance to store JSON object, or null if you want to use org.json.simple.JSONObject.
	 */
	Map<String, Object> createObjectContainer();
	
	/**
	 * @return A List instance to store JSON array, or null if you want to use org.json.simple.JSONArray. 
	 */
	List<Object> creatArrayContainer();
}
