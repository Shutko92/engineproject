package searchengine.model.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import searchengine.model.entities.LemmaEntity;
import searchengine.model.entities.IndexEntity;

@Repository
public interface IndexRepository extends JpaRepository<IndexEntity, Integer> {
    int countByLemma(LemmaEntity lemma);
}
