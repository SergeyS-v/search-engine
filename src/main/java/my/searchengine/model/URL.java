package my.searchengine.model;

import java.util.Objects;

public class URL {
    private String url;
    private String urlWithoutProtocol;
    private String host;
    private boolean isBad;

    public URL(){}
    public URL(String url) {
        this.url = url.intern();
        this.urlWithoutProtocol = url.replaceFirst(".+://", "");
        String[] urlPathArray = url.split("/");
        this.host = urlPathArray.length < 3 ? null : urlPathArray[2].intern();
    }
    public String getUrl() {
        return url;
    }
    public void setUrl(String url) {
        this.url = url;
        this.urlWithoutProtocol = url.replaceFirst(".+://", "");
    }
    public String getUrlWithoutProtocol() {
        return this.urlWithoutProtocol;
    }
    public String getUrlWithoutProtocolAndParameters() {
        return this.urlWithoutProtocol.replaceFirst("\\?.*", "");
    }
    public String getHost() {
        return this.host;
    }
    public boolean isBad() {
        return isBad;
    }
    public void setBad(boolean bad) {
        isBad = bad;
    }

    @Override
    public String toString() {
        return url + "      | isBad = " + isBad;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof URL)) return false;
        URL url1 = (URL) o;
        return this.getUrlWithoutProtocolAndParameters().equalsIgnoreCase(url1.getUrlWithoutProtocolAndParameters());
    }

    @Override
    public int hashCode() {
        return Objects.hash(getUrlWithoutProtocolAndParameters());
    }
}
