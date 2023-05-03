package searchengine.model;

import com.sun.istack.NotNull;

import javax.persistence.*;
import java.time.LocalDateTime;

@Entity
public class Site {
    @Id
    @NotNull
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private int id;
    @NotNull
    @Column(columnDefinition = "ENUM('INDEXING', 'INDEXED', 'FAILED')")
    private Status status;
    @NotNull
    private LocalDateTime status_time;
    @Column(columnDefinition = "TEXT")
    private String last_error;
    @NotNull
    @Column(columnDefinition = "VARCHAR(255)")
    private String url;
    @NotNull
    @Column(columnDefinition = "VARCHAR(255)")
    private String name;//site name
}
