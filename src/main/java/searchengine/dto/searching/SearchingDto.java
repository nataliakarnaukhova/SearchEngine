package searchengine.dto.searching;

import lombok.*;

@Builder
@Setter
@Getter
@AllArgsConstructor
@NoArgsConstructor
public class SearchingDto {
    private String site;
    private String siteName;
    private String uri;
    private String title;
    private String snippet;
    private float relevance;
}
