package searchengine.model.entities;

import lombok.*;

import javax.persistence.*;

@Entity
@Setter
@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Table(name = "`indexes`",uniqueConstraints = @UniqueConstraint(columnNames = {"page", "lemma"}))
public class IndexEntity {
    @Id
    @Column(nullable = false)
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private int id;
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(columnDefinition = "INT", name = "page", nullable = false)
    private PageEntity page;
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(columnDefinition = "INT", name = "lemma", nullable = false)
    private LemmaEntity lemma;
    @Column(name = "`rank`", nullable = false)
    private float rank;
}
