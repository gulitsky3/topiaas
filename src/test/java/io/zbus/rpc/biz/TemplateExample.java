package io.zbus.rpc.biz;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.ibatis.session.SqlSession;
import org.springframework.beans.factory.annotation.Autowired;

import io.zbus.rpc.Template;
import io.zbus.rpc.annotation.Filter;
import io.zbus.rpc.annotation.Route;
import io.zbus.rpc.biz.model.HelpTopic;
import io.zbus.transport.Message;

 
@Filter("admin")
public class TemplateExample {
	@Autowired
	Template template;
	
	@Autowired
	SqlSession sqlSession; 
	
	@Route("/") 
	@Filter("logger")
	public Message home(Message req) { 
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
}
