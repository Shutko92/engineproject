package searchengine.model;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface LemmaRepository extends JpaRepository<LemmaEntity, Integer> {
    LemmaEntity findByLemma(String key);
    Optional<LemmaEntity> findBySiteAndLemma(SiteEntity site, String name);
    Iterable<? extends LemmaEntity> findAllBySite(SiteEntity site);

}
