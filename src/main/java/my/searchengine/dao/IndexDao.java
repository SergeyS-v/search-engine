package my.searchengine.dao;

import my.searchengine.AppProp;
import my.searchengine.model.Index;
import my.searchengine.model.Lemma;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.dao.DataAccessException;
import org.springframework.jdbc.core.BatchPreparedStatementSetter;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.PreparedStatementSetter;
import org.springframework.jdbc.core.ResultSetExtractor;
import org.springframework.stereotype.Repository;

import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.*;
import java.util.stream.Collectors;

@Repository
public class IndexDao {

    @Autowired
    JdbcTemplate jdbcTemplate;
    @Autowired
    AppProp appProp;

    private static final Logger logger = LoggerFactory.getLogger(IndexDao.class);

    public void insertIndexBatch(Collection<Index> indexCollection){
        ArrayList<Index> indexesList = new ArrayList<>(indexCollection);
        class InsertIndexBatch implements BatchPreparedStatementSetter {
            @Override
            public void setValues(PreparedStatement ps, int i) throws SQLException {
                ps.setInt(1, indexesList.get(i).getPageId());
                ps.setInt(2, indexesList.get(i).getLemmaId());
                ps.setFloat(3, indexesList.get(i).getRank());
            }
            @Override
            public int getBatchSize() {
                return indexesList.size();
            }
        }
        try {
            String stmt = "INSERT INTO `index`(page_id, lemma_id, rank) VALUES (?, ?, ?)";
            jdbcTemplate.batchUpdate(stmt, new InsertIndexBatch());
        } catch (DataAccessException e) {
            logger.error("Ошибка БД в IndexDao", e);
            e.printStackTrace();
        }
    }

    public List<Integer> getPagesIdForLemmaList(List<Lemma> lemmaListFrequencySorted) {
        if (lemmaListFrequencySorted.isEmpty()) {
            return new LinkedList<>();
        }
        HashMap<Integer, List<Integer>> siteIdPageIdListsMap = new HashMap<>();
        lemmaListFrequencySorted.forEach(lemma -> {
            siteIdPageIdListsMap.put(lemma.getSiteId(), null);
        });
        Map<Integer, List<Lemma>> siteIdLemmaListsMap = lemmaListFrequencySorted.stream().collect(Collectors.groupingBy(Lemma::getSiteId));
        String stmt = "SELECT page_id FROM `index` WHERE lemma_id = ?";
        siteIdPageIdListsMap.keySet().forEach(siteId -> {
            int lemmaId = lemmaListFrequencySorted.stream().filter(lemma -> lemma.getSiteId() == siteId).findFirst().get().getId();
            siteIdPageIdListsMap.put(siteId, jdbcTemplate.queryForList(stmt, Integer.class, lemmaId));
        });
        //Далее искать соответствия следующей леммы и этого списка страниц, и так по каждой следующей лемме.
        //Список страниц при этом на каждой итерации должен уменьшаться
        siteIdPageIdListsMap.keySet().forEach(siteId -> {
            for (int i = 1; i < siteIdLemmaListsMap.get(siteId).size(); i++) {
                if(siteIdPageIdListsMap.get(siteId).isEmpty()) {
                    break;
                }
                String stmt2 = String.format("SELECT page_id FROM `index` WHERE page_id IN (%s) AND lemma_id = ?",
                        siteIdPageIdListsMap.get(siteId).stream()
                                .map(Object::toString)
                                .collect(Collectors.joining(", ")));
                siteIdPageIdListsMap.put(siteId, jdbcTemplate.queryForList(stmt2, Integer.class, siteIdLemmaListsMap.get(siteId).get(i).getId()));
            }
        });
        return siteIdPageIdListsMap.values().stream().flatMap(Collection::stream).collect(Collectors.toList());
    }

    public HashMap<Integer, Float> getLemmasRankForPages(Collection<Integer> pagesIds, Collection<Lemma> lemmas) {
        if(pagesIds.isEmpty() || lemmas.isEmpty()) {
            return new HashMap<>();
        }
        String stmt = String.format("SELECT page_id, SUM(rank) AS absRelevance FROM `index` WHERE page_id IN (%s) AND lemma_id IN (%s) GROUP BY page_id",
                pagesIds.stream()
                        .map(x -> "?")
                        .collect(Collectors.joining(", "))
                , lemmas.stream()
                        .map(x -> "?")
                        .collect(Collectors.joining(", ")));

        PreparedStatementSetter pss = new PreparedStatementSetter() {
            @Override
            public void setValues(PreparedStatement ps) throws SQLException {
                int i = 1;
                for (Integer pageId : pagesIds) {
                    ps.setInt(i++, pageId);
                }
                for (Lemma lemma : lemmas) {
                    ps.setInt(i++, lemma.getId());
                }
            }
        };

        HashMap<Integer, Float> absRelevForPages = jdbcTemplate.query(stmt, pss, new ResultSetExtractor<HashMap<Integer, Float>>() {
            @Override
            public HashMap<Integer, Float> extractData(ResultSet rs) throws SQLException, DataAccessException {
                HashMap<Integer, Float> absRankMap = new HashMap<>();
                while (rs.next()){
                    absRankMap.put(rs.getInt("page_id"), rs.getFloat("absRelevance"));
                }
                return absRankMap;
            }
        });
        return absRelevForPages;
    }

    public Integer getPageQuantity(){
        return jdbcTemplate.queryForObject("SELECT COUNT(DISTINCT page_id) AS CNT FROM `index`;", Integer.class);
    }

    public List<Integer> getLemmasIdsWithTooManyPages(){
        String stmt = String.format("SELECT lemma_id FROM `index` GROUP BY lemma_id HAVING count(page_id) > (SELECT COUNT(DISTINCT page_id) * %s FROM `index`);",
                appProp.getPageQuantityCoefficientToOptimizeLemmas());
        return jdbcTemplate.queryForList(stmt, Integer.class);
    }
}
