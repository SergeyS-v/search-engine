package my.searchengine.model;

import java.util.Objects;

public class Query {
    private String query;
    private Integer siteId;
    private long queryTime;

    public Query(String query, Integer siteId) {
        this.query = query;
        this.siteId = siteId;
        this.queryTime = System.currentTimeMillis();
    }
    public String getQuery() {
        return query;
    }
    public void setQuery(String query) {
        this.query = query;
    }
    public Integer getSiteId() {
        return siteId;
    }
    public void setSiteId(Integer siteId) {
        this.siteId = siteId;
    }
    public long getQueryTime() {
        return queryTime;
    }
    public void setQueryTime(long queryTime) {
        this.queryTime = queryTime;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof Query)) return false;
        Query query1 = (Query) o;
        return Objects.equals(query, query1.query) && Objects.equals(siteId, query1.siteId);
    }

    @Override
    public int hashCode() {
        return Objects.hash(query, siteId);
    }
}
