package searchengine.model.entities;

import lombok.*;

import javax.persistence.*;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Entity
@Setter
@Getter
@Builder
@Table(name = "sites")
@NoArgsConstructor
@AllArgsConstructor
public class SiteEntity {
    @Id
    @Column(nullable = false)
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private int id;
    @Enumerated(EnumType.STRING)
    @Column(columnDefinition = "ENUM('INDEXING', 'INDEXED', 'FAILED')", nullable = false)
    private Status status;
    @Column(name = "status_time", nullable = false)
    private LocalDateTime statusTime;
    @Column(columnDefinition = "TEXT", name = "last_error")
    private String lastError;
    @Column(columnDefinition = "VARCHAR(255)", nullable = false)
    private String url;
    @Column(columnDefinition = "VARCHAR(255)", nullable = false)
    private String name;//site name
    @OneToMany(mappedBy = "site", cascade = CascadeType.MERGE)
    private List<PageEntity> pages = new ArrayList<>();
    @OneToMany(mappedBy = "site", cascade = CascadeType.MERGE)
    private List<LemmaEntity> lemmas = new ArrayList<>();
}