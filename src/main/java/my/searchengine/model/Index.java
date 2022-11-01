package my.searchengine.model;

import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.util.Objects;

@Getter @Setter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Index {
    private int id;
    private int pageId;
    private int lemmaId;
    private float rank;

    public Index(int pageId, int lemmaId, float rank){
        this.lemmaId = lemmaId;
        this.pageId = pageId;
        this.rank = rank;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof Index)) return false;
        Index index = (Index) o;
        return pageId == index.pageId && lemmaId == index.lemmaId;
    }

    @Override
    public int hashCode() {
        return Objects.hash(pageId, lemmaId);
    }
}
