package my.searchengine.controller.responses;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.Setter;
import my.searchengine.AppProp;
import my.searchengine.dao.PageDao;
import my.searchengine.model.Page;
import my.searchengine.model.QueryResult;
import org.jsoup.Jsoup;

import java.util.*;

@Getter @Setter
public class RequestResponse extends Response {
    private int count;
    private List<SiteResponseInfo> data = new ArrayList<>();

    private RequestResponse(Boolean result) {
        super(result);
    }

    @Getter
    @AllArgsConstructor
    private static class SiteResponseInfo {
        private final String site;
        private final String siteName;
        private final String uri;
        private final String title;
        private final String snippet;
        private final Float relevance;
    }

    public static RequestResponse makeRequestResponse(QueryResult queryResult, PageDao pageDao, AppProp appProp, int offset, int limit) {
        RequestResponse requestResponse = new RequestResponse(true);
        List<Page> pageList = pageDao.getPagesByIds(queryResult.getRelPageIdRelevanceMap().keySet());

        TreeSet<SiteResponseInfo> sortedData = new TreeSet<>(new Comparator<SiteResponseInfo>() {
            @Override
            public int compare(SiteResponseInfo o1, SiteResponseInfo o2) {
                int result = o2.relevance.compareTo(o1.relevance);
                if (result == 0) {
                    result = -1;
                }
                return result;
            }
        });

        pageList.forEach(page -> {
            String site = page.getHostName();
            String siteName = appProp.getHostToSiteNameMap().get(site);
            String uri = page.getPath();
            String title = Jsoup.parse(page.getContent()).title();
            String snippet = queryResult.getPageIdSnippetMap().get(page.getId());
            Float relevance = queryResult.getRelPageIdRelevanceMap().get(page.getId());
            sortedData.add(new SiteResponseInfo(site, siteName, uri, title, snippet, relevance));
        });
        requestResponse.setCount(sortedData.size());
        int endIndex = Math.min((offset + limit), sortedData.size());
        requestResponse.setData(new ArrayList<>(sortedData).subList(offset, endIndex));
        return requestResponse;
    }
}
