package searchengine.dto.statistics;

import lombok.Data;

@Data
public class StatisticsResponse {
    private boolean result;
    private StatisticsData statistics;

    public StatisticsResponse(StatisticsData statistics) {
        result = true;
        this.statistics = statistics;
    }
}
