package com.github.hank;

public interface CrawlerDao {
    String getNextLinkThenDelete();
    void insertNewsIntoDatabase(String url, String title, String content);
    boolean isLinkProcessed(String link);
    void insertProcessedLink(String link);
    void insertToBeProcessedLink(String link);
}
