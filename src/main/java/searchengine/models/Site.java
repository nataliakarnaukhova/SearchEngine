package searchengine.models;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import searchengine.enums.Status;

import javax.persistence.*;
import java.io.Serializable;
import java.util.Date;
import java.util.List;

@Entity
@Getter
@Setter
@NoArgsConstructor
public class Site implements Serializable {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(nullable = false)
    private Integer id;

    @Column(columnDefinition = "ENUM('INDEXING', 'INDEXED', 'FAILED')", nullable = false)
    @Enumerated(EnumType.STRING)
    private Status status;

    @Column(columnDefinition = "DATETIME", nullable = false)
    private Date statusTime;

    @Column(columnDefinition = "TEXT")
    private String lastError;

    @Column(columnDefinition = "VARCHAR(255)", nullable = false)
    private String url;

    @Column(columnDefinition = "VARCHAR(255)", nullable = false)
    private String name;

    @OneToMany(mappedBy = "site", cascade = CascadeType.ALL)
    private List<Page> pageList;

    @OneToMany(mappedBy = "site", cascade = CascadeType.ALL)
    private List<Lemma> lemmaList;
}
