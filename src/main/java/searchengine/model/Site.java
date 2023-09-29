package searchengine.model;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import javax.persistence.*;
import java.util.*;

@Entity
@Table(name = "Sites")
@Getter
@Setter
@NoArgsConstructor
public class Site {
    @Id
    @GeneratedValue(strategy = GenerationType.AUTO)
    private int id;

    @Column(nullable = false)
    @Enumerated(EnumType.STRING)
    private Status status;

    @Column(name = "status_time", nullable = false)
    private Date statusTime;

    @Column(name = "last_error", columnDefinition = "TEXT")
    private String lastError;

    @Column(nullable = false)
    private String url;

    @Column(nullable = false)
    private String name;

    @OneToMany(mappedBy = "site", fetch = FetchType.LAZY, cascade = CascadeType.REMOVE)
    private Set<Page> pages = new HashSet<>();

    @OneToMany(mappedBy = "site", cascade = CascadeType.REMOVE, fetch = FetchType.LAZY)
    private Set<Lemma> lemmas = new HashSet<>();

    protected void addPage (Page page){
        getPages().add(page);
    }

    protected void addLemma (Lemma lemma){
        getLemmas().add(lemma);
    }
}
