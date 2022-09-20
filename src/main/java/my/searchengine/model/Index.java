package my.searchengine.model;

import java.util.Objects;

public class Index {

    private int id;
    private int pageId;
    private int lemmaId;
    private float rank;

    public Index(){}
    public Index(int pageId, int lemmaId, float rank){
        this.lemmaId = lemmaId;
        this.pageId = pageId;
        this.rank = rank;
    }

    public int getId() {
        return id;
    }
    public void setId(int id) {
        this.id = id;
    }
    public int getPageId() {
        return pageId;
    }
    public void setPageId(int pageId) {
        this.pageId = pageId;
    }
    public int getLemmaId() {
        return lemmaId;
    }
    public void setLemmaId(int lemmaId) {
        this.lemmaId = lemmaId;
    }
    public float getRank() {
        return rank;
    }
    public void setRank(float rank) {
        this.rank = rank;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof Index)) return false;
        Index index = (Index) o;
        return pageId == index.pageId && lemmaId == index.lemmaId;
    }

    @Override
    public int hashCode() {
        return Objects.hash(pageId, lemmaId);
    }
}
