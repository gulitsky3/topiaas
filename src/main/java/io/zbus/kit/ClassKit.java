
package io.zbus.kit;

import java.io.File;
import java.lang.annotation.Annotation;
import java.lang.reflect.Method;
import java.net.URL;
import java.net.URLClassLoader;
import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;

public class ClassKit {

	public static void main(String[] args) throws Exception {
		String[] names = getParameterNames(ClassKit.class.getMethod("newInstance", new Class[]{ String.class }));

		System.out.println(names == null ? null : Arrays.asList(names));
	}

	public static String[] getParameterNames(Method targetMethod) {
		try {
			String nameDiscovererClass = "org.springframework.core.LocalVariableTableParameterNameDiscoverer";
			Class<?> clazz = Thread.currentThread().getContextClassLoader().loadClass(nameDiscovererClass);
			Object nameDiscoverer = clazz.getConstructor().newInstance();
			Method getMethod = clazz.getMethod("getParameterNames", new Class[]{ Method.class });
			if (getMethod != null) {
				return (String[])getMethod.invoke(nameDiscoverer, targetMethod);
			}
		} catch (Throwable e) {
			// ignore
			return null;
		}
		return null;
	}

	@SuppressWarnings("unchecked")
	public static <T> T newInstance(String className) throws Exception{ 
		Class<?> clazz = Class.forName(className);
		return (T)clazz.newInstance(); 
	} 

	public static Set<Class<?>> scan(Class<? extends Annotation> annotation){
		Set<Class<?>> result = new HashSet<Class<?>>(); 
		ClassLoader cl = ClassLoader.getSystemClassLoader(); 
		URL[] urls = ((URLClassLoader) cl).getURLs(); 
		for (URL url : urls) {
			File root = new File(url.getFile()); 
			list(root, root, result, annotation);
		}  
		return result;
	}
	
	public static Set<Class<?>> scan(File packagePath, Class<? extends Annotation> annotation){  
		Set<Class<?>> result = new HashSet<Class<?>>(); 
		list(packagePath, packagePath, result, annotation);
		return result;
	}
	
	private static void list(File root, File current, Set<Class<?>> result, Class<? extends Annotation> annotation){
		File[] files = current.listFiles();
		if(files == null) return;
		for(File file : files){
			if (file.isFile()) { 
				String fileName = file.getAbsolutePath();
				if(!fileName.endsWith(".class")) continue;  
				String className = fileName.substring(root.getAbsolutePath().length()+1, fileName.lastIndexOf('.'));
				className = className.replace('\\', '.'); 
				try {
					Class<?> clazz = Class.forName(className); 
					if(annotation == null){
						result.add(clazz);
						continue;
					}
					Annotation ann = clazz.getAnnotation(annotation);
					if(ann != null){
						result.add(clazz);
					}
				} catch (ClassNotFoundException e) {
					//ignore
				} 
				
	        } else if (file.isDirectory()) { 
	        	list(root, file, result, annotation);
	        } 
		}
	}
}
