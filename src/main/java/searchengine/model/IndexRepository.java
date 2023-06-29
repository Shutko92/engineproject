package searchengine.model;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface IndexRepository extends JpaRepository<IndexEntity, Integer> {
    int countByLemma(LemmaEntity lemma);
}
