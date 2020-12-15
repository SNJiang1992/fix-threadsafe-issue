package com.github.hank;

import org.apache.ibatis.io.Resources;
import org.apache.ibatis.session.SqlSession;
import org.apache.ibatis.session.SqlSessionFactory;
import org.apache.ibatis.session.SqlSessionFactoryBuilder;

import java.io.IOException;
import java.io.InputStream;
import java.util.HashMap;
import java.util.Map;

public class MyBatisCrawlerDao implements CrawlerDao{

    private SqlSessionFactory sqlSessionFactory;

    public MyBatisCrawlerDao() {
        try{
            String resource = "mybatis/mybatis-config.xml";
            InputStream inputStream = Resources.getResourceAsStream(resource);
            sqlSessionFactory = new SqlSessionFactoryBuilder().build(inputStream);
        }catch (Exception e){
            throw new RuntimeException(e);
        }

    }

    @Override
    public synchronized String getNextLinkThenDelete() {
        try (SqlSession session = sqlSessionFactory.openSession(true)) {
            String url =  session.selectOne("myMapper.selectNextAvailableLink");
            if(url!=null){
                session.delete("myMapper.deleteLink",url);
            }
            return url;
        }
    }


    @Override
    public void insertNewsIntoDatabase(String url, String title, String content) {
        try (SqlSession session = sqlSessionFactory.openSession(true)) {
            session.insert("myMapper.insertNews",new News(url,title,content));
        }
    }

    @Override
    public boolean isLinkProcessed(String link) {
        try (SqlSession session = sqlSessionFactory.openSession(true)) {
           int count = session.selectOne("myMapper.countLink",link);
           return count !=0;
        }
    }

    @Override
    public void insertProcessedLink(String link){
        Map<String,Object> param = new HashMap<>();
        param.put("tableName","links_already_processed");
        param.put("link",link);
        try (SqlSession session = sqlSessionFactory.openSession(true)) {
            session.insert("myMapper.insertLink",param);
        }
    }

    @Override
    public void insertToBeProcessedLink(String link){
//        Map<String,Object> param = new HashMap<>();
//        param.put("tableName","links_to_be_processed");
//        param.put("link",link);
        try (SqlSession session = sqlSessionFactory.openSession(true)) {
//            session.insert("myMapper.insertLink",param);
            session.insert("myMapper.insertToProcessed",link);
        }
    }
}
