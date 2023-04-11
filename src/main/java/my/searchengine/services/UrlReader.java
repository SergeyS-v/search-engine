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
import org.springframework.transaction.annotation.Transactional;

import javax.annotation.PreDestroy;
import javax.net.ssl.*;
import java.io.IOException;
import java.net.SocketTimeoutException;
import java.net.UnknownHostException;
import java.security.SecureRandom;
import java.security.cert.CertificateException;
import java.security.cert.X509Certificate;
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
    public static final ConcurrentHashMap<String, Site.Status> indexingStatusBySiteHost = new ConcurrentHashMap<>();
    public static final ConcurrentHashMap<String, Site> siteToWorkMap = new ConcurrentHashMap<>(); // TODO: 22.11.2022 Точно ли нужно конкарент? Точно ли нужно Map?
    public static final ConcurrentHashMap<String, String> lastErrorForSite = new ConcurrentHashMap<>();

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
                if (isIndexing.get() || UrlReader.indexingStatusBySiteHost.get(x.getHost()) == Site.Status.INDEXING) {
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
            if (!appProp.getNotIndexedPagesCodes().contains(responseCode)) {
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

    @Transactional
    public void startIndexing(String host) { // TODO: 22.11.2022 Проверять
        logger.info("Параметры приложения. Кол-во сайтов: " + appProp.getSites().size() +
                " CONNECTION_TIMEOUT: " + appProp.getConnectionTimeout() +
                " PAGE_QUEUE_INSERT_SIZE: " + appProp.getPageQueueInsertSize() +
                " urlSiteNameRegex: " + String.join(", ", urlChecker.getUrlSiteNameRegexes()));

        double start = System.currentTimeMillis();
        if (host.isBlank()) {
            siteToWorkMap.values().forEach(siteToWork -> {
                //  appProp.getSites().forEach(siteFromAppPropSitesList -> {
                if (siteToWork.getStatus() != Site.Status.INDEXING && isIndexing.get()) {
                    updateStatusForSite(siteToWork, Site.Status.INDEXING); // TODO: 21.11.2022 Проверяю этот метод
//                    lastErrorForSite.put(siteToWork.getHost(), "");
//                    siteToWork.setLastError("");
                    forkJoinPool.invoke(new RecursiveReader(new URL(appProp.getHostToSiteUrlMap().get(siteToWork.getHost())), false));
                    if (siteToWork.getStatus().equals(Site.Status.INDEXING)) {
                        siteToWork.setStatus(Site.Status.INDEXED);
                    }
                }
            });
        } else {
            Site siteToWork = siteToWorkMap.get(host);
//            siteToWork.setLastError("");
            // lastErrorForSite.put(host, "");
            forkJoinPool.invoke(new RecursiveReader(new URL(appProp.getHostToSiteUrlMap().get(host)), false));
            if (siteToWork.getStatus().equals(Site.Status.INDEXING)) {
                siteToWork.setStatus(Site.Status.INDEXED);
            }
        }
        isIndexing.set(false);
        pagesController.insertPageQueue();
        logger.info("Время выполнения индексации " + (System.currentTimeMillis() - start) + " мс.");
    }

    @org.springframework.transaction.annotation.Transactional
    public void initSiteTable(Site.Status status, String errorMessage) {
        List<AppProp.Sites> failedSites = new LinkedList<>(); // TODO: 22.11.2022 Убираем?
        trustEveryone();
        appProp.getSites().forEach(siteToWork -> {
            Document jDoc = connectToUrl(siteToWork.getUrl(), siteToWork.getHost());
            if (jDoc != null) {
                String host = jDoc.connection().response().url().getHost();
                Site site = errorMessage.isBlank() ? new Site(status, host, siteToWork.getName()) :
                        new Site(status, host, siteToWork.getName(), errorMessage);
                indexingStatusBySiteHost.put(site.getHost(), site.getStatus());
                siteToWorkMap.put(site.getHost(), site);
                siteRepository.save(site);
            } else {
                String error = "Не удалось соединиться с узлом " + siteToWork.getUrl();
                logger.error(error);
                Site site = new Site(Site.Status.FAILED, siteToWork.getUrl(), siteToWork.getName(), error);
                failedSites.add(siteToWork); // TODO: 22.11.2022 Убираем?
                siteRepository.save(site);
            }
        });
        failedSites.forEach(appProp.getSites()::remove); // TODO: 22.11.2022 Нужно ли это делать, после того, как я положил работающие сайты в отдельную мапу
    }

    @Transactional
    public void updateStatusForSite(Site site, Site.Status status) {
        UrlReader.indexingStatusBySiteHost.put(site.getHost(), status);
        if (status == Site.Status.INDEXING) {
//            daoController.clearSiteInfo(host);
            site.getPageList().clear();
            site.getLemmaList().clear();
        }
        site.setStatus(status);
        //   daoController.getSiteDao().insertSite(new Site(status, host,"updateStatus"));
    }

    public void indexOnePage(String url) {
        forkJoinPool.execute(new RecursiveReader(new URL(url), true));
    }

    @Transactional
    public void testJpa() {
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

    public static void trustEveryone() {
        try {
            HttpsURLConnection.setDefaultHostnameVerifier(new HostnameVerifier() {
                public boolean verify(String hostname, SSLSession session) {
                    return true;
                }
            });

            SSLContext context = SSLContext.getInstance("TLS");
            context.init(null, new X509TrustManager[]{new X509TrustManager() {
                public void checkClientTrusted(X509Certificate[] chain, String authType) throws CertificateException {
                }

                public void checkServerTrusted(X509Certificate[] chain, String authType) throws CertificateException {
                }

                public X509Certificate[] getAcceptedIssuers() {
                    return new X509Certificate[0];
                }
            }}, new SecureRandom());
            HttpsURLConnection.setDefaultSSLSocketFactory(context.getSocketFactory());
        } catch (Exception e) {
            // e.printStackTrace();
        }
    }

}
