package my.searchengine;

import my.searchengine.dao.DaoController;
import my.searchengine.model.Site;
import my.searchengine.services.UrlReader;
import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

@SpringBootApplication
public class SearchEngine implements CommandLineRunner {

    final UrlReader urlReader;
    final DaoController daoController;
    final AppProp appProp;

    public SearchEngine(UrlReader urlReader, DaoController daoController, AppProp appProp){
        this.urlReader = urlReader;
        this.daoController = daoController;
        this.appProp = appProp;
    }

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
