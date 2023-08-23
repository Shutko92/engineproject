package searchengine.model.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import searchengine.model.entities.SiteEntity;
import searchengine.model.entities.LemmaEntity;

import java.util.List;
import java.util.Optional;
import java.util.Set;

@Repository
public interface LemmaRepository extends JpaRepository<LemmaEntity, Integer> {
    Optional<LemmaEntity> findBySiteAndLemma(SiteEntity site, String name);
    Set<LemmaEntity> findAllBySite(SiteEntity site);
    List<Optional<LemmaEntity>> findByLemma(String lemma);
}