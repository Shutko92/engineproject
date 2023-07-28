package searchengine.dto.statistics;

import lombok.Data;

import java.util.List;

@Data
public class StatisticsData {
    private TotalStatistics total;
    private List<DetailedStatisticsItem> detailed;

    public StatisticsData(TotalStatistics total, List<DetailedStatisticsItem> detailed) {
        this.total = total;
        this.detailed = detailed;
    }
}
