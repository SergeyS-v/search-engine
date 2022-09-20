package my.searchengine;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

import javax.annotation.PostConstruct;
import java.util.*;

@Component
@ConfigurationProperties (prefix = "app-properties")
public class AppProp {

    private List<Sites> sites;
    private int connectionTimeout;
    private byte maxConnectionAttempt;
    private byte criticalDelayInIterationsToLog;
    private int pageQueueInsertSize;
    private float titleWeight;
    private float bodyWeight;
    private String userAgent;
    private String referrer;
    private float pageQuantityCoefficientToOptimizeLemmas;
    private int minPageQuantityToOptimizeLemmas;
    private int wordsInSnippet;
    private int stringPartsInSnippet;
    private List<String> wordInfoToExcept;
    private HashSet<Integer> notIndexedPagesCodes;
    private List<String> domainNotFilesList;
    private boolean onlyQueryMode;
    private final Map<String, String> hostToSiteNameMap = new HashMap<>();
    private final Map<String, String> hostToSiteUrlMap = new HashMap<>();

    public static class Sites {
        private String url;
        private String name;
        private String host;

        public String getUrl() {
            return url;
        }
        public void setUrl(String url) {
            this.url = url;
        }
        public String getName() {
            return name;
        }
        public void setName(String name) {
            this.name = name;
        }
        public String getHost() {
            return host;
        }
        public void setHost(String host) {
            this.host = host;
        }

        @Override
        public boolean equals(Object o) {
            if (this == o) return true;
            if (!(o instanceof Sites)) return false;
            Sites sites = (Sites) o;
            return this.url.equalsIgnoreCase(sites.url);
        }

        @Override
        public int hashCode() {                   //TODO Почему при перегрузке hashCode() перестаёт работать appProp.getSites().removeAll(failedSites) ?
            return Objects.hashCode(url);
        }
    }

    @PostConstruct
    private void correctSitesUrls(){
        sites.forEach(site -> site.setUrl(site.getUrl().trim().endsWith("/") ?
                site.getUrl().trim().substring(0, site.getUrl().length() - 1) : site.getUrl().trim()));
    }

    public List<Sites> getSites() {
        return this.sites;
    }
    public void setSites(List<Sites> sites) {
        this.sites = sites;
    }
    public int getConnectionTimeout() {
        return connectionTimeout;
    }
    public void setConnectionTimeout(int connectionTimeout) {
        this.connectionTimeout = connectionTimeout;
    }
    public byte getMaxConnectionAttempt() {
        return maxConnectionAttempt;
    }
    public void setMaxConnectionAttempt(byte maxConnectionAttempt) {
        this.maxConnectionAttempt = maxConnectionAttempt;
    }
    public byte getCriticalDelayInIterationsToLog() {
        return criticalDelayInIterationsToLog;
    }
    public void setCriticalDelayInIterationsToLog(byte criticalDelayInIterationsToLog) {
        this.criticalDelayInIterationsToLog = criticalDelayInIterationsToLog;
    }
    public int getPageQueueInsertSize() {
        return pageQueueInsertSize;
    }
    public void setPageQueueInsertSize(int pageQueueInsertSize) {
        this.pageQueueInsertSize = pageQueueInsertSize;
    }
    public float getTitleWeight() {
        return titleWeight;
    }
    public void setTitleWeight(float titleWeight) {
        this.titleWeight = titleWeight;
    }
    public float getBodyWeight() {
        return bodyWeight;
    }
    public void setBodyWeight(float bodyWeight) {
        this.bodyWeight = bodyWeight;
    }
    public String getUserAgent() {return userAgent;}
    public void setUserAgent(String userAgent) {this.userAgent = userAgent; }
    public String getReferrer() {
        return referrer;
    }
    public void setReferrer(String referrer) {
        this.referrer = referrer;
    }
    public float getPageQuantityCoefficientToOptimizeLemmas() {
        return pageQuantityCoefficientToOptimizeLemmas;
    }
    public void setPageQuantityCoefficientToOptimizeLemmas(float pageQuantityCoefficientToOptimizeLemmas) {
        this.pageQuantityCoefficientToOptimizeLemmas = pageQuantityCoefficientToOptimizeLemmas;
    }
    public int getMinPageQuantityToOptimizeLemmas() {
        return minPageQuantityToOptimizeLemmas;
    }
    public void setMinPageQuantityToOptimizeLemmas(int minPageQuantityToOptimizeLemmas) {
        this.minPageQuantityToOptimizeLemmas = minPageQuantityToOptimizeLemmas;
    }
    public int getWordsInSnippet() {
        return wordsInSnippet;
    }
    public void setWordsInSnippet(int wordsInSnippet) {
        this.wordsInSnippet = wordsInSnippet;
    }
    public int getStringPartsInSnippet() {
        return stringPartsInSnippet;
    }
    public void setStringPartsInSnippet (int stringPartsInSnippet) {
        this.stringPartsInSnippet = stringPartsInSnippet;
    }
    public List<String> getWordInfoToExcept() {
        return wordInfoToExcept;
    }
    public void setWordInfoToExcept(List<String> wordInfoToExcept) {
        this.wordInfoToExcept = wordInfoToExcept;
    }
    public Set<Integer> getNotIndexedPagesCodes() {
        return notIndexedPagesCodes;
    }
    public void setNotIndexedPagesCodes (HashSet<Integer> notIndexedPagesCodes) { this.notIndexedPagesCodes = notIndexedPagesCodes; }
    public List<String> getDomainNotFilesList() {return  domainNotFilesList;}
    public void setDomainNotFilesList (List<String> domainNotFilesList) {this.domainNotFilesList = domainNotFilesList;}
    public void setOnlyQueryMode (boolean onlyQueryMode) {
        this.onlyQueryMode = onlyQueryMode;
    }
    public boolean isOnlyQueryMode() {
        return onlyQueryMode;
    }
    public Map<String, String> getHostToSiteNameMap(){
        return this.hostToSiteNameMap;
    }
    public Map<String, String> getHostToSiteUrlMap() {
        return hostToSiteUrlMap;
    }

    public void addHostToSiteNameValue(String host, String siteName) {
        this.hostToSiteNameMap.put(host, siteName);
    }
}
