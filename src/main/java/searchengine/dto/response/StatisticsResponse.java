package searchengine.dto.response;

import lombok.Getter;
import searchengine.dto.statistics.StatisticsDto;

@Getter
public class StatisticsResponse extends SuccessResponse {
    private final StatisticsDto statistics;

    public StatisticsResponse(StatisticsDto statistics) {
        this.statistics = statistics;
    }
}
