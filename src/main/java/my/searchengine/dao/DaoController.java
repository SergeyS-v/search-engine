package my.searchengine.dao;

import lombok.AllArgsConstructor;
import lombok.Getter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

@AllArgsConstructor
@Getter
@Repository
public class DaoController {
    private final IndexDao indexDao;
    private final LemmaDao lemmaDao;
    private final PageDao pageDao;
    private final SiteDao siteDao;
    private final StatisticsDao statisticsDao;
    private final JdbcTemplate jdbcTemplate;
    private static final Logger logger = LoggerFactory.getLogger(DaoController.class);

    public void init() {
        try {
            //DROP TABLES
            jdbcTemplate.execute("DROP TABLE IF EXISTS `index`");
            jdbcTemplate.execute("DROP TABLE IF EXISTS page");
            jdbcTemplate.execute("DROP TABLE IF EXISTS lemma");
            jdbcTemplate.execute("DROP TABLE IF EXISTS site");
            jdbcTemplate.execute("DROP TABLE IF EXISTS field");

            // CREATE site TABLE
            jdbcTemplate.execute("CREATE TABLE site (" +
                    "id INT NOT NULL AUTO_INCREMENT PRIMARY KEY, " +
                    "status ENUM('INDEXING', 'INDEXED', 'FAILED') NOT NULL, " +
                    "status_time DATETIME NOT NULL, " +
                    "last_error TEXT," +
                    "url VARCHAR(255) NOT NULL," +
                    "name VARCHAR(255) NOT NULL," +
                    "UNIQUE INDEX url(url(255)));");

            // CREATE page TABLE
            jdbcTemplate.execute("CREATE TABLE page(" +
                    "id INT NOT NULL AUTO_INCREMENT, " +
                    "site_id INT NOT NULL, " +
                    "path TEXT NOT NULL, " +
                    "code INT NOT NULL, " +
                    "content MEDIUMTEXT NOT NULL, " +
                    "PRIMARY KEY(id), " +
                    "FOREIGN KEY (site_id) REFERENCES site (id) ON DELETE CASCADE," +
                    "UNIQUE INDEX path(site_id, path(767)));");

            // CREATE field TABLE
            jdbcTemplate.execute("CREATE TABLE field(" +
                    "id INT NOT NULL AUTO_INCREMENT PRIMARY KEY, " +
                    "name VARCHAR(255) NOT NULL, " +
                    "selector VARCHAR(255) NOT NULL, " +
                    "weight FLOAT NOT NULL);");
            jdbcTemplate.execute("insert into field(name, selector, weight) values ('title', 'title', 1)");
            jdbcTemplate.execute("insert into field(name, selector, weight) values ('body', 'body', 0.8)");

            // CREATE lemma TABLE
            jdbcTemplate.execute("CREATE TABLE lemma(" +
                    "id INT NOT NULL AUTO_INCREMENT PRIMARY KEY, " +
                    "lemma VARCHAR(255) NOT NULL, " +
                    "site_id INT NOT NULL, " +
                    "frequency INT NOT NULL, " +
                    "FOREIGN KEY (site_id) REFERENCES site (id) ON DELETE CASCADE," +
                    "UNIQUE INDEX lemma(site_id, lemma(255)));"); // TODO: 05.11.2022 Наверное, вместо этого нужно сделать PK составным как id и site_id. А site_id сделать FK

            // CREATE index TABLE
            jdbcTemplate.execute("CREATE TABLE `index`(" +
//                    "id INT NOT NULL AUTO_INCREMENT PRIMARY KEY, " +
                    "page_id INT NOT NULL, " +
                    "lemma_id INT NOT NULL, " +
                    "rank FLOAT DEFAULT 0," +
                    "PRIMARY KEY (page_id, lemma_id)," +
                    "FOREIGN KEY (page_id) REFERENCES page (id) ON DELETE CASCADE," +
                    "FOREIGN KEY (lemma_id) REFERENCES lemma (id) ON DELETE CASCADE" +
                    ");");
        } catch (Exception e) {
            logger.error("Ошибка инициализации таблиц в БД", e);
        }
    }

    public void clearSiteInfo(String host) {
        pageDao.deletePagesForSite(host);
        lemmaDao.deleteLemmasForSite(host);
    }

    public void clearPageInfo(String host, String path) {
        pageDao.clearPageInfo(host, path);
    }
}
