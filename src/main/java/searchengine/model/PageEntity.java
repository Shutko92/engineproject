package searchengine.model;

import lombok.Getter;
import lombok.Setter;

import javax.persistence.*;

@Entity
@Setter
@Getter
@Table(name = "pages", indexes = {@Index(name = "idx_page_path", columnList = "path", unique = true)},
uniqueConstraints = @UniqueConstraint(columnNames = {"site_id", "path"}))
public class PageEntity {
    @Id
    @Column(nullable = false)
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private int id;
    @ManyToOne(cascade = CascadeType.ALL, fetch = FetchType.LAZY)
    @JoinColumn(columnDefinition = "INT", name = "site_id", nullable = false)
    private SiteEntity siteId;
    @Column(columnDefinition = "TEXT", nullable = false)
    private String path;//needs index
    @Column(columnDefinition = "INT", nullable = false)
    private int code;
    @Column(columnDefinition = "MEDIUMTEXT", nullable = false)
    private String content;

    public static PageBuilder builder() {
        return new PageBuilder();
    }

    public static class PageBuilder {
        private int id;
        private SiteEntity siteId;
        private String path;
        private int code;
        private String content;

        public PageBuilder siteId(SiteEntity siteId) {
            this.siteId = siteId;
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
            return new PageEntity();
        }
    }
}
