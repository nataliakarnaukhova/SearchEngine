package searchengine.dto.statistics;

import lombok.*;

@Builder
@Setter
@Getter
@AllArgsConstructor
@NoArgsConstructor
public class TotalStatisticsDto {
    private int sites;
    private int pages;
    private int lemmas;
    private boolean indexing;
}
