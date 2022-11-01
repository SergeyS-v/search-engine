package my.searchengine.dao;

import lombok.AllArgsConstructor;
import my.searchengine.model.Lemma;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.dao.DataAccessException;
import org.springframework.jdbc.core.BatchPreparedStatementSetter;
import org.springframework.jdbc.core.BeanPropertyRowMapper;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.PreparedStatementSetter;
import org.springframework.stereotype.Repository;

import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.util.*;
import java.util.stream.Collectors;

@AllArgsConstructor
@Repository
public class LemmaDao {
    private final JdbcTemplate jdbcTemplate;
    private static final Logger logger = LoggerFactory.getLogger(LemmaDao.class);

    public void insertLemmaBatch(Map<Lemma, Integer> lemmasFrequencyMap, Integer siteId){
        String stmt = "INSERT INTO lemma(lemma, frequency, site_id) VALUES (?, ?, ?)";
        ArrayList<Lemma> lemmasList = new ArrayList<>(lemmasFrequencyMap.keySet());
        class InsertLemmaBatch implements BatchPreparedStatementSetter {
            @Override
            public void setValues(PreparedStatement ps, int i) throws SQLException {
                ps.setString(1, lemmasList.get(i).getLemma());
                ps.setInt(2, lemmasFrequencyMap.get(lemmasList.get(i)));
                ps.setInt(3, siteId);
            }
            @Override
            public int getBatchSize() {
                return lemmasList.size();
            }
        }
        try {
            jdbcTemplate.batchUpdate(stmt, new InsertLemmaBatch());
        } catch (DataAccessException e) {
            logger.error("Ошибка БД в LemmaDao", e);
            e.printStackTrace();
        }
    }

    public void updateLemmaBatch(Collection<Lemma> lemmasCollection){
        if (lemmasCollection.isEmpty()) {
            return;
        }
        ArrayList<Lemma> lemmasList = new ArrayList<>(lemmasCollection);
        class UpdateLemmaBatch implements BatchPreparedStatementSetter {
            @Override
            public void setValues(PreparedStatement ps, int i) throws SQLException {
                ps.setInt(1, lemmasList.get(i).getFrequency());
                ps.setInt(2, lemmasList.get(i).getId());
            }
            @Override
            public int getBatchSize() {
                return lemmasCollection.size();
            }
        }
        try {
            String stmt = "update lemma set frequency = ? where id = ?";
            jdbcTemplate.batchUpdate(stmt, new UpdateLemmaBatch());
        } catch (DataAccessException e) {
            logger.error("Ошибка БД в LemmaDao", e);
            e.printStackTrace();
        }
    }

    public List<Lemma> getSameLemmaFromDBFrequencySorted(Collection<Lemma> lemmas, Integer siteId) {
        if (lemmas.isEmpty()) {
            return new LinkedList<>();
        }
        String stmt;
        if (siteId != null) {
            stmt = String.format("SELECT * FROM lemma WHERE lemma.lemma in (%s) AND site_id = (%d) ORDER BY frequency",
                    lemmas.stream()
                            .map(x -> "?")
                            .collect(Collectors.joining(", ")), siteId);
        } else {
            stmt = String.format("SELECT * FROM lemma WHERE lemma.lemma in (%s) ORDER BY frequency",
                    lemmas.stream()
                            .map(x -> "?")
                            .collect(Collectors.joining(", ")));
        }

        PreparedStatementSetter pss = new PreparedStatementSetter() {
            @Override
            public void setValues(PreparedStatement ps) throws SQLException {
                int i = 1;
                for (Lemma lemma : lemmas) {
                    ps.setString(i++, lemma.getLemma());
                }
            }
        };
        return jdbcTemplate.query(stmt, pss, new BeanPropertyRowMapper<>(Lemma.class));
    }

    public List<Lemma> getLemmasIdForPage(Collection<Lemma> lemmasForPage, int siteId){
        if (lemmasForPage.isEmpty()) {
            return new LinkedList<>();
        }

        String stmt = String.format("SELECT * FROM lemma WHERE lemma IN (%s) AND site_id = ?",
                lemmasForPage.stream()
                        .map(x -> "?")
                        .collect(Collectors.joining(", ")));


        PreparedStatementSetter pss = new PreparedStatementSetter() {
            @Override
            public void setValues(PreparedStatement ps) throws SQLException {
                int i = 1;
                for (Lemma lemma : lemmasForPage) {
                    ps.setString(i++, lemma.getLemma());
                }
                ps.setInt(i, siteId);
            }
        };
        return jdbcTemplate.query(stmt, pss, new BeanPropertyRowMapper<>(Lemma.class));
    }

    public void deleteLemmasForSite(String host) {
        String stmt = String.format("DELETE FROM lemma WHERE site_id = (SELECT id FROM site WHERE url LIKE \"%s\");", host);
        jdbcTemplate.execute(stmt);
    }
}
