package my.searchengine.model;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.jsoup.nodes.Document;

import java.util.HashMap;
import java.util.Objects;

@Getter @Setter
@NoArgsConstructor
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

    public Page(String path, int code, String content, String hostName){
        this.path = path;
        this.code = code;
        this.content = content;
        this.hostName = hostName;
        this.isOnlyOnePageForIndexing = false;
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
