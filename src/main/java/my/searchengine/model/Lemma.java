package my.searchengine.model;

import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import javax.persistence.*;
import java.util.LinkedList;
import java.util.List;
import java.util.Objects;

@Getter @Setter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@Entity
@Table(name = "lemma")
public class Lemma {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private int id;
    private String lemma;
    private int frequency;
    @Transient
    private int siteId;
    @ManyToOne
    @JoinColumn(name = "site_id", referencedColumnName = "id")
    private Site site;
    @ManyToMany
    @JoinTable(name = "`index`",
                joinColumns = @JoinColumn(name = "lemma_id"),
                inverseJoinColumns = @JoinColumn(name = "page_id"))
    private List<Page> pageList = new LinkedList<>(); // TODO: 18.11.2022 Подумать над инициацией

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
