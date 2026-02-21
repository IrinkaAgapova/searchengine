package searchengine.model;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import java.util.List;
import javax.persistence.*;

@Entity
@Getter
@Setter
//@Table(name = "page", indexes = {@Index(name = "path_index", columnList = "path")})
@Table(name = "page")
@NoArgsConstructor

public class Page {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private int id;

    @Column(columnDefinition = "VARCHAR(255)", nullable = false)
    private String path;

    private int code;

    @Column(columnDefinition = "MEDIUMTEXT CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci")
    private String content;

    @ManyToOne()
    @JoinColumn(name = "site_id", nullable = false)
    private Site site;

    @OneToMany(mappedBy = "page", cascade = CascadeType.ALL,fetch = FetchType.LAZY)
    private List<Index> indexList;

    public Page(String path) {
        this.path = path;
    }
}

