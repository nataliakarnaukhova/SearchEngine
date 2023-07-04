package searchengine.dto.statistics;

import lombok.*;

import java.util.List;

@Builder
@Setter
@Getter
@AllArgsConstructor
@NoArgsConstructor
public class StatisticsDto {
    private TotalStatisticsDto total;
    private List<DetailedStatistics> detailed;
}
