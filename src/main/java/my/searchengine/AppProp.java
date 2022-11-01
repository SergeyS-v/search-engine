package my.searchengine;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

import javax.annotation.PostConstruct;
import java.util.*;

@Component
@ConfigurationProperties (prefix = "app-properties")
@Getter @Setter
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
    private int limitForAllLemmasAreTooPopular;
    private final Map<String, String> hostToSiteNameMap = new HashMap<>();
    private final Map<String, String> hostToSiteUrlMap = new HashMap<>();

    @Getter @Setter
    public static class Sites {
        private String url;
        private String name;
        private String host;
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

    public void addHostToSiteNameValue(String host, String siteName) {
        this.hostToSiteNameMap.put(host, siteName);
    }
}
