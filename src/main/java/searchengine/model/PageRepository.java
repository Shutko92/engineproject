package searchengine.model;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface PageRepository extends JpaRepository<PageEntity, Integer> {
    boolean existsBySiteIdAndPath(Integer siteId, String path);
}
