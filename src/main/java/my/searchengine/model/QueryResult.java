package my.searchengine.model;

import java.util.*;

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

    public String getQuery() {
        return query;
    }
    public HashMap<String, Lemma> getLemmasHashMap(){
        return this.lemmasHashMap;
    }
    public void setLemmasHashMap(HashMap<String, Lemma> lemmasHashMap) {
        this.lemmasHashMap = lemmasHashMap;
    }
    public HashMap<Integer, Float> getRelPageIdRelevanceMap() {
        return this.relPageIdRelevanceMap;
    }
    public void setRelPageIdRelevanceMap(HashMap<Integer, Float> relPageIdRelevanceMap) {
        this.relPageIdRelevanceMap = relPageIdRelevanceMap;
    }
    public Map<Integer, String> getPageIdSnippetMap(){
        return this.pageIdSnippetMap;
    }
    public long getQueryTime(){
        return this.queryTime;
    }
    public void addSnippet(Integer pageId, String snippet){
        if (!snippet.isBlank()) {
            this.pageIdSnippetMap.put(pageId, snippet);
        }
    }
}

