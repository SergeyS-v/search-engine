package my.searchengine.services;

import lombok.AllArgsConstructor;
import my.searchengine.AppProp;
import my.searchengine.dao.DaoController;
import my.searchengine.model.*;
import my.searchengine.repositories.LemmaRepository;
import my.searchengine.repositories.PageRepository;
import my.searchengine.repositories.SiteRepository;
import my.searchengine.services.checkers.UrlChecker;
import org.jsoup.HttpStatusException;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.select.Elements;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import javax.annotation.PreDestroy;
import javax.transaction.Transactional;
import java.io.IOException;
import java.net.SocketTimeoutException;
import java.net.UnknownHostException;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ForkJoinPool;
import java.util.concurrent.ForkJoinTask;
import java.util.concurrent.RecursiveAction;
import java.util.concurrent.atomic.AtomicBoolean;

@AllArgsConstructor
@Service
public class UrlReader {

    private final PagesController pagesController;
    private final UrlChecker urlChecker;
    private final AppProp appProp;
    private final Lemmatizer lemmatizer;
    private final DaoController daoController;

    private final SiteRepository siteRepository;
    private final LemmaRepository lemmaRepository;
    private final PageRepository pageRepository;

    private final ForkJoinPool forkJoinPool = new ForkJoinPool();
    public static final AtomicBoolean isIndexing = new AtomicBoolean();
    public static final ConcurrentHashMap <String, Site.Status> indexingStatusBySiteHost = new ConcurrentHashMap<>();
    public static final ConcurrentHashMap <String, String> lastErrorForSite = new ConcurrentHashMap<>();

    private static final Logger logger = LoggerFactory.getLogger(UrlReader.class);

    @PreDestroy
    private void closePool() {
        forkJoinPool.shutdown();
    }

    private class RecursiveReader extends RecursiveAction {
        private final URL startURL;
        private final String host;
        private final boolean isOnePageIndexing;

        public RecursiveReader(URL url, boolean isOnePageIndexing) {
            this.startURL = url;
            this.host = url.getHost();
            this.isOnePageIndexing = isOnePageIndexing;
        }

        @Override
        protected void compute() {
            List<ForkJoinTask<Void>> taskList = new LinkedList<>();
            getUrlsFromStartUrlAndMakePage().forEach(x -> {
                if(isIndexing.get() || UrlReader.indexingStatusBySiteHost.get(x.getHost()) == Site.Status.INDEXING) {
                    taskList.add(new RecursiveReader(x, false).fork());
                }
            });
            taskList.forEach(ForkJoinTask::join);
        }

        public Set<URL> getUrlsFromStartUrlAndMakePage() {
            Set<URL> urlSet = new HashSet<>();
            Document jDoc = connectToUrl(startURL.getUrl(), host);
            if (jDoc == null) {
                return new HashSet<>();
            }
            Page page = Page.createPageFromJDoc(jDoc);
            if (isOnePageIndexing) {
                page.setOnlyOnePageForIndexing(true);
            }
            Integer responseCode = jDoc.connection().response().statusCode();
            if(!appProp.getNotIndexedPagesCodes().contains(responseCode)) {
                page.setTitleLemmas(lemmatizer.getLemmasFromString(jDoc.title()));
                page.setBodyLemmas(lemmatizer.getLemmasFromString(jDoc.body().text()));
            }
            String host = jDoc.connection().response().url().getHost();
            Site site = new Site(UrlReader.indexingStatusBySiteHost.get(host), host, appProp.getHostToSiteNameMap().get(host));
            daoController.getSiteDao().insertSite(site);
            pagesController.addPage(page);

            if (!isOnePageIndexing) {
                Elements hrefArray = jDoc.getElementsByTag("a");
                hrefArray.forEach(x -> urlSet.add(new URL(x.attr("abs:href"))));

                urlChecker.markBadUrlInSet(urlSet);
                pagesController.cleanBadUrls(urlSet);
                pagesController.cleanDoublesInUrlSet(urlSet);
            }
            return urlSet;
        }
    }

    private Document connectToUrl(String url, String host) {
        Document jDoc = null;
        byte currentIteration = 1;
        boolean webException = true;
        do {
            try {
                jDoc = Jsoup.connect(url)
                        .userAgent(appProp.getUserAgent())
                        .timeout(appProp.getConnectionTimeout() * currentIteration)
                        .referrer(appProp.getReferrer())
                        .get();
                webException = false;
                if (currentIteration > appProp.getCriticalDelayInIterationsToLog()) {
                    logger.info("Страница получена с " + currentIteration + " попытки. Ожидание: " + appProp.getConnectionTimeout() * currentIteration + " мс.");
                }
            } catch (SocketTimeoutException | HttpStatusException ex) {
                currentIteration++;
            } catch (IllegalArgumentException ex) {
                UrlReader.lastErrorForSite.put(host, "IllegalArgumentException. URL: " + url);
                logger.error("Задан неверный формат URL. Пример формата: http://www.playback.ru/");
                return null;
            } catch (UnknownHostException e) {
                UrlReader.lastErrorForSite.put(host, "Не удалось соединиться с узлом " + url + " UnknownHostException");
                logger.error("Не удалось соединиться с узлом " + url + " UnknownHostException", e);
                e.printStackTrace();
                return null;
            } catch (IOException e) {
                UrlReader.lastErrorForSite.put(host, "Не удалось соединиться с узлом " + url + " IOException");
                logger.error("Не удалось соединиться с узлом " + url + " IOException", e);
            } catch (NullPointerException e) {
                UrlReader.lastErrorForSite.put(host, "Не удалось соединиться с узлом " + url + " NullPointerException");
                logger.error("Не удалось соединиться с узлом " + url + " NullPointerException", e);
                e.printStackTrace();
                return null;
            }
        } while (webException && currentIteration <= appProp.getMaxConnectionAttempt());

        if (jDoc == null) {
            UrlReader.lastErrorForSite.put(host, "Страница не получена. Превышен лимит кол-ва попыток соединения MAX_CONNECTION_ATTEMPT. URL: " + url);
            logger.warn("Страница не получена. Превышен лимит кол-ва попыток соединения MAX_CONNECTION_ATTEMPT. URL: " + url);
        }
        return jDoc;
    }

    public void startIndexing(String host){
        logger.info("Параметры приложения. Кол-во сайтов: " + appProp.getSites().size() +
                    " CONNECTION_TIMEOUT: " + appProp.getConnectionTimeout() +
                    " PAGE_QUEUE_INSERT_SIZE: " + appProp.getPageQueueInsertSize() +
                    " urlSiteNameRegex: " + String.join(", ", urlChecker.getUrlSiteNameRegexes()));

        double start = System.currentTimeMillis();
        if(host.isBlank()) {
            appProp.getSites().forEach(siteFromAppPropSitesList -> {
                if (UrlReader.indexingStatusBySiteHost.get(siteFromAppPropSitesList.getHost()) != Site.Status.INDEXING && isIndexing.get()) {
                    updateStatusForSite(siteFromAppPropSitesList.getHost(), Site.Status.INDEXING);
                    lastErrorForSite.put(siteFromAppPropSitesList.getHost(), "");
                    forkJoinPool.invoke(new RecursiveReader(new URL(siteFromAppPropSitesList.getUrl()), false));
                    if (UrlReader.indexingStatusBySiteHost.get(siteFromAppPropSitesList.getHost()).equals(Site.Status.INDEXING)) {
                        updateStatusForSite(siteFromAppPropSitesList.getHost(), Site.Status.INDEXED);
                    }
                }
            });
        } else {
            lastErrorForSite.put(host, "");
            forkJoinPool.invoke(new RecursiveReader(new URL(appProp.getHostToSiteUrlMap().get(host)), false));
            if (UrlReader.indexingStatusBySiteHost.get(host).equals(Site.Status.INDEXING)) {
                updateStatusForSite(host, Site.Status.INDEXED);
            }
        }
        isIndexing.set(false);
        pagesController.insertPageQueue();
        logger.info("Время выполнения индексации " + (System.currentTimeMillis() - start) + " мс.");
    }

    public void initSiteTable(Site.Status status, String errorMessage){
        List<AppProp.Sites> failedSites = new LinkedList<>();
        appProp.getSites().forEach(siteToWork -> {
            Document jDoc = connectToUrl(siteToWork.getUrl(), siteToWork.getHost());
            if (jDoc != null) {
                String host = jDoc.connection().response().url().getHost();
                Site site = errorMessage.isBlank() ? new Site(status, host, siteToWork.getName()) :
                        new Site(status, host, siteToWork.getName(), errorMessage);
                indexingStatusBySiteHost.put(site.getHost(), site.getStatus());
                daoController.getSiteDao().insertSite(site);
            } else {
                String error = "Не удалось соединиться с узлом " + siteToWork.getUrl();
                logger.error(error);
                Site site = new Site(Site.Status.FAILED, siteToWork.getUrl(), siteToWork.getName(), error);
                failedSites.add(siteToWork);
                daoController.getSiteDao().insertSite(site);
            }
        });
        failedSites.forEach(appProp.getSites()::remove);
    }

    public void updateStatusForSite(String host, Site.Status status){
        UrlReader.indexingStatusBySiteHost.put(host, status);
        if (status == Site.Status.INDEXING) {
            daoController.clearSiteInfo(host);
        }
        daoController.getSiteDao().insertSite(new Site(status, host,"updateStatus"));
    }

    public void indexOnePage(String url) {
        forkJoinPool.execute(new RecursiveReader(new URL(url), true));
    }

    @Transactional
    public void testJpa(){
        Site site = new Site(Site.Status.INDEXING, "hostTest", "nameTest");
        Page page = new Page("test_path", 100, "test_content", "test_hostname");
        site.getPageList().add(page);
        page.setSite(site);

        Lemma lemma = new Lemma("test", 100);
        lemma.setSite(site);
        page.getLemmaList().add(lemma);
        lemma.getPageList().add(page);


        siteRepository.save(site);
        pageRepository.save(page);
        lemmaRepository.save(lemma);
    }
}
