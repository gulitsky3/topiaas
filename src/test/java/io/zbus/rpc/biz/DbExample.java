package io.zbus.rpc.biz;

import java.util.List;

import org.apache.ibatis.session.SqlSession;
import org.springframework.beans.factory.annotation.Autowired;

import io.zbus.rpc.biz.model.HelpTopic;

 
public class DbExample {  
	
	@Autowired
	SqlSession sqlSession;
	
	public List<HelpTopic> test(){
		return sqlSession.selectList("io.zbus.rpc.biz.db.test"); 
	}
}
