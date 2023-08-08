package searchengine.model.entities;

import lombok.*;

import javax.persistence.*;
import javax.persistence.Index;
import java.util.ArrayList;
import java.util.List;

@Entity
@Setter
@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Table(name = "pages", indexes = {@Index(name = "idx_page_path", columnList = "path", unique = true)},
        uniqueConstraints = @UniqueConstraint(columnNames = {"site", "path"}))
public class PageEntity {
    @Id
    @Column(nullable = false)
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private int id;
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(columnDefinition = "INT", name = "site", nullable = false)
    private SiteEntity site;
    @Column(nullable = false)
    private String path;
    @Column(columnDefinition = "INT", nullable = false)
    private int code;
    @Column(columnDefinition = "MEDIUMTEXT", nullable = false)
    private String content;
    @OneToMany(mappedBy = "page", cascade = CascadeType.MERGE)
    private List<IndexEntity> indexes = new ArrayList<>();
}
