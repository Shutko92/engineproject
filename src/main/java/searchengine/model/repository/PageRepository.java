package searchengine.model.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import searchengine.model.entities.SiteEntity;
import searchengine.model.entities.PageEntity;

@Repository
public interface PageRepository extends JpaRepository<PageEntity, Integer> {
    boolean existsBySiteIdAndPath(Integer siteId, String path);

    PageEntity findBySiteAndPath(SiteEntity site, String path);

//    Optional<PageEntity> findBySiteAndPath(SiteEntity site, String path);

}
