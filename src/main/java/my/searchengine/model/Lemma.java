package my.searchengine.model;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.util.Objects;

@Getter @Setter
@NoArgsConstructor
public class Lemma {
    private int id;
    private String lemma;
    private int frequency;
    private int siteId;

    public Lemma(String lemma, int frequency) {
        this.lemma = lemma;
        this.frequency = frequency;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof Lemma)) return false;
        Lemma lemma = (Lemma) o;
        return Objects.equals(this.lemma, lemma.lemma);
    }

    @Override
    public int hashCode() {
        return Objects.hash(lemma);
    }
}
