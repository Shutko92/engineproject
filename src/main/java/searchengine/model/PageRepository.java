package searchengine.model;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface PageRepository extends JpaRepository<PageEntity, Integer> {
    boolean existsBySiteIdAndPath(Integer siteId, String path);

    PageEntity findBySiteAndPath(SiteEntity site, String path);

//    Optional<PageEntity> findBySiteAndPath(SiteEntity site, String path);

}
