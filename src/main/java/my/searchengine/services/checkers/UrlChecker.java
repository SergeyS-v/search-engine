package my.searchengine.services.checkers;

import my.searchengine.model.URL;
import my.searchengine.AppProp;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import javax.annotation.PostConstruct;
import java.util.HashSet;
import java.util.Set;
import java.util.stream.Collectors;

@Service
public class UrlChecker {

    @Autowired
    AppProp appProp;

    static final Logger logger = LoggerFactory.getLogger("badUrlLogger");

    //----- Regexes ------
    public static final String toFilesUrlRegex = ".+\\.\\w{2,4}$"; //Ссылки на файлы для отбраковки. Проверяем по наличию расширения в пути
    public static String toDomainNotFilesRgx; // = ".+(\\.(ru)|\\.(com)|\\.(pro)|\\.(html)|\\.(php))$";
    public static final String siteNameRegexFull = "http(s)?://(\\w+\\.)?\\w+\\.\\w+(/.*)?"; //Проверка на общий формат представленной к обработке ссылки, которая должна включать протокол и хост
    private static final Set<String> urlHostNameRegexes = new HashSet<>(); //для проверки ссылок на требуемый хост

    public Set<String> getUrlSiteNameRegexes(){
        return urlHostNameRegexes;
    }

    public void markBadUrlInSet(Set<URL> urlSet){
        if (urlSet.size() == 0) {
            return;
        }
        for (URL url : urlSet) {
            if (url.getUrl().matches(toFilesUrlRegex) && !url.getUrl().matches(toDomainNotFilesRgx)) {
                url.setBad(true);
                logger.info(url.getUrl());
            } else if (!urlHostNameRegexes.contains(url.getHost())) {
                url.setBad(true);
                logger.info(url.getUrl());
            }
        }
    }

    @PostConstruct
    private void createMainURLRegex() {
        appProp.getSites().forEach(site -> {
            if(site.getUrl().matches(siteNameRegexFull)) {
                String hostName = site.getUrl().split("/")[2].intern();
                urlHostNameRegexes.add(hostName);
                site.setHost(hostName);
                appProp.addHostToSiteNameValue(hostName, site.getName());
                appProp.getHostToSiteUrlMap().put(hostName, site.getUrl());
            } else {
                logger.error("Указанный для индексации url сайта (" + site.getUrl() + ") не соответствует формату.");
            }
        });

        toDomainNotFilesRgx = ".+(\\.(" + appProp.getDomainNotFilesList().stream().collect(Collectors.joining(")|\\.(")) + "))$";
    }
}
