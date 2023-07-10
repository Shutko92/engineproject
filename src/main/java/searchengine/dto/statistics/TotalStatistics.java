package searchengine.dto.statistics;

import lombok.Data;

@Data
public class TotalStatistics {
    private int sites;
    private long pages;
    private long lemmas;
    private boolean indexing;
}
