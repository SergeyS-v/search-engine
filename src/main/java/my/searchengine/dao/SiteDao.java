package my.searchengine.dao;

import lombok.AllArgsConstructor;
import my.searchengine.model.Site;
import my.searchengine.services.UrlReader;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.PreparedStatementSetter;
import org.springframework.stereotype.Repository;

import java.sql.PreparedStatement;
import java.sql.SQLException;

@AllArgsConstructor
@Repository
public class SiteDao {

    private final JdbcTemplate jdbcTemplate;

    public void insertSite(Site site) {
        String stmt = "INSERT INTO site (status, status_time, last_error, url, name) VALUES (?, ?, ?, ?, ?) ON DUPLICATE KEY UPDATE status = ?, status_time = ?, last_error = ?";
        if (site.getLastError() == null) {
            String lastError = UrlReader.lastErrorForSite.get(site.getHost());
            if (lastError != null && !lastError.isBlank()) {
                site.setLastError(UrlReader.lastErrorForSite.get(site.getHost()));
            }
        }
        PreparedStatementSetter pss = new PreparedStatementSetter() {
            @Override
            public void setValues(PreparedStatement ps) throws SQLException {
                ps.setString(1, site.getStatus().name());
                ps.setString(2, site.getStatusTime().toString());
                ps.setString(3, site.getLastError());
                ps.setString(4, site.getHost());
                ps.setString(5, site.getName());
                ps.setString(6, site.getStatus().name());
                ps.setString(7, site.getStatusTime().toString());
                ps.setString(8, site.getLastError());
            }
        };

        jdbcTemplate.update(stmt, pss);
    }

    public Integer getSiteIdByHost(String host) {
        String stmt = String.format("SELECT id FROM site WHERE url LIKE \"%s\";", host);
        return jdbcTemplate.queryForObject(stmt, Integer.class);
    }
}
