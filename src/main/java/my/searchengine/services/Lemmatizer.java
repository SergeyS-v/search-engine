package my.searchengine.services;

import my.searchengine.AppProp;
import my.searchengine.model.Lemma;
import org.apache.lucene.morphology.LuceneMorphology;
import org.apache.lucene.morphology.WrongCharaterException;
import org.apache.lucene.morphology.russian.RussianLuceneMorphology;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.stream.Collectors;

@Service
public class Lemmatizer {

    private final AppProp appProp;
    private final LuceneMorphology luceneMorphology;
    private final String wordInfoExceptRegex;
    static final Logger logger = LoggerFactory.getLogger("morfError");

    public Lemmatizer(AppProp appProp) {
        this.appProp = appProp;
        wordInfoExceptRegex = getWordInfoExceptRegex();
        this.luceneMorphology = getLuceneMorphology();
    }

    private String getWordInfoExceptRegex(){
        return String.format(".+(%s).*",
                appProp.getWordInfoToExcept().stream()
                        .map(x -> "("+x+")")
                        .collect(Collectors.joining("|")));
    }
    private LuceneMorphology getLuceneMorphology()  {
        try {
            return new RussianLuceneMorphology();
        } catch (IOException e) {
            logger.error("ошибка создания RussianLuceneMorphology", e);
            return null;
        }
    }

    private void countLemma(String baseForm, HashMap<String, Integer> lemmaCountMap){
        lemmaCountMap.merge(baseForm, 1, Integer::sum);
    }

    public HashMap<String, Lemma> getLemmasFromString(String text){
        HashMap<String, Integer> lemmaCountMap = new HashMap<>();
        text.lines()
                .map(x -> x.replaceAll("\\p{Punct}" ,""))
                .flatMap(x -> Arrays.stream(x.split(" ")))
                .map(String::toLowerCase)
                .filter(x -> isWord(x))
                .flatMap(x -> luceneMorphology.getNormalForms(x).stream())
                .map(x -> x.replaceAll("ё", "е"))
                .forEach(x -> countLemma(x, lemmaCountMap));
        HashMap<String, Lemma> lemmaMap = new HashMap<>();
        lemmaCountMap.forEach((lemma, frequency) -> lemmaMap.put(lemma, new Lemma(lemma, frequency)));
        return lemmaMap;
    }

    protected boolean isWord(String word) {
        try {
            if (word.isEmpty()) {
                return false;
            }
            List<String> wordBaseForms = luceneMorphology.getMorphInfo(word);
            for (String wordInfo : wordBaseForms) {
                if (wordInfo.matches(wordInfoExceptRegex)){  // ".+((СОЮЗ)|(МЕЖД)|(ПРЕДЛ)|(МС)|(ЧАСТ)).*"
                    return false;
                }
            }
        } catch (WrongCharaterException e) {
            return false;
        } catch (Exception e) {
            logger.error("Ошибка морфологического разбора слова: " + word + " -> " + e.getMessage());
            return false;
        }
        return true;
    }
}
