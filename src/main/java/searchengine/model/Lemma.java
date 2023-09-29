package searchengine.model;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import javax.persistence.*;
import java.util.HashSet;
import java.util.Set;

@Entity
@Table(name = "Lemmas")
@Getter
@Setter
@NoArgsConstructor
public class Lemma {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private int id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "site_id", nullable = false)
    private Site site;

    @Column(nullable = false)
    private String lemma;

    @Column(nullable = false)
    private int frequency;

    @OneToMany(mappedBy = "lemma", cascade = CascadeType.REMOVE, fetch = FetchType.LAZY)
    private Set<Index> indexes = new HashSet<>();

    public void setSite(Site site) {
        this.site = site;
        site.addLemma(this);
    }

    protected void addIndex(Index index) {
        getIndexes().add(index);
    }

    public void incrementFrequency() {
        frequency++;
    }

    public void decrementFrequency() {
        frequency--;
    }
}
