package my.searchengine.dao;

import my.searchengine.model.Page;
import my.searchengine.model.URL;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.BatchPreparedStatementSetter;
import org.springframework.jdbc.core.BeanPropertyRowMapper;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.PreparedStatementSetter;
import org.springframework.stereotype.Repository;

import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.LinkedList;
import java.util.List;
import java.util.stream.Collectors;

@Repository
public class PageDao {
    @Autowired
    JdbcTemplate jdbcTemplate;
    static final Logger logger = LoggerFactory.getLogger(PageDao.class);

    public void insertPageBatch(Collection<Page> pagesCollection){
        if (pagesCollection.isEmpty()) {
            return;
        }
        ArrayList<Page> pageList = new ArrayList<>(pagesCollection);
        class InsertPageBatch implements BatchPreparedStatementSetter{
            @Override
            public void setValues(PreparedStatement ps, int i) throws SQLException {
                ps.setString(1, pageList.get(i).getHostName());
                ps.setString(2, pageList.get(i).getPath());
                ps.setInt(3, pageList.get(i).getCode());
                ps.setString(4, pageList.get(i).getContent());
            }
            @Override
            public int getBatchSize() {
                return pagesCollection.size();
            }
        }
        try {
            String stmt = "insert into page(site_id, path, code, content) values ((SELECT id from site WHERE url like ?), ?, ?, ?)";
            jdbcTemplate.batchUpdate(stmt, new InsertPageBatch());
        } catch (Exception e) {
            logger.error("Ошибка БД в PageDao", e);
            e.printStackTrace();
        }
    }

    public List<URL> getUrlsWithSamePathInPage(Collection<URL> urls){
        if (urls.isEmpty()) {
            return new LinkedList<>();
        }

        String stmt = String.format("select url from (select concat(site.url, page.path) as url from page join site on page.site_id = site.id) t1 where url in (%s);",
                urls.stream()
                        .map(v -> "?")
                        .collect(Collectors.joining(", ")));

        PreparedStatementSetter pss = new PreparedStatementSetter() {
            @Override
            public void setValues(PreparedStatement ps) throws SQLException {
                int i = 1;
                for (URL url : urls) {
                    ps.setString(i++, url.getUrlWithoutProtocolAndParameters());
                }
            }
        };

        return jdbcTemplate.query(stmt, pss, new BeanPropertyRowMapper<>(URL.class));
    }

    public List<Page> getPagesPathsWithSamePathInPage(Collection<Page> pages){
        if (pages.isEmpty()) {
            return new LinkedList<>();
        }

        String stmt = String.format("select p.path, s.url as hostName from page p join site s on p.site_id = s.id where p.path in (%s) " +
                        "and p.site_id in (SELECT id from site where url in (%s));",
                pages.stream()
                        .map(x -> "?")
                        .collect(Collectors.joining(", ")),
                pages.stream()
                        .map(x -> "?")
                        .collect(Collectors.joining(", ")));
        PreparedStatementSetter pss = new PreparedStatementSetter() {
            @Override
            public void setValues(PreparedStatement ps) throws SQLException {
                int i = 1;
                for (Page page : pages) {
                    ps.setString(i++, page.getPath());
                }
                for (Page page : pages) {
                    ps.setString(i++, page.getHostName());
                }
            }
        };
        return jdbcTemplate.query(stmt, pss, new BeanPropertyRowMapper<>(Page.class));
    }

    public List<Page> getPagesIdFor(Collection<Page> pages){
        if (pages.isEmpty()) {
            return new LinkedList<>();
        }

        String stmt = String.format("select p.id, p.path, s.url as hostName from page p join site s on p.site_id = s.id where p.path in (%s) " +
                        "and p.site_id in (SELECT id from site where url in (%s));",
                pages.stream()
                        .map(x -> "?")
                        .collect(Collectors.joining(", ")),
                pages.stream()
                        .map(x -> "?")
                        .collect(Collectors.joining(", ")));

        PreparedStatementSetter pss = new PreparedStatementSetter() {
            @Override
            public void setValues(PreparedStatement ps) throws SQLException {
                int i = 1;
                for (Page page : pages) {
                    ps.setString(i++, page.getPath());
                }
                for (Page page : pages) {
                    ps.setString(i++, page.getHostName());
                }
            }
        };
        return jdbcTemplate.query(stmt, pss, new BeanPropertyRowMapper<>(Page.class));
    }

    public List<Page> getPagesByIds(Collection<Integer> pagesIds){
        if(!pagesIds.isEmpty()) {
            String stmt = String.format("SELECT p.id, p.site_id, s.url AS hostName, p.path, p.code, p.content FROM page p JOIN site s ON p.site_id = s.id WHERE p.id IN (%s);",
                    pagesIds.stream()
                            .map(x -> Integer.toString(x))
                            .collect(Collectors.joining(", ")));
            return jdbcTemplate.query(stmt, new BeanPropertyRowMapper<>(Page.class));
        }
        return new LinkedList<>();
    }

    public void deletePagesForSite(String host) {
        String stmt = String.format("DELETE FROM page WHERE site_id = (SELECT id FROM site WHERE url LIKE \"%s\");", host);
        jdbcTemplate.execute(stmt);
    }

    public void clearPageInfo(String host, String path) {
        String stmt = String.format("DELETE FROM page WHERE site_id = (SELECT id FROM site WHERE url like \"%s\") AND path LIKE \"%s\";",
                host, path);
        jdbcTemplate.execute(stmt);
    }
}
