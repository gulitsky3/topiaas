package io.zbus.kit;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.Collection;
import java.util.List;

import javax.script.Bindings;

public class JsKit {
	public static Object convert(final Object obj) { 
	    if (obj instanceof Bindings) {
	        try {
	            final Class<?> cls = Class.forName("jdk.nashorn.api.scripting.ScriptObjectMirror"); 
	            if (cls.isAssignableFrom(obj.getClass())) {
	                final Method isArray = cls.getMethod("isArray");
	                final Object result = isArray.invoke(obj);
	                if (result != null && result.equals(true)) {
	                    final Method values = cls.getMethod("values");
	                    final Object vals = values.invoke(obj);
	                    if (vals instanceof Collection<?>) {
	                        final Collection<?> coll = (Collection<?>) vals;
	                        return coll.toArray(new Object[0]);
	                    }
	                }
	            }
	        } catch (ClassNotFoundException | NoSuchMethodException | SecurityException
	                | IllegalAccessException | IllegalArgumentException | InvocationTargetException e) {}
	    }
	    if (obj instanceof List<?>) {
	        final List<?> list = (List<?>) obj;
	        return list.toArray(new Object[0]);
	    }
	    return obj;
	}
}
