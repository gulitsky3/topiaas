package io.zbus.rpc.spring;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.ibatis.session.SqlSession;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.support.ClassPathXmlApplicationContext;

import io.zbus.rpc.Template;
import io.zbus.rpc.annotation.Route;
import io.zbus.rpc.biz.HelpTopic;
import io.zbus.transport.Message;

public class RpcServerSpring {
	@Autowired
	Template template;
	
	@Autowired
	SqlSession sqlSession; 
	
	@Route("/")
	public Message home(Message req) {
		System.out.println(req);
		
		Map<String, Object> data = new HashMap<String, Object>();
        data.put("user", "Big Joe");   
        Map<String, Object> product = new HashMap<>();
        product.put("url", "/my");
        product.put("name", "Google");
        data.put("latestProduct", product); 
        
		return template.render("home.html", data); 
	}   
	
	public List<HelpTopic> db(){ 
		return sqlSession.selectList("io.zbus.rpc.biz.db.test"); 
	} 
	
	@SuppressWarnings("resource")
	public static void main(String[] args) throws Exception {  
		new ClassPathXmlApplicationContext("rpc/spring-server-remote.xml");      
	}

}
