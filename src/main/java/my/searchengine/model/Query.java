package my.searchengine.model;

import lombok.Getter;
import lombok.Setter;

import java.util.Objects;

@Getter @Setter
public class Query {
    private String query;
    private Integer siteId;
    private long queryTime;

    public Query(String query, Integer siteId) {
        this.query = query;
        this.siteId = siteId;
        this.queryTime = System.currentTimeMillis();
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
