package my.searchengine.model;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import javax.persistence.*;
import java.time.LocalDateTime;
import java.util.LinkedList;
import java.util.List;

@Getter @Setter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@Entity
@Table(name = "site")
public class Site {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private int id;
    @Enumerated(EnumType.STRING)
    private Status status;
    @Column(name = "status_time")
    private LocalDateTime statusTime;
    @Column(name = "last_error")
    private String lastError;
    @JsonProperty("url")
    @Column(name = "url")
    private String host;
    private String name;
    //Новое в JPA:
    @OneToMany(mappedBy = "site")
    private List<Page> pageList = new LinkedList<>(); // TODO: 18.11.2022 Разобраться как инициировать этот список
    @OneToMany(mappedBy = "site")
    private List<Lemma> lemmaList = new LinkedList<>(); // TODO: 18.11.2022 Разобраться как инициировать этот список

    public enum Status {
        INDEXING,
        INDEXED,
        FAILED
    }

    public Site(Status status, String host, String name){
        this.status = status;
        this.statusTime = LocalDateTime.now();
        this.lastError = null;
        this.host = host;
        this.name = name;
    }
    public Site(Status status, String host, String name, String lastError){
        this.status = status;
        this.statusTime = LocalDateTime.now();
        this.lastError = lastError;
        this.host = host;
        this.name = name;
    }
}
