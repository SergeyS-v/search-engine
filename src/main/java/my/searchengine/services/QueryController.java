package my.searchengine.services;

import lombok.AllArgsConstructor;
import my.searchengine.AppProp;
import my.searchengine.dao.DaoController;
import my.searchengine.model.Lemma;
import my.searchengine.model.Page;
import my.searchengine.model.Query;
import my.searchengine.model.QueryResult;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.PrintWriter;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;

@AllArgsConstructor
@Service
public class QueryController {
    private final Lemmatizer lemmatizer;
    private final DaoController daoController;
    private final AppProp appProp;

    private static final Logger logger = LoggerFactory.getLogger(QueryController.class);
    private final ConcurrentHashMap<Query, QueryResult> queryQueryResultMap = new ConcurrentHashMap<>();

    public ConcurrentHashMap<Query, QueryResult> getQueryQueryResultMap() {
        return queryQueryResultMap;
    }

    public QueryResult performNewQuery(Query query){
        QueryResult newQueryResult = new QueryResult(query);
        newQueryResult.setLemmasHashMap(lemmatizer.getLemmasFromString(query.getQuery()));
        setPagesRankForQuery(newQueryResult, query.getSiteId());
        createQueryResult(newQueryResult);
        printQuerySnippets(newQueryResult);
        return newQueryResult;
    }

    public void setPagesRankForQuery(QueryResult query, Integer siteId){
        List<Lemma> lemmaListFrequencySorted = daoController.getLemmaDao().getSameLemmaFromDBFrequencySorted(query.getLemmasHashMap().values(), siteId);
        Integer pageQuantity = daoController.getIndexDao().getPageQuantity();
        if (pageQuantity != null && pageQuantity > appProp.getMinPageQuantityToOptimizeLemmas()) {
            List<Integer> lemmasIdsWithTooManyPages = daoController.getIndexDao().getLemmasIdsWithTooManyPages();
            if (lemmaListFrequencySorted.stream().allMatch(x -> lemmasIdsWithTooManyPages.contains(x.getId()))) {
                lemmaListFrequencySorted = lemmaListFrequencySorted.stream().limit(appProp.getLimitForAllLemmasAreTooPopular()).collect(Collectors.toList());
            } else {
                lemmaListFrequencySorted.removeIf(x -> (lemmasIdsWithTooManyPages.contains(x.getId())));
            }
        }
        List<Integer> pagesIdForQuery = daoController.getIndexDao().getPagesIdForLemmaList(lemmaListFrequencySorted);
        HashMap<Integer, Float> absRelevanceMap = daoController.getIndexDao().getLemmasRankForPages(pagesIdForQuery, lemmaListFrequencySorted);
        Optional<Float> maxRelevanceOpt = absRelevanceMap.values().stream().max(Float::compareTo);
        if (maxRelevanceOpt.isPresent()) {
            HashMap<Integer, Float> relRelevanceMap = new HashMap<>();
            absRelevanceMap.forEach((pageId, absRel) -> relRelevanceMap.put(pageId, absRel / maxRelevanceOpt.get()));
            query.setRelPageIdRelevanceMap(relRelevanceMap);
        }
    }

    public void createQueryResult(QueryResult query) {
        List<Page> pagesForResult = daoController.getPageDao().getPagesByIds(query.getRelPageIdRelevanceMap().keySet());
        for (Page page : pagesForResult){
            makeAndSetSnippetToQuery(query, page);
        }
    }

    public void makeAndSetSnippetToQuery(QueryResult query, Page page){
        Document jDoc = Jsoup.parse(page.getContent());
        String body = jDoc.body().text();
        List<String> snippetStrList =
                body.lines().flatMap(x -> splitStrForSnippets(x).stream())
                .map(x -> stringForSnippet(x, query))
                .filter(x -> !x.isBlank())
                .limit(appProp.getStringPartsInSnippet())
                .collect(Collectors.toList());
        query.addSnippet(page.getId(), createSnippetHtml(snippetStrList, jDoc));
    }

    private List<String> splitStrForSnippets(String string) {
        List<String> strings = new ArrayList<>();
        int index = 0;
        int startIndex = 0;
        boolean isSplitting = true;
        while (isSplitting) {
            for (int i = 0; i < appProp.getWordsInSnippet() && index >= 0; i++) {
                index = string.indexOf(" ", ++index);
            }
            if (index < 0) {
                index = string.length();
                isSplitting = false;
            }
            strings.add("..." + string.substring(startIndex, index) + "...");
            startIndex = index + 1;
        }
        return strings;
    }

    private String stringForSnippet(String string, QueryResult query) {
        String originalStr = string;
        string = string.replaceAll("\\p{Punct}" ,"");
        String[] wordArray = string.split(" ");
        HashSet<String> wordToBoldSet = new HashSet<>();
        for (String word : wordArray) {
            if(lemmatizer.isWord(word.toLowerCase())){
                HashMap<String, Lemma> lemmasForWord = lemmatizer.getLemmasFromString(word.replaceAll("ё", "е"));
                query.getLemmasHashMap().keySet().forEach(lemmaForQuery -> {
                    if (lemmasForWord.get(lemmaForQuery) != null) {
                        wordToBoldSet.add(word);
                    }
                });
            }
        }

        if (wordToBoldSet.isEmpty()) {
            return "";
        } else {
            for (String wordToBold : wordToBoldSet) {
                originalStr = originalStr.replaceAll(wordToBold, "<b>" + wordToBold + "</b>");
            }
            return originalStr;
        }
    }

    private String createSnippetHtml(List<String> snippetStrList, Document jDoc) {
        return String.format(
                "<p><b>Title: %s</b></p>\n" +
                "<p>%s</p>", jDoc.title(), String.join("\n", snippetStrList));
    }

    public void printQuerySnippets(QueryResult query){
        try {
            File fileHtml = new File("logs/querySnippet.html");
            PrintWriter writerHtml = new PrintWriter(fileHtml);
            StringBuilder snippet = new StringBuilder(String.format("<!DOCTYPE HTML>\n" +
                                                      "<meta charset=\"utf-8\">" +
                                                      "<p><b>Request: %s</b></p>\n", query.getQuery()));
            query.getPageIdSnippetMap().forEach((pageId, snippetForPage) -> snippet.append("\nPageId: ").append(pageId).append("\n").append("Snippet: ").append(snippetForPage));
            writerHtml.print(snippet);
            writerHtml.flush();
        } catch (FileNotFoundException e) {
            logger.warn(e.getMessage());
            e.printStackTrace();
        } catch (IllegalFormatException e) {
            logger.warn(e.getMessage() + " " + query.getQuery());
            e.printStackTrace();
        }
    }
}
