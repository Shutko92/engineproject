package searchengine.services;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.Map;

@Slf4j
@Service
@RequiredArgsConstructor
public class SearchService {
    private final LemmaFinder lemmaFinder;
    public void startSearch(String query, String site, int offset, int limit) {
        Map<String, Integer> lemmaMap = lemmaFinder.collectLemmas(query);
    }
}
