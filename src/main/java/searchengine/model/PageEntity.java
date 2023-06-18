package searchengine.model;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.RequiredArgsConstructor;
import lombok.Setter;

import javax.persistence.*;
import javax.persistence.Index;

@Entity
@Setter
@Getter
@NoArgsConstructor
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
    private String path;//needs index
    @Column(columnDefinition = "INT", nullable = false)
    private int code;
    @Column(columnDefinition = "MEDIUMTEXT", nullable = false)
    private String content;

    public PageEntity(SiteEntity site, String path, int code, String content) {
        this.site = site;
        this.path = path;
        this.code = code;
        this.content = content;
    }

    public static PageBuilder builder() {
        return new PageBuilder();
    }

    public static class PageBuilder {
        private SiteEntity site;
        private String path;
        private int code;
        private String content;

        public PageBuilder site(SiteEntity site) {
            this.site = site;
            return this;
        }
        public PageBuilder path(String path) {
            this.path = path;
            return this;
        }
        public PageBuilder code(int code) {
            this.code = code;
            return this;
        }
        public PageBuilder content(String content) {
            this.content = content;
            return this;
        }

        public PageEntity build() {
            return new PageEntity(site, path, code, content);
        }
    }
}
