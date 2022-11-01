package my.searchengine.model;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDateTime;

@Getter @Setter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Site {
    private int id;
    private Status status;
    private LocalDateTime statusTime;
    private String lastError;
    @JsonProperty("url")
    private String host;
    private String name;

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
