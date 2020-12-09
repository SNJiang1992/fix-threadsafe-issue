package com.github.hank;

import org.apache.http.HttpEntity;
import org.apache.http.NameValuePair;
import org.apache.http.client.entity.UrlEncodedFormEntity;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClients;
import org.apache.http.message.BasicNameValuePair;
import org.apache.http.util.EntityUtils;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;

import java.io.IOException;
import java.sql.*;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

public class Main {

    private static String getNextLink(Connection connection, String sql) throws SQLException {
        String result = null;
        try (PreparedStatement statement = connection.prepareStatement(sql)) {
            ResultSet resultSet = statement.executeQuery();
            while (resultSet.next()) {
                result = resultSet.getString(1);
            }
        }
        return result;
    }

    public static void main(String[] args) throws IOException, SQLException {
        Connection connection = DriverManager.getConnection("jdbc:h2:file:E:/javaProjects/crawler/database");

        String link;
        while ((link = getNextLink(connection, "select link from links_to_be_processed limit 1")) != null) {

            updateDatabase(connection, link);


            if (isUrlProcessed(connection, link)) {
                continue;
            }


            if (isInterestingLink(link)) {
                CloseableHttpClient httpclient = HttpClients.createDefault();
                if (link.startsWith("//")) {
                    link = "https:" + link;
                }
                HttpGet httpGet = new HttpGet(link);
                httpGet.addHeader("user-agent", "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/87.0.4280.88 Safari/537.36");
                try (CloseableHttpResponse response = httpclient.execute(httpGet)) {
                    System.out.println(response.getStatusLine());
                    HttpEntity entity1 = response.getEntity();
                    // do something useful with the response body
                    // and ensure it is fully consumed
                    String html = EntityUtils.toString(entity1);
                    addLinksInDoc(link, html, connection);
                }
            } else {
                continue;
            }
        }


    }

    private static void updateDatabase(Connection connection, String link) throws SQLException {
        try (PreparedStatement statement = connection.prepareStatement("delete from links_to_be_processed where link = ?")) {
            statement.setString(1, link);
            statement.executeUpdate();
        }
    }

    private static boolean isUrlProcessed(Connection connection, String url) throws SQLException {
        boolean flag = false;

        try (PreparedStatement statement = connection.prepareStatement("select link from links_already_processed where link = ?")) {
            statement.setString(1, url);
            ResultSet resultSet = statement.executeQuery();
            while (resultSet.next()) {
                flag = true;
            }
        }
        return flag;
    }

    private static void addLinksInDoc(String link, String html, Connection connection) throws SQLException {
        Document doc = Jsoup.parse(html);
        ArrayList<Element> links = doc.select("a");
        for (Element a : links) {
            String href = a.attr("href");
            try (PreparedStatement statement = connection.prepareStatement("insert into LINKS_TO_BE_PROCESSED (LINK) values (?)")) {
                statement.setString(1, href);
                statement.executeUpdate();
            }
        }
        ArrayList<Element> articleTags = doc.select("article");
        if (!articleTags.isEmpty()) {
            for (Element articleTag : articleTags
            ) {
                String title = articleTags.get(0).child(0).text();
                System.out.println(title);
            }
        }

        try (PreparedStatement statement = connection.prepareStatement("insert into links_already_processed (LINK) values (?)")) {
            statement.setString(1, link);
            statement.executeUpdate();
        }
    }

    private static boolean isInterestingLink(String link) {
        return link.contains("sina.cn") && !link.contains("passport.sina.cn") && link.contains("news.sina.cn") || link.equals("https://sina.cn/");
    }
}
