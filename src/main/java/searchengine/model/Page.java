package searchengine.model;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import javax.persistence.*;
import javax.persistence.Index;
import java.util.HashSet;
import java.util.Set;

@Entity
@Table(name = "Pages",
        indexes = @Index(name = "path_index", columnList = "site_id, path", unique = true))
@Getter
@Setter
@NoArgsConstructor
public class Page {
    @Id
    @GeneratedValue(strategy =  GenerationType.AUTO)
    private int id;

    @ManyToOne(optional = false, fetch = FetchType.LAZY)
    @JoinColumn(name = "site_id", nullable = false)
    private Site site;

    @Column(nullable = false)
    private String path;

    @Column(nullable = false)
    private int code;

    @Column(nullable = false, columnDefinition = "mediumtext")
    private String content;

    @OneToMany(mappedBy = "page", cascade = CascadeType.REMOVE, fetch = FetchType.LAZY)
    private Set<searchengine.model.Index> indexes = new HashSet<>();

    public void setSite (Site site){
        this.site = site;
        site.addPage(this);
    }

    protected void addIndex (searchengine.model.Index index){
        getIndexes().add(index);
    }
}
