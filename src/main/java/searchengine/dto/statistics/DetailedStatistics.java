package searchengine.dto.statistics;

import lombok.*;

@Setter
@Getter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class DetailedStatistics {
    private String url;
    private String name;
    private String status;
    private long statusTime;
    private String error;
    private int pages;
    private int lemmas;
}
