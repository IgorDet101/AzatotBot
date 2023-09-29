package searchengine.model;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import javax.persistence.*;

@Entity
@Table(name = "\"Index\"")
@Getter
@Setter
@NoArgsConstructor
public class Index {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private int id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "page_id", nullable = false)
    private Page page;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "lemma_id", nullable = false)
    private Lemma lemma;

    //@Column(name = "find_rank", nullable = false)
    @Column(name = "\"rank\"", nullable = false)
    private float rank;

    public void setPage (Page page){
        this.page = page;
        page.addIndex(this);
    }

    public void setLemma (Lemma lemma){
        this.lemma = lemma;
        lemma.addIndex(this);
    }
}
