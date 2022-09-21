package my.searchengine.model;

import java.util.Objects;

public class Lemma {
    private int id;
    private String lemma;
    private int frequency;
    private int siteId;

    public Lemma(){}
    public Lemma(String lemma, int frequency) {
        this.lemma = lemma;
        this.frequency = frequency;
    }
    public int getId() {
        return id;
    }
    public void setId(int id) {
        this.id = id;
    }
    public String getLemma() {
        return lemma;
    }
    public void setLemma(String lemma) {
        this.lemma = lemma;
    }
    public int getFrequency() {
        return frequency;
    }
    public void setFrequency(int frequency) {
        this.frequency = frequency;
    }
    public int getSiteId() {
        return siteId;
    }
    public void setSiteId(int siteId) {
        this.siteId = siteId;
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
