package my.searchengine.controller.responses;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Getter;
import lombok.Setter;
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
    @Getter @Setter
    private boolean result;
    @Getter @Setter
    private Statistics statistics = new Statistics();

    @Getter @Setter
    public static class Statistics {
        private Total total = new Total();
        private List<Detailed> detailed = new ArrayList<>();

        @Getter @Setter
        private static class Total {
            private int sites;
            private int pages;
            private int lemmas;
            @JsonProperty("isIndexing")
            private boolean isIndexing;
        }

        @Getter @Setter
        public static class Detailed {
            private String url;
            private String name;
            private String status;
            private String statusTime;
            private String error;
            private int pages;
            private int lemmas;
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
