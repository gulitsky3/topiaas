package io.zbus.kit;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import javax.script.Bindings;

public class JsKit {
	private static Class<?> scriptClass;
	static {
		try {
			scriptClass = Class.forName("jdk.nashorn.api.scripting.ScriptObjectMirror");
		} catch (ClassNotFoundException e) { 
			scriptClass = null;
			e.printStackTrace();
		} 
	}
	@SuppressWarnings("unchecked")
	public static Object convert(final Object obj) { 
		if(scriptClass == null) return obj;
		
	    if (obj instanceof Bindings) {
	        try { 
	            if (!scriptClass.isAssignableFrom(obj.getClass())) return obj; 
	            
	            final Object isArray = scriptClass.getMethod("isArray").invoke(obj); 
                
                if (isArray != null && isArray.equals(true)) { 
                    final Object invoked = scriptClass.getMethod("values").invoke(obj);
                    if (invoked != null && invoked instanceof Collection) {
                        final Collection<Object> values = (Collection<Object>) invoked;
                        List<Object> list = new ArrayList<>();
                        for(Object o : values) {
                        	list.add(convert(o));
                        } 
                        return list;
                    } 
                    
                } else {
                	 final Object invoked = scriptClass.getMethod("entrySet").invoke(obj);
                     if (invoked != null && invoked instanceof Set) { 
                    	 final Set<Map.Entry<String, Object>> entrySet = (Set<Map.Entry<String, Object>>)invoked;
                    	 
                    	 Map<String, Object> map = new HashMap<>();
                    	 for(Map.Entry<String, Object> e : entrySet) {
                    		 map.put(e.getKey(), convert(e.getValue()));
                    	 }
                    	 return map;
                     } 
                } 
                
	        } catch (Exception e) {
	        	//ignore
	        }
	    }
	    
	    return obj;
	}
}
