package io.zbus.mq.disk;

public class FileNameNormalizer {
	
	public static String escapeName(String name) {
		return name.replaceAll("[//]", "^");
	}
	
	public static String normalizeName(String name) {
		return name.replaceAll("[/^]", "/");
	} 
}
