package my.searchengine.model;

import lombok.Getter;
import lombok.Setter;

import java.util.*;

@Getter @Setter
public class QueryResult {
    private final String query;
    private final long queryTime;
    private HashMap<String, Lemma> lemmasHashMap;
    private HashMap<Integer, Float> relPageIdRelevanceMap = new HashMap<>();
    private final Map<Integer, String> pageIdSnippetMap = new HashMap<>();

    public QueryResult(Query query) {
        this.query = query.getQuery();
        this.queryTime = query.getQueryTime();
    }

    public void addSnippet(Integer pageId, String snippet){
        if (!snippet.isBlank()) {
            this.pageIdSnippetMap.put(pageId, snippet);
        }
    }
}

