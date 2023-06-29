package searchengine.model;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import searchengine.config.Site;

import java.util.Optional;

@Repository
public interface SiteRepository extends JpaRepository<SiteEntity, Integer> {
    SiteEntity findByUrl(String url);
    boolean existsByStatus(Status status);
    boolean existsByIdAndStatus(Integer id, Status status);
    Optional<SiteEntity> findByUrlIgnoreCase(String siteUrl);

}
