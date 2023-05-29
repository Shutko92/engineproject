package searchengine.model;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface SiteRepository extends JpaRepository<SiteEntity, Integer> {
    SiteEntity findSiteByUrl(String url);
    boolean existsByStatus(Status status);
    boolean existsByIdAndStatus(Integer id, Status status);
    void deleteSiteByUrl(String url);
}
