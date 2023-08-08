package searchengine.model.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import searchengine.model.entities.LemmaEntity;
import searchengine.model.entities.IndexEntity;
import searchengine.model.entities.PageEntity;

import java.util.Set;

@Repository
public interface IndexRepository extends JpaRepository<IndexEntity, Integer> {
    int countByLemma(LemmaEntity lemma);
    Set<IndexEntity> findAllByLemmaAndPageIn(LemmaEntity lemmaEntity, Set<PageEntity> pages);
    Set<IndexEntity> findAllByPageAndLemmaIn(PageEntity pageEntity, Set<LemmaEntity> lemmas);
}