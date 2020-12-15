package com.github.hank;

import org.apache.http.HttpEntity;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClients;
import org.apache.http.util.EntityUtils;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import java.io.IOException;
import java.sql.*;
import java.util.ArrayList;
import java.util.stream.Collectors;

public class Crawler extends Thread{
   CrawlerDao dao;
    public Crawler(CrawlerDao dao){
        this.dao = dao;
    }
    @Override
    public void run() {
        try {
            String link;
            while ((link = dao.getNextLinkThenDelete()) != null) {
                if (dao.isLinkProcessed(link)) {
                    continue;
                }

                if (isInterestingLink(link)) {
                    CloseableHttpClient httpclient = HttpClients.createDefault();
                    if (link.startsWith("//")) {
                        link = "https:" + link;

                    }
                    System.out.println(link);
                    HttpGet httpGet = new HttpGet(link);
                    httpGet.addHeader("user-agent", "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/87.0.4280.88 Safari/537.36");
                    try (CloseableHttpResponse response = httpclient.execute(httpGet)) {
                        HttpEntity entity1 = response.getEntity();
                        // do something useful with the response body
                        // and ensure it is fully consumed
                        String html = EntityUtils.toString(entity1);
                        Document doc = Jsoup.parse(html);
                        handleDoc(doc, link);
                    }
                } else {
                    continue;
                }
            }
        }catch (Exception e){
            throw new RuntimeException(e);
        }
    }

    public void handleDoc(Document doc,String link){
        ArrayList<Element> links = doc.select("a");
        for (Element a : links) {
            String href = a.attr("href");
            if (href.startsWith("//")) {
                href = "https:" + href;
            }
            if(href.toLowerCase().startsWith("javascript")){
                continue;
            }
            dao.insertToBeProcessedLink(href);
        }
        ArrayList<Element> articleTags = doc.select("article");
        if (!articleTags.isEmpty()) {
            for (Element articleTag : articleTags
            ) {
                String title = articleTags.get(0).child(0).text();
                String content = articleTag.select("p").stream().map(Element::text).collect(Collectors.joining("\n"));
                dao.insertNewsIntoDatabase(link,title,content);
            }
        }
        dao.insertProcessedLink(link);
    }


    private static boolean isInterestingLink(String link) {
        return link.contains("sina.cn") && !link.contains("passport.sina.cn") && link.contains("news.sina.cn") || link.equals("https://sina.cn/");
    }
}
