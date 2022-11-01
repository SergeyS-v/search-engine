package my.searchengine;

import lombok.AllArgsConstructor;
import my.searchengine.dao.DaoController;
import my.searchengine.model.Site;
import my.searchengine.services.UrlReader;
import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

@AllArgsConstructor
@SpringBootApplication
public class SearchEngine implements CommandLineRunner {

    private final UrlReader urlReader;
    private final DaoController daoController;
    private final AppProp appProp;

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
