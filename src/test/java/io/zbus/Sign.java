package io.zbus;

import java.security.MessageDigest;
import java.util.HashMap;
import java.util.Map;
import java.util.Map.Entry;

import com.alibaba.fastjson.JSON;

import java.util.SortedMap;
import java.util.TreeMap;

public class Sign {
	private static final char[] HEX_DIGITS = { '0', '1', '2', '3', '4', '5', '6', '7', '8', '9', 'a', 'b', 'c', 'd','e', 'f' };
	private static String hexString(byte[] bytes) {
		int len = bytes.length;
		StringBuilder buf = new StringBuilder(len * 2);
		for (int j = 0; j < len; j++) {
			buf.append(HEX_DIGITS[(bytes[j] >> 4) & 0x0f]);
			buf.append(HEX_DIGITS[bytes[j] & 0x0f]);
		}
		return buf.toString();
	}

	public static Map<String, Object> sign(Map<String, Object> data, String apiKey, String apiSecret) throws Exception {
		SortedMap<String, Object> sortedData = new TreeMap<String, Object>(data);
		sortedData.put("apiKey", apiKey);
		sortedData.put("timestamp", System.currentTimeMillis());
		String signString = "";
		for (Entry<String, Object> e : sortedData.entrySet()) {
			signString += e.getKey() + "=" + e.getValue() + "&";
		}
		signString += "apiSecret="+apiSecret;
		MessageDigest messageDigest = MessageDigest.getInstance("SHA1");
		messageDigest.update(signString.getBytes("utf8"));
		String signed = hexString(messageDigest.digest());
		sortedData.put("sign", signed);
		System.out.println(signed);
		return sortedData;
	}
	

	public static void main(String[] args) throws Exception {
		/*
		 *  {"deviceId":"rSLBWTZzHZrFsjwuFwb2","apiKey":"22b8f89b45f276443cbf6a378bd96926","timestamp":"1576206092757","sign":"1f26ade346e7370af1d007c1c8eba19aaf59a82c"}
		 */
		//1）业务数据
		Map<String, Object> data = new HashMap<String, Object>();
		data.put("deviceId", "rSLBWTZzHZrFsjwuFwb2");
		//data.put("startTime", 15915973818L);
		
		String apiKey = "22b8f89b45f276443cbf6a378bd96926";
		String apiSecret = "b7beb1b52cb7c26c80c89ee6fec12e72a9c5b0bd";
		//2）签名
		sign(data, apiKey, apiSecret);
		
		String jsonString = JSON.toJSONString(data);
		//3）发送JSON数据，取决你用啥HTTPClient
		
	}

}
