package my.searchengine.model;

import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.util.Objects;

@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class URL {
    @Getter
    private String url;
    @Getter
    private String urlWithoutProtocol;
    @Getter
    private String host;
    @Getter @Setter
    private boolean isBad;

    public URL(String url) {
        this.url = url.intern();
        this.urlWithoutProtocol = url.replaceFirst(".+://", "");
        String[] urlPathArray = url.split("/");
        this.host = urlPathArray.length < 3 ? null : urlPathArray[2].intern();
    }

    public void setUrl(String url) {
        this.url = url;
        this.urlWithoutProtocol = url.replaceFirst(".+://", "");
    }
    public String getUrlWithoutProtocolAndParameters() {
        return this.urlWithoutProtocol.replaceFirst("\\?.*", "");
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
