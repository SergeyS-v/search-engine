package my.searchengine;

import my.searchengine.dao.DaoController;
import my.searchengine.model.Site;
import my.searchengine.services.UrlReader;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

@SpringBootApplication
public class SearchEngine implements CommandLineRunner {

    @Autowired
    UrlReader urlReader;
    @Autowired
    DaoController daoController;
    @Autowired
    AppProp appProp;

    public static void main(String[] args) {
        SpringApplication.run(SearchEngine.class, args);
    }

    @Override
    public void run(String... args) throws Exception {
        if(!appProp.isOnlyQueryMode()) {
            daoController.init();
            urlReader.initSiteTable(Site.Status.FAILED, "Indexing not started");
        }
    }
}
