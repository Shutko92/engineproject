package searchengine.services.searching;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import searchengine.dto.searching.SearchInfo;
import searchengine.dto.searching.SearchResponse;
import searchengine.model.entities.IndexEntity;
import searchengine.model.entities.LemmaEntity;
import searchengine.model.entities.PageEntity;
import searchengine.model.entities.SiteEntity;
import searchengine.model.repository.IndexRepository;
import searchengine.model.repository.LemmaRepository;
import searchengine.model.repository.SiteRepository;
import searchengine.services.indexing.HtmlParser;
import searchengine.services.lemmas.LemmaFinder;

import java.net.MalformedURLException;
import java.net.URL;
import java.util.*;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class SearchServiceImpl implements SearchService {
    private final LemmaFinder lemmaFinder;
    private final LemmaRepository lemmaRepository;
    private final IndexRepository indexRepository;
    private final HtmlParser htmlParser;
    private final SiteRepository siteRepository;

    @Override
    public SearchResponse processSearch(String query, String site, int offset, int limit) {
        Map<String, Integer> queryMap = lemmaFinder.collectLemmas(query);
        Map<LemmaEntity, Integer> mapToSort = findLemmaMatches(site, queryMap);
        Map<LemmaEntity, Integer> sortedLemmas = sortMap(mapToSort);
        Set<PageEntity> pagesList = searchForPages(sortedLemmas.keySet());

        if (!pagesList.isEmpty()) {
            Map<PageEntity, Double> relevancePageMap = calculateRelevance(pagesList, sortedLemmas.keySet());
            List<SearchInfo> organizedSearch = organizeSearch(relevancePageMap, query);
            List<SearchInfo> subInfo = subList(organizedSearch, offset, limit);
            return new SearchResponse(true, organizedSearch.size(), subInfo);
        }
        return new SearchResponse(false, "Указанная страница не найдена");
    }


    private List<SearchInfo> organizeSearch(Map<PageEntity, Double> sortedRelevance, String query) {
        List<SearchInfo> collector = new ArrayList<>();
        for (Map.Entry<PageEntity, Double> pageRelevanceEntry : sortedRelevance.entrySet()) {
            String title = htmlParser.getTitle(pageRelevanceEntry.getKey().getContent());
            String snippet = cutSnippet(query, pageRelevanceEntry.getKey());
            if (!title.isEmpty() && !snippet.isEmpty()) {
                SearchInfo instance = new SearchInfo();
                String url = pageRelevanceEntry.getKey().getSite().getUrl();
                instance.setSite(String.valueOf(pageRelevanceEntry.getKey().getSite()));
                instance.setSiteName(pageRelevanceEntry.getKey().getSite().getName());
                instance.setUri(url);
                instance.setTitle(title);
                instance.setSnippet(snippet);
                instance.setRelevance(pageRelevanceEntry.getValue());
                collector.add(instance);
            }

        }
        return collector;
    }

    private Map<PageEntity, Double> calculateRelevance(Set<PageEntity> pageSet, Set<LemmaEntity> lemmas) {
        List<PageEntity> pageList = pageSet.stream().toList();
        Map<PageEntity, Set<Float>> ranksPerPage = new HashMap<>();
        for (int i = 1; i < pageList.size(); i++) {
            Set<Float> ranks = indexRepository.findAllByPageAndLemmaIn(pageList.get(i), lemmas)
                    .stream().map(IndexEntity::getRank)
                    .collect(Collectors.toSet());
            ranksPerPage.put(pageList.get(i), ranks);
        }

        Map<PageEntity, Integer> relAbs = new HashMap<>();
        for (Map.Entry<PageEntity, Set<Float>> ranksValue : ranksPerPage.entrySet()) {
            int sum = 0;
            for (Float rank : ranksValue.getValue()) {
                sum += rank;
            }
            relAbs.put(ranksValue.getKey(), sum);
        }
        Map<PageEntity, Integer> sortedRelAbs = sortMapReverse(relAbs);

        Map<PageEntity, Double> relRel = new HashMap<>();
        double maxRankValue = sortedRelAbs.values().stream().findFirst().get();
        sortedRelAbs.forEach((key, value) -> {
            double result = (double) value / maxRankValue;
            relRel.put(key, result);
        });
        return relRel;
    }

    private List<String> findSearchWords(String cleanText, String searchQuery) {
        Map<String, String> lemmasToWordsInText = lemmaFinder.collectLemmasAndWords(cleanText);
        Map<String, String> searchLemmasToWords = lemmaFinder.collectLemmasAndWords(searchQuery);
        List<String> searchWords = new ArrayList<>();
        for (Map.Entry<String, String> queryEntry : searchLemmasToWords.entrySet()) {
            for (Map.Entry<String, String> textEntry : lemmasToWordsInText.entrySet()) {
                if (textEntry.getKey().equals(queryEntry.getKey())) {
                    searchWords.add(textEntry.getValue());

                }
            }
        }
        return searchWords;
    }

    private String cutSnippet (String query, PageEntity page) {
        String text = htmlParser.htmlToText(page.getContent());
        List<String> keywords = findSearchWords(text, query);
        StringBuilder snippet = new StringBuilder();
        final int NORMAL_SIZE = 25;
        final int SPECIAL_SIZE = 12;
        int sideStep = NORMAL_SIZE;
        if (keywords.size()> 3) {
            sideStep = SPECIAL_SIZE;
        }

        for (String word : keywords) {

            if (text.contains(word)) {
                int firstIndex = text.indexOf(word);
                int lastIndex = firstIndex + word.length();
                String before;
                String after;

                if (firstIndex - sideStep < 0) {
                    before = "..." + text.substring(0, firstIndex) + " <b>";
                } else {
                    before = "..." + text.substring(firstIndex - sideStep, firstIndex) + " <b>";
                }

                if (lastIndex + sideStep > text.length()) {
                    after = "</b> " + text.substring(lastIndex) + "...";
                } else {
                    after = "</b> " + text.substring(lastIndex, firstIndex + sideStep) + "...";
                }
                String keyWord = text.substring(firstIndex, lastIndex);
                snippet.append(before).append(keyWord).append(after);
            }

        }
        return snippet.toString();
    }

    private Set<PageEntity> searchForPages(Set<LemmaEntity> lemmas) {
        if (lemmas.isEmpty()) {
            return Set.of();
        }

        List<LemmaEntity> lemmaList = lemmas.stream().toList();
        Set<PageEntity> pages = lemmaList.get(0).getIndexes().stream()
                .map(IndexEntity::getPage)
                .collect(Collectors.toSet());

        for (int i = 1; i < lemmaList.size(); i++) {
            pages = indexRepository.findAllByLemmaAndPageIn(lemmaList.get(i), pages)
                    .stream().map(IndexEntity::getPage)
                    .collect(Collectors.toSet());
            if (pages.isEmpty()) {
                return pages;
            }
        }
        return pages;
    }

    private List<SearchInfo> subList(List<SearchInfo> searchData, Integer offset, Integer limit) {
        int fromIndex = offset;
        int toIndex = fromIndex + limit;

        if (toIndex > searchData.size()) {
            toIndex = searchData.size();
        }
        if (fromIndex > toIndex) {
            return List.of();
        }
        return searchData.subList(fromIndex, toIndex);
    }

    private Map<LemmaEntity, Integer> findLemmaMatches(String site, Map<String, Integer> queryMap) {
        String siteUrl = "";
        try {
            URL gotUrl = new URL(site);
            siteUrl = gotUrl.getProtocol() + "://" + gotUrl.getHost() + "/";
        } catch (MalformedURLException e) {
            log.error("Error at parsing url, ", e);
        }
        Optional<SiteEntity> optional = siteRepository.findByUrlIgnoreCase(siteUrl);
        Map<LemmaEntity, Integer> newMap = new HashMap<>();
        if (optional.isPresent()) {
            SiteEntity siteToSearch = optional.get();
            for (String lemma : queryMap.keySet()) {
                Optional<LemmaEntity> optionalLemma = lemmaRepository.findBySiteAndLemma(siteToSearch, lemma);
                optionalLemma.ifPresent(lemmaEntity -> newMap.put(lemmaEntity, lemmaEntity.getFrequency()));
            }
            return newMap;
        }
        for (String lemma : queryMap.keySet()) {
            Optional<LemmaEntity> optionalLemma = lemmaRepository.findByLemma(lemma);
            optionalLemma.ifPresent(lemmaEntity -> newMap.put(lemmaEntity, lemmaEntity.getFrequency()));
        }
        return newMap;
    }

    private Map<LemmaEntity, Integer> sortMap(Map<LemmaEntity, Integer> mapToSort) {
        return mapToSort.entrySet().stream()
                .sorted(Map.Entry.comparingByValue())
                .collect(Collectors.toMap(
                        Map.Entry::getKey,
                        Map.Entry::getValue,
                        (a, b) -> a,
                        LinkedHashMap::new));
    }

    private Map<PageEntity, Integer> sortMapReverse(Map<PageEntity, Integer> mapToSort) {
        return mapToSort.entrySet().stream()
                .sorted(Map.Entry.comparingByValue(Comparator.reverseOrder())).limit(500)
                .collect(Collectors.toMap(
                        Map.Entry::getKey,
                        Map.Entry::getValue,
                        (a, b) -> a,
                        LinkedHashMap::new));
    }
}