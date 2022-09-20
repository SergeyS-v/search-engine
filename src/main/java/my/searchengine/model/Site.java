package my.searchengine.model;

import com.fasterxml.jackson.annotation.JsonProperty;

import java.time.LocalDateTime;

public class Site {
    private int id;
    private Status status;
    private LocalDateTime statusTime;
    private String lastError;
    private String host;
    private String name;

    public enum Status {
        INDEXING,
        INDEXED,
        FAILED
    }

    public Site(){}
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

    public int getId() {
        return id;
    }
    public void setId(int id) {
        this.id = id;
    }
    public Status getStatus() {
        return status;
    }
    public void setStatus(Status status) {
        this.status = status;
    }
    public LocalDateTime getStatusTime() {
        return statusTime;
    }
    public void setStatusTime(LocalDateTime statusTime) {
        this.statusTime = statusTime;
    }
    public String getLastError() {
        return lastError;
    }
    public void setLastError(String lastError) {
        this.lastError = lastError;
    }
    @JsonProperty("url")
    public String getHost() {
        return host;
    }
    public void setHost(String host) {
        this.host = host;
    }
    public String getName() {
        return name;
    }
    public void setName(String name) {
        this.name = name;
    }
}
