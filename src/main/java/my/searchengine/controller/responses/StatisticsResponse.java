package my.searchengine.controller.responses;

import com.fasterxml.jackson.annotation.JsonProperty;
import my.searchengine.dao.StatisticsDao;
import my.searchengine.services.UrlReader;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;

@Component
public class StatisticsResponse {
    @Autowired
    StatisticsDao statisticsDao;
    private boolean result;
    private Statistics statistics = new Statistics();

    public boolean isResult() {
        return result;
    }
    public void setResult(boolean result) {
        this.result = result;
    }
    public Statistics getStatistics() {
        return statistics;
    }
    public void setStatistics(Statistics statistics) {
        this.statistics = statistics;
    }

    public static class Statistics {
        private Total total = new Total();
        private List<Detailed> detailed = new ArrayList<>();

        public Total getTotal(){
            return total;
        }
        public void setTotal(Total total) {
            this.total = total;
        }
        public List<Detailed> getDetailed(){
            return detailed;
        }
        public void setDetailed(List<Detailed> detailed) {
            this.detailed = detailed;
        }

        private static class Total {
            private int sites;
            private int pages;
            private int lemmas;
            private boolean isIndexing;

            public int getSites() {
                return sites;
            }
            public void setSites(int sites) {
                this.sites = sites;
            }
            public int getPages() {
                return pages;
            }
            public void setPages(int pages) {
                this.pages = pages;
            }
            public int getLemmas() {
                return lemmas;
            }
            public void setLemmas(int lemmas) {
                this.lemmas = lemmas;
            }
            @JsonProperty("isIndexing")
            public boolean isIndexing() {
                return isIndexing;
            }
            public void setIndexing(boolean indexing) {
                isIndexing = indexing;
            }

        }
        public static class Detailed {
            private String url;
            private String name;
            private String status;
            private String statusTime;
            private String error;
            private int pages;
            private int lemmas;

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
            public String getStatus() {
                return status;
            }
            public void setStatus(String status) {
                this.status = status;
            }
            public String getStatusTime() {
                return statusTime;
            }
            public void setStatusTime(String statusTime) {
                this.statusTime = statusTime;
            }
            public String getError() {
                return error;
            }
            public void setError(String error) {
                this.error = error;
            }
            public int getPages() {
                return pages;
            }
            public void setPages(int pages) {
                this.pages = pages;
            }
            public int getLemmas() {
                return lemmas;
            }
            public void setLemmas(int lemmas) {
                this.lemmas = lemmas;
            }
        }
    }

    public void updateStatistics() {
        this.statistics.total.setIndexing(false);
        this.setResult(true);
        this.statistics.setDetailed(statisticsDao.getDetailedStatistics());
        this.statistics.total.setSites(statistics.detailed.size());
        this.statistics.total.setPages(this.statistics.detailed.stream().mapToInt(Statistics.Detailed::getPages).sum());
        this.statistics.total.setLemmas(this.statistics.detailed.stream().mapToInt(Statistics.Detailed::getLemmas).sum());
        this.statistics.total.setIndexing(UrlReader.isIndexing.get());
    }
}
