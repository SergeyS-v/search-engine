package my.searchengine.controller;

import my.searchengine.AppProp;
import my.searchengine.controller.responses.ErrorResponse;
import my.searchengine.controller.responses.RequestResponse;
import my.searchengine.controller.responses.Response;
import my.searchengine.controller.responses.StatisticsResponse;
import my.searchengine.dao.DaoController;
import my.searchengine.model.Query;
import my.searchengine.model.QueryResult;
import my.searchengine.model.Site;
import my.searchengine.services.QueryController;
import my.searchengine.services.UrlReader;
import my.searchengine.services.checkers.UrlChecker;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.*;

import java.util.Iterator;
import java.util.Map;
import java.util.concurrent.ForkJoinPool;
import java.util.concurrent.TimeUnit;

@Controller
public class SearchEngineController {

    @Autowired
    DaoController daoController;
    @Autowired
    StatisticsResponse statistics;
    @Autowired
    UrlReader urlReader;
    @Autowired
    AppProp appProp;
    @Autowired
    QueryController queryController;
    private final int TIME_THRESHOLD = 30_000; // Условный порог, для определения источника ответа на запрос

    @RequestMapping("${SearchEngineController.apiPath}")
    public String getIndex() {
           return "admin";
    }

    @RequestMapping("/statistics")
    @ResponseBody
    public StatisticsResponse getStatistics() {
        statistics.updateStatistics();
        return statistics;
    }

    @RequestMapping("/startIndexing")
    @ResponseBody
    public Response startIndexing(){ // TODO: 16.09.2022 Уточнить постановку. Ошибки отображаются только без ResponseEntity, но в Этапе 7 есть требование: Такие ответы должны сопровождаться соответствующими статус-кодами.
        if (!appProp.isOnlyQueryMode()) {
            if(UrlReader.isIndexing.get()){
                return new ErrorResponse("Индексация уже запущена");
            } else {
                UrlReader.isIndexing.set(true);
                urlReader.startIndexing("");
                return new Response(true);
            }
        } else {
            return new ErrorResponse("Приложение работает в режиме поиска");
        }
    }

    @RequestMapping("/stopIndexing")
    @ResponseBody
    public ResponseEntity<Response> stopIndexing() {
        if (!UrlReader.isIndexing.get() && UrlReader.indexingStatusBySiteHost.values().stream().noneMatch(x -> x.equals(Site.Status.INDEXING))) {
            return ResponseEntity.badRequest().body(new ErrorResponse("Индексация не запущена"));
        } else {
            UrlReader.isIndexing.set(false);
            UrlReader.indexingStatusBySiteHost.forEach((host,status) -> { // TODO: 07.09.2022 Ошибка проиндексированному сайту не должна выставляться
                if (status == Site.Status.INDEXING) {
                    UrlReader.indexingStatusBySiteHost.put(host, Site.Status.FAILED);
                    daoController.getSiteDao().insertSite(new Site(Site.Status.FAILED, host, "updateStatus", "Indexing stopped by user"));
                    UrlReader.lastErrorForSite.put(host, "Indexing stopped by user");
                }
            });
            return ResponseEntity.ok(new Response(true));
        }
    }

    @PostMapping("/indexPage")
    @ResponseBody
    public Response indexPage(@RequestParam(name = "url") String url) {
        if (!appProp.isOnlyQueryMode()) {
            String host = url;
            String[] urlParts = url.trim().split("/", 4);
            if (url.matches(UrlChecker.siteNameRegexFull)) {
                host = urlParts[2];
            }
            if (appProp.getHostToSiteNameMap().get(host) == null) {
             return new ErrorResponse("Данная страница находится за пределами сайтов, указанных в конфигурационном файле");
            } else if (urlParts.length > 2) {
                String path = "/".concat(urlParts[3]);
                daoController.clearPageInfo(host, path);
                urlReader.indexOnePage(url);
                return new Response(true);
            } else if (!UrlReader.isIndexing.get() && UrlReader.indexingStatusBySiteHost.get(host) != Site.Status.INDEXING) {
                urlReader.updateStatusForSite(host, Site.Status.INDEXING);
                if (UrlReader.indexingStatusBySiteHost.values().stream().filter(x -> x != Site.Status.INDEXING).findFirst().isEmpty()) {
                    UrlReader.isIndexing.set(true);
                }
                urlReader.startIndexing(host);
                return new Response(true);
            } else {
                return new ErrorResponse("Данный сайт уже индексируется");
            }
        } else {
            return new ErrorResponse("Приложение работает в режиме поиска");
        }
    }

    @GetMapping("/search")
    @ResponseBody
    public RequestResponse search(@RequestParam(name = "site", defaultValue = "default") String host,
                       @RequestParam String query, @RequestParam(defaultValue = "0") Integer offset, @RequestParam(defaultValue = "${SearchEngineController.defaultResponseLimit}") Integer limit) {
       Integer siteId = host.matches("default") ? null : daoController.getSiteDao().getSiteIdByHost(host);
       Query newQuery = new Query(query, siteId);
       QueryResult queryResult = queryController.getQueryQueryResultMap().get(newQuery);
       if (queryResult != null && (newQuery.getQueryTime() - queryResult.getQueryTime() < TIME_THRESHOLD)) { // TODO: 12.09.2022 Подумать над условием
           return RequestResponse.makeRequestResponse(queryResult, daoController.getPageDao(), appProp, offset, limit);
       } else {
           queryResult = queryController.performNewQuery(newQuery);
           queryController.getQueryQueryResultMap().put(newQuery, queryResult);
       }
        // TODO: 12.09.2022  Подумать как чистить queryController в отдельном потоке;
        ForkJoinPool.commonPool().execute(new Runnable() {
            @Override
            public void run() {
                Iterator<Map.Entry<Query, QueryResult>> queryIterator = queryController.getQueryQueryResultMap().entrySet().iterator();
                while (queryIterator.hasNext()) {
                    Map.Entry<Query, QueryResult> entry = queryIterator.next();
                    if(System.currentTimeMillis() - entry.getValue().getQueryTime() > TIME_THRESHOLD) {
                        queryIterator.remove();
                    }
                }
            }
        });
       return RequestResponse.makeRequestResponse(queryResult, daoController.getPageDao(), appProp, offset, limit);
    }
}
