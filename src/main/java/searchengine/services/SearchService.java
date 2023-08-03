package searchengine.services;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import searchengine.dto.searching.SearchInfo;
import searchengine.dto.searching.SearchResponse;
import searchengine.dto.searching.WordLemmas;
import searchengine.model.entities.IndexEntity;
import searchengine.model.entities.LemmaEntity;
import searchengine.model.entities.PageEntity;
import searchengine.model.entities.SiteEntity;
import searchengine.model.repository.IndexRepository;
import searchengine.model.repository.LemmaRepository;
import searchengine.model.repository.SiteRepository;

import java.net.MalformedURLException;
import java.net.URL;
import java.util.*;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class SearchService {
    private final LemmaFinder lemmaFinder;
    private final LemmaRepository lemmaRepository;
    private final IndexRepository indexRepository;
    private final HtmlParser htmlParser;
    private final SiteRepository siteRepository;
    private static final int SYMBOLS_IN_SNIPPET = 100;

    public SearchResponse processSearch(String query, String site, int offset, int limit) {
        Map<String, Integer> queryMap = lemmaFinder.collectLemmas(query);
        Map<LemmaEntity, Integer> mapToSort = findLemmaMatches(site, queryMap);
        Map<LemmaEntity, Integer> sortedLemmas = sortMap(mapToSort);
        Set<PageEntity> pagesList = searchForPages(sortedLemmas.keySet());

        if (!pagesList.isEmpty()) {
            Map<PageEntity, Double> relevancePageMap = calculateRelevance(pagesList, sortedLemmas.keySet());
            Map<PageEntity, Double> sortedRelevance = sortMapReverse(relevancePageMap);
            List<SearchInfo> organizedSearch = organizeSearch(sortedRelevance, query);
            List<SearchInfo> subInfo = subList(organizedSearch, offset, limit);
            return new SearchResponse(true, pagesList.size(), subInfo);
        }
        return new SearchResponse(false, "Указанная страница не найдена");
    }

    private List<SearchInfo> organizeSearch(Map<PageEntity, Double> sortedRelevance, String query) {
        List<SearchInfo> collector = new ArrayList<>();
        for (Map.Entry<PageEntity, Double> pageRelevanceEntry : sortedRelevance.entrySet()) {
            String title = htmlParser.getTitle(pageRelevanceEntry.getKey().getContent());
            String snippet = cutSnippet(query, pageRelevanceEntry.getKey());

            SearchInfo instance = new SearchInfo();
            instance.setSite(String.valueOf(pageRelevanceEntry.getKey().getSite()));
            instance.setSiteName(pageRelevanceEntry.getKey().getSite().getName());
            instance.setUri(pageRelevanceEntry.getKey().getPath());
            instance.setTitle(title);
            instance.setSnippet(snippet);
            instance.setRelevance(pageRelevanceEntry.getValue());
            collector.add(instance);
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

        Map<PageEntity, Double> relRel = new HashMap<>();
        double maxRankValue = relAbs.values().stream().max(Double::compare).get();
        for (Map.Entry<PageEntity, Integer> relAbsEntry : relAbs.entrySet()) {
            double result = (double) relAbsEntry.getValue() / maxRankValue;
            relRel.put(relAbsEntry.getKey(), result);
        }
        return relRel;
    }

    private List<String> getSearchWords(String cleanText, String searchQuery) {
        Map<String, String> stringMap = lemmaFinder.collectLemmasAndWords(cleanText);
        Map<String, String> searchLemmas = lemmaFinder.collectLemmasAndWords(searchQuery);
        List<String> searchWords = new ArrayList<>();
        for (Map.Entry<String, String> stringEntry : searchLemmas.entrySet()) {
            for (Map.Entry<String, String> stringStringEntry : stringMap.entrySet()) {
                if (stringStringEntry.getKey().equals(stringEntry.getKey())) {
                    searchWords.add(stringStringEntry.getValue());

                }
            }
        }
        return searchWords;
    }

    private String paddingSnippet(String content, List<WordLemmas> pageCommonSequence) {
        String[] split = htmlParser.htmlToText(content).trim().split("\\s+");
        int startIndex = pageCommonSequence.get(0).getIndex();
        int endIndex = pageCommonSequence.get(pageCommonSequence.size() - 1).getIndex() + 1;
        int length = 0;
        int leftIndex = startIndex;
        int rightIndex = endIndex;
        while (length < SYMBOLS_IN_SNIPPET) {
            if (leftIndex > 0) {
                leftIndex--;
                length += split[leftIndex].length() + 1;
            }
            if (rightIndex < split.length) {
                length += split[rightIndex].length() + 1;
                rightIndex++;
            }
            if (startIndex == 0 && endIndex == split.length) {
                break;
            }
        }
        return (String.join(" ", Arrays.asList(split).subList(leftIndex, startIndex)) + " <b>" +
                String.join(" ", Arrays.asList(split).subList(startIndex, endIndex)) + "</b> " +
                String.join(" ", Arrays.asList(split).subList(endIndex, rightIndex))).trim();
    }

    private String cutSnippet (String query, PageEntity page) {
        String text = htmlParser.htmlToText(page.getContent());
        List<String> keywords = getSearchWords(text, query);
        StringBuilder snippet = new StringBuilder();

        for (String word : keywords) {
            final int SIDE_STEP = 50;

            if (text.contains(word)) {
                int firstIndex = text.indexOf(word);
                int lastIndex = firstIndex + word.length();
                String before;
                String after;

                if (firstIndex - SIDE_STEP < 0) {
                    before = "..." + text.substring(0, firstIndex) + " <b>";
                } else {
                    before = "..." + text.substring(firstIndex - SIDE_STEP, firstIndex) + " <b>";
                }
                if (lastIndex + SIDE_STEP > text.length()) {
                    after = "</b> " + text.substring(lastIndex) + "...";
                } else {
                    after = "</b> " + text.substring(lastIndex, firstIndex + SIDE_STEP) + "...";
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

    private Map<PageEntity, Double> sortMapReverse(Map<PageEntity, Double> mapToSort) {
        return mapToSort.entrySet()
                .stream()
                .sorted(Map.Entry.comparingByValue(Comparator.reverseOrder()))
                .collect(Collectors.toMap(
                        Map.Entry::getKey,
                        Map.Entry::getValue,
                        (a, b) -> a,
                        LinkedHashMap::new));
    }
}