package searchengine.model;

import com.sun.istack.NotNull;

import javax.persistence.*;

@Entity
public class Page {
    @Id
    @NotNull
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private int id;
    @NotNull
    @Column(columnDefinition = "INT")
    private int site_id;
    @NotNull
    @Column(columnDefinition = "TEXT")
    private String path;
    @NotNull
    @Column(columnDefinition = "INT")
    private int code;
    @NotNull
    @Column(columnDefinition = "MEDIUMTEXT")
    private String content;
}
