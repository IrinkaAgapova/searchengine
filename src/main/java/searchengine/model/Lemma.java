package searchengine.model;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import javax.persistence.*;
import java.util.List;
import java.util.Objects;


@Entity
@Setter
@Getter
@NoArgsConstructor
@Table(name = "lemma")

public class Lemma {
    @Id
    @Column(nullable = false)
    @GeneratedValue (strategy = GenerationType.IDENTITY)
    private Integer id;

    @Column(columnDefinition = "VARCHAR(255)", nullable = false)
    private String lemma;

    @Column(nullable = false)
    private Integer frequency;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "site_id")
   // @Column(nullable = false)
    private Site site;

@OneToMany(mappedBy = "lemma", fetch = FetchType.LAZY, cascade = CascadeType.ALL)
private List<Index> indexList;
    @Override
    public boolean equals(Object obj) {
        if (this == obj) return true;
        if (obj == null || getClass() != obj.getClass()) return false;
        Lemma that = (Lemma) obj;
        return Objects.equals(this.lemma, that.lemma);
    }

    @Override
    public int hashCode() {
        return Objects.hash(lemma);
    }

}