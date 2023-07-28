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

    public SearchResponse oneSiteSearch(String query, String site, int offset, int limit) {
        String siteUrl = "";
        String path = "/";
        try {
            URL gotUrl = new URL(site);
            siteUrl = gotUrl.getProtocol() + "://" + gotUrl.getHost() + "/";
            path = gotUrl.getPath();
        } catch (MalformedURLException e) {
            log.error("Error at parsing url, ", e);
        }

        path = path.trim();
        path = path.isBlank() ? "/" : path;
        Optional<SiteEntity> optional = siteRepository.findByUrlIgnoreCase(siteUrl);
        if (optional.isPresent()) {
            SiteEntity siteToSearch = optional.get();
            Map<String, Integer> queryMap = lemmaFinder.collectLemmas(query);
            Map<LemmaEntity, Integer> mapToSort = findLemmaMatchesSingleSite(siteToSearch, queryMap);
            Map<LemmaEntity, Integer> sortedLemmas = sortMap(mapToSort);
            Set<PageEntity> pagesList = searchForPages(sortedLemmas.keySet());

            if (pagesList.size()>0) {
                Map<PageEntity, Integer> relevancePageMap = calculateRelevance(pagesList, sortedLemmas.keySet());
                Map<PageEntity, Integer> sortedRelevance = sortMapReverse(relevancePageMap);
                Set<SearchInfo> organizedSearch = organizeSearch(sortedRelevance, query);
                return new SearchResponse(true, pagesList.size(), organizedSearch);
            } else {
                return new SearchResponse(false, "Указанная страница не найдена");
            }
        }
        return new SearchResponse(false, "Нет индексированного сайта с url " + siteUrl);
    }

    public SearchResponse groupSiteSearch(String query, int offset, int limit) {
        Map<String, Integer> queryMap = lemmaFinder.collectLemmas(query);
        Map<LemmaEntity, Integer> mapToSort = findLemmaMatches(queryMap);
        Map<LemmaEntity, Integer> sortedLemmas = sortMap(mapToSort);
        Set<PageEntity> pagesList = searchForPages(sortedLemmas.keySet());

        if (pagesList.size()>0) {
            Map<PageEntity, Integer> relevancePageMap = calculateRelevance(pagesList, sortedLemmas.keySet());
            Map<PageEntity, Integer> sortedRelevance = sortMapReverse(relevancePageMap);
            Set<SearchInfo> organizedSearch = organizeSearch(sortedRelevance, query);
            return new SearchResponse(true, pagesList.size(), organizedSearch);
        }
            return new SearchResponse(false, "Указанная страница не найдена");
    }

    private Set<SearchInfo> organizeSearch(Map<PageEntity, Integer> sortedRelevance, String query) {
        Set<SearchInfo> collector = new HashSet<>();
        for (Map.Entry<PageEntity, Integer> pageRelevanceEntry : sortedRelevance.entrySet()) {
            String title = htmlParser.getTitle(pageRelevanceEntry.getKey().getContent());
            String snippet = cutSnippet(query, pageRelevanceEntry.getKey());
            SearchInfo instance = new SearchInfo();
            instance.setUri(pageRelevanceEntry.getKey().getPath());
            instance.setSite(String.valueOf(pageRelevanceEntry.getKey().getSite()));
            instance.setSiteName(title);
            instance.setSnippet(snippet);
            instance.setRelevance(pageRelevanceEntry.getValue());
            collector.add(instance);
        }
        return collector;
    }

    private Map<PageEntity, Integer> calculateRelevance(Set<PageEntity> pageSet, Set<LemmaEntity> lemmas) {
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
        Map<PageEntity, Integer> relRel = new HashMap<>();
        Integer maxRankValue = relAbs.values().stream().max(Integer::compare).get();
        for (Map.Entry<PageEntity, Integer> relAbsEntry : relAbs.entrySet()) {
            int result = relAbsEntry.getValue() / maxRankValue;
            relRel.put(relAbsEntry.getKey(), result);
        }
        return relRel;
    }

    private List<String> getSearchWords(String cleanText, String searchQuery) {
        Map<String, String> stringMap = lemmaFinder.collectLemmasAndQueryWord(cleanText);
        Map<String, String> searchLemmas = lemmaFinder.collectLemmasAndQueryWord(searchQuery);
        return searchLemmas
                .keySet().stream()
                .filter(stringMap::containsKey)
                .map(stringMap::get)
                .toList();
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
        String text = htmlParser.htmlToText(page.getContent()).trim();
        List<String> keywords = getSearchWords(text, query);
        StringBuilder snippet = new StringBuilder();
        for (String word : keywords) {
            int firstIndex = text.indexOf(word);
            int lastIndex = text.indexOf(firstIndex + word.length(), firstIndex);
            String before;
            String after;
            if (firstIndex - 15 < 0) {
                before = text.substring(0, firstIndex) + " <b>";
            } else {
                before = "..." + text.substring(firstIndex - 15, firstIndex) + " <b>";
            }
            if (lastIndex + 15 > text.length()) {
                after = "</b> " + text.substring(lastIndex);
            } else {
                after = "</b> " + text.substring(lastIndex, firstIndex + 15) + "...";
            }
            String keyWord = text.substring(firstIndex, lastIndex);
            snippet.append(before).append(keyWord).append(after);
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

    private Map<LemmaEntity, Integer> findLemmaMatchesSingleSite(SiteEntity siteToSearch, Map<String, Integer> queryMap) {
        Map<LemmaEntity, Integer> newMap = new HashMap<>();
        for (String lemma : queryMap.keySet()) {
            Optional<LemmaEntity> optionalLemma = lemmaRepository.findBySiteAndLemma(siteToSearch, lemma);
            optionalLemma.ifPresent(lemmaEntity -> newMap.put(lemmaEntity, lemmaEntity.getFrequency()));
        }
        return newMap;
    }


    private Map<LemmaEntity, Integer> findLemmaMatches(Map<String, Integer> queryMap) {
        Map<LemmaEntity, Integer> newMap = new HashMap<>();
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