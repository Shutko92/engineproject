package searchengine.model.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import searchengine.model.entities.Status;
import searchengine.model.entities.SiteEntity;

import java.util.Optional;

@Repository
public interface SiteRepository extends JpaRepository<SiteEntity, Integer> {
    boolean existsByStatus(Status status);
    boolean existsByIdAndStatus(Integer id, Status status);
    Optional<SiteEntity> findByUrlIgnoreCase(String siteUrl);

    boolean existsByStatusNot(Status status);
}
