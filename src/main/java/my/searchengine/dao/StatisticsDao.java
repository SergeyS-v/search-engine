package my.searchengine.dao;

import lombok.AllArgsConstructor;
import my.searchengine.controller.responses.StatisticsResponse;
import org.springframework.jdbc.core.BeanPropertyRowMapper;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

import java.util.List;

@AllArgsConstructor
@Repository
public class StatisticsDao {

    private final JdbcTemplate jdbcTemplate;

    public List<StatisticsResponse.Statistics.Detailed> getDetailedStatistics(){
        String stmt = "select site.url, site.name, site.status, site.status_time as statusTime, site.last_error as error, count(`index`.lemma_id) as lemmas, count(distinct `index`.page_id) as pages\n" +
                "from site left join page on site.id = page.site_id\n" +
                "left join `index` on page.id = `index`.page_id\n" +
                "group by site.id;";
        return jdbcTemplate.query(stmt, new BeanPropertyRowMapper<>(StatisticsResponse.Statistics.Detailed.class));
    }
}
