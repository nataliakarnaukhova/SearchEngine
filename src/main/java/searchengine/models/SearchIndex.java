package searchengine.models;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import javax.persistence.*;

@Entity
@NoArgsConstructor
@Getter
@Setter
public class SearchIndex {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(nullable = false)
    private Integer id;

    @Column(nullable = false)
    private Integer pageId;

    @Column(nullable = false)
    private Integer lemmaId;

    @Column(nullable = false)
    private Float lemmaRank;
}
