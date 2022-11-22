package my.searchengine.repositories;

import my.searchengine.model.Site;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface SiteRepository extends JpaRepository<Site, Integer> {
    List<Site> findByHostLike(String host);
}
