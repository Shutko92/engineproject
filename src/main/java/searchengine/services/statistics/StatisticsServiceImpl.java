package searchengine.services.statistics;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import searchengine.dto.statistics.DetailedStatisticsItem;
import searchengine.dto.statistics.StatisticsData;
import searchengine.dto.statistics.StatisticsResponse;
import searchengine.dto.statistics.TotalStatistics;
import searchengine.model.entities.SiteEntity;
import searchengine.model.entities.Status;
import searchengine.model.repository.LemmaRepository;
import searchengine.model.repository.PageRepository;
import searchengine.model.repository.SiteRepository;

import java.util.ArrayList;
import java.util.List;

@Service
@RequiredArgsConstructor
public class StatisticsServiceImpl implements StatisticsService {
    private final SiteRepository siteRepository;
    private final PageRepository pageRepository;
    private final LemmaRepository lemmaRepository;

    @Override
    public StatisticsResponse getStatistics() {
        TotalStatistics total = new TotalStatistics(
                siteRepository.count(),
                pageRepository.count(),
                lemmaRepository.count(),
                !siteRepository.existsByStatusNot(Status.INDEXED)
        );

        List<DetailedStatisticsItem> detailed = new ArrayList<>();
        List<SiteEntity> sitesList = siteRepository.findAll();

        for (SiteEntity site : sitesList) {

            DetailedStatisticsItem item = new DetailedStatisticsItem();
            item.setName(site.getName());
            item.setUrl(site.getUrl());
            item.setPages(site.getPages().size());
            item.setLemmas(site.getLemmas().size());
            item.setStatus(String.valueOf(site.getStatus()));
            item.setError(site.getLastError());
            item.setStatusTime(site.getStatusTime());
            detailed.add(item);
        }

        StatisticsData statistics = new StatisticsData(total, detailed);
        return new StatisticsResponse(statistics);
    }
}