package my.searchengine.model;

import org.jsoup.nodes.Document;

import java.util.HashMap;
import java.util.Objects;

public class Page {

    private int id;
    private int siteId;
    private String hostName;
    private String path;
    private int code;
    private String content;
    private HashMap<String, Lemma> titleLemmas;
    private HashMap<String, Lemma> bodyLemmas;
    private boolean isOnlyOnePageForIndexing;

    public Page(){}
    public Page(String path, int code, String content, String hostName){
        this.path = path;
        this.code = code;
        this.content = content;
        this.hostName = hostName;
        this.isOnlyOnePageForIndexing = false;
    }

    public void setId(int id) {
        this.id = id;
    }
    public int getId() {
        return id;
    }
    public void setSiteId(int siteId) {
        this.siteId = siteId;
    }
    public int getSiteId() {
        return siteId;
    }
    public void setHostName(String hostName) {
        this.hostName = hostName;
    }
    public String getHostName() {
        return hostName;
    }
    public void setPath(String path) {
        this.path = path;
    }
    public String getPath() {
        return path;
    }
    public void setCode(int code) {
        this.code = code;
    }
    public int getCode() {
        return code;
    }
    public void setContent(String content) {
        this.content = content;
    }
    public String getContent() {
        return content;
    }
    public void setTitleLemmas(HashMap<String, Lemma> titleLemmas) {
        this.titleLemmas = titleLemmas;
    }
    public HashMap<String, Lemma> getTitleLemmas() {
        return this.titleLemmas;
    }
    public void setBodyLemmas(HashMap<String, Lemma> bodyLemmas) {
        this.bodyLemmas = bodyLemmas;
    }
    public HashMap<String, Lemma> getBodyLemmas() {
        return this.bodyLemmas;
    }
    public void setOnlyOnePageForIndexing(boolean onlyOnePageForIndexing) {
        isOnlyOnePageForIndexing = onlyOnePageForIndexing;
    }
    public boolean isOnlyOnePageForIndexing() {
        return isOnlyOnePageForIndexing;
    }
    public String getPathWithHostName(){
            return hostName  + path;
    }

    public static Page createPageFromJDoc(Document jDoc) {
        if (jDoc == null) {
            return null;
        }

        String content = jDoc.html();
        int responseCode = jDoc.connection().response().statusCode();
        String url = jDoc.connection().response().url().getPath().matches("") ? "/" : jDoc.connection().response().url().getPath();
        String hostName = jDoc.connection().response().url().getHost();
        return new Page(url, responseCode, content, hostName);
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof Page)) return false;
        Page page = (Page) o;
        return siteId == page.siteId && Objects.equals(hostName, page.hostName) && Objects.equals(path, page.path);
    }

    @Override
    public int hashCode() {
        return Objects.hash(siteId, hostName, path);
    }
}
