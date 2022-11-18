package my.searchengine.model;

import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.jsoup.nodes.Document;

import javax.persistence.*;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Objects;

@Getter @Setter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@Entity
@Table(name = "page")
public class Page {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private int id;
    @Transient
    private int siteId;
    @ManyToOne
    @JoinColumn(name = "site_id", referencedColumnName = "id")
    private Site site;
    @Transient
    private String hostName;
    private String path;
    private int code;
    private String content;
    @Transient
    private HashMap<String, Lemma> titleLemmas;
    @Transient
    private HashMap<String, Lemma> bodyLemmas;
    @Transient
    private boolean isOnlyOnePageForIndexing;
    @ManyToMany(mappedBy = "pageList")
    private List<Lemma> lemmaList = new LinkedList<>();

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
