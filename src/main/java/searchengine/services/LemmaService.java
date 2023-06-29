package searchengine.services;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import searchengine.config.Site;
import searchengine.model.*;

import java.util.HashSet;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

@Slf4j
@RequiredArgsConstructor
@Service
public class LemmaService {
    private final HtmlParser htmlParser;
    private final LemmaFinder lemmaFinder;
    private final LemmaRepository lemmaRepository;
    private final IndexRepository indexRepository;
    private final SiteRepository siteRepository;
    public void findAndSave(PageEntity page) {
        String clearText = htmlParser.htmlToText(page.getContent());
        Map<String, Integer> lemmaMap = lemmaFinder.collectLemmas(clearText);

//        for (Map.Entry<String, Integer> lemmaEntry : lemmaMap.entrySet()) {
//            String key = lemmaEntry.getKey();
//            Integer value = lemmaEntry.getValue();
//            LemmaEntity firstLemmaRequestEntity = lemmaRepository.findByLemma(key);
//            if (firstLemmaRequestEntity == null) {
//                lemmaRepository.save(LemmaEntity.builder().site(page.getSite()).lemma(key).frequency(1).build());
//            }
//            LemmaEntity secondLemmaRequest = lemmaRepository.findByLemma(key);
//            indexRepository.save(IndexEntity.builder().page(page).lemmaEntity(secondLemmaRequest).rank(value).build());
//        }
        Set<LemmaEntity> lemmaSetToSave = new HashSet<>();
        Set<IndexEntity> indices = new HashSet<>();
        synchronized (lemmaRepository) {
            lemmaMap.forEach((name, count) -> {
                Optional<LemmaEntity> optionalLemma = lemmaRepository.findBySiteAndLemma(page.getSite(), name);
                LemmaEntity lemma;
                if (optionalLemma.isPresent()) {
                    lemma = optionalLemma.get();
                } else {
                    lemma = LemmaEntity.builder()
                            .frequency(1)
                            .lemma(name)
                            .site(page.getSite())
                            .build();
                    lemmaSetToSave.add(lemma);
                }

                indices.add(IndexEntity.builder()
                        .page(page)
                        .lemma(lemma)
                        .rank((float) count)
                        .build());
            });
            lemmaRepository.saveAll(lemmaSetToSave);
        }
        indexRepository.saveAll(indices);
    }

    public void updateLemmasFrequency(Integer siteId) {
        SiteEntity site = siteRepository.findById(siteId).orElseThrow(() -> new IllegalStateException("Site not found"));
        Set<LemmaEntity> lemmaToSave = new HashSet<>();
        Set<LemmaEntity> lemmaToDelete = new HashSet<>();
        log.info("Start calculate lemmas frequency for site: {}", site);
        for (LemmaEntity lemma : lemmaRepository.findAllBySite(site)) {
            int frequency = indexRepository.countByLemma(lemma);
            if (frequency == 0) {
                lemmaToDelete.add(lemma);
            } else if (lemma.getFrequency() != frequency) {
                lemma.setFrequency(frequency);
                lemmaToSave.add(lemma);
            }
        }
        lemmaRepository.deleteAll(lemmaToDelete);
        log.info("Update lemmas: " + lemmaToSave.size());
        lemmaRepository.saveAll(lemmaToSave);
    }
}