package my.searchengine.services;

import lombok.AllArgsConstructor;
import my.searchengine.AppProp;
import my.searchengine.dao.DaoController;
import my.searchengine.model.Index;
import my.searchengine.model.Lemma;
import my.searchengine.model.Page;
import my.searchengine.model.URL;
import org.springframework.stereotype.Service;

import java.util.*;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.stream.Collectors;

@AllArgsConstructor
@Service
public class PagesController {

    private final DaoController daoController;
    private final AppProp appProp;
    public final Queue<Page> pageQueue = new ConcurrentLinkedQueue<>();

    public void addPage(Page page){
        if (page == null) {
            return;
        }
        pageQueue.add(page);
        checkQueueSizeAndInsertPageBatch(appProp.getPageQueueInsertSize());
    }

    private synchronized void checkQueueSizeAndInsertPageBatch(int threshold){
        Map<String, Page> pageMap = new HashMap<>();
        Page page;
        while ((page = pageQueue.poll()) != null && pageMap.size() < threshold) {
                pageMap.put(page.getPathWithHostName(), page);
        }
            List<Page> doublesPage = daoController.getPageDao().getPagesPathsWithSamePathInPage(pageMap.values());
            doublesPage.forEach(x -> pageMap.remove(x.getPathWithHostName()));
            daoController.getPageDao().insertPageBatch(pageMap.values());
            List<Page> pagesId = daoController.getPageDao().getPagesIdFor(pageMap.values());

            pagesId.forEach(x -> {
                Page pageForId = pageMap.get(x.getPathWithHostName());
                if (pageForId != null) {
                    pageForId.setId(x.getId());
                }
            });

            countAndUpdateLemmasForPages(pageMap);
    }

    private void countAndUpdateLemmasForPages(Map<String, Page> pageMap) {
        pageMap.values().forEach(pageForLemmas -> {
            //Апдэйтим frequency для лемм, которые есть в базе
            pageForLemmas.setSiteId(daoController.getSiteDao().getSiteIdByHost(pageForLemmas.getHostName()));
            HashMap<Lemma, Integer> allLemmasForPage = new HashMap<>();
            pageForLemmas.getBodyLemmas().values().forEach( x -> {
                x.setSiteId(pageForLemmas.getSiteId());
                allLemmasForPage.put(x, x.getFrequency());
            });
            pageForLemmas.getTitleLemmas().values().forEach(x -> {
                Integer frequency = allLemmasForPage.get(x);
                if (frequency != null) {
                    allLemmasForPage.replace(x, frequency + x.getFrequency());
                } else {
                    x.setSiteId(pageForLemmas.getSiteId());
                    allLemmasForPage.put(x, x.getFrequency());
                }
            });
            List<Lemma> sameLemmaFromDb = daoController.getLemmaDao().getSameLemmaFromDBFrequencySorted(allLemmasForPage.keySet(), pageForLemmas.getSiteId());
            if (!pageForLemmas.isOnlyOnePageForIndexing()) {
                sameLemmaFromDb.forEach(x -> {
                    Integer frequency = allLemmasForPage.get(x);
                    x.setFrequency(x.getFrequency() + (frequency == null ? 0 : frequency));
                });
                daoController.getLemmaDao().updateLemmaBatch(sameLemmaFromDb);
            }

            //Вставляем отсутствующие в БД леммы
            HashMap<Lemma, Integer> newLemmaMap = new HashMap<>(allLemmasForPage);
            sameLemmaFromDb.forEach(newLemmaMap::remove);
            daoController.getLemmaDao().insertLemmaBatch(newLemmaMap, pageForLemmas.getSiteId());
            //Получаем из БД все id для лемм этой страницы;
            List<Lemma> lemmaFromDb = daoController.getLemmaDao().getLemmasIdForPage(allLemmasForPage.keySet(), pageForLemmas.getSiteId());
            lemmaFromDb.forEach(lemmaWithId -> {
                Lemma tittlePageLemma = pageForLemmas.getTitleLemmas().get(lemmaWithId.getLemma());
                Lemma bodyPageLemma = pageForLemmas.getBodyLemmas().get(lemmaWithId.getLemma());
                if(tittlePageLemma != null) {
                    tittlePageLemma.setId(lemmaWithId.getId());
                }
                if(bodyPageLemma != null) {
                    bodyPageLemma.setId(lemmaWithId.getId());
                }
            });
            //Считаем и заполняем таблицу Index;
            daoController.getIndexDao().insertIndexBatch(countLemmasRankForPage(pageForLemmas));
        });
    }

    private Set<Index> countLemmasRankForPage(Page page){
        HashMap<Lemma, Float> rankMap = new HashMap<>();
        page.getBodyLemmas().values().forEach(bodyLemma -> rankMap.put(bodyLemma, bodyLemma.getFrequency() * appProp.getBodyWeight()));
        page.getTitleLemmas().values().forEach(titleLemma -> {
            Float newRank = rankMap.computeIfPresent(titleLemma, (lemma, rank) ->
                    rank + titleLemma.getFrequency() * appProp.getTitleWeight());
            if (newRank == null) {
                rankMap.put(titleLemma, titleLemma.getFrequency() * appProp.getTitleWeight());
            }
        });
        HashSet<Index> indexesSet = new HashSet<>();
        rankMap.forEach((lemma, rank) -> indexesSet.add(new Index(page.getId(), lemma.getId(), rank)));
        return indexesSet;
    }

    public void cleanBadUrls(Collection<URL> urls){
        urls.removeIf(URL::isBad);
    }

    public void cleanDoublesInUrlSet(Collection<URL> urls){
        List<URL> doublesUrlList = daoController.getPageDao().getUrlsWithSamePathInPage(urls);
        doublesUrlList.addAll(doublesUrlList.stream().map(x ->new URL(x.getUrl() + "#")).collect(Collectors.toList()));
        urls.removeAll(doublesUrlList);
    }

    public void insertPageQueue() {
        if(!pageQueue.isEmpty()) {
            checkQueueSizeAndInsertPageBatch(pageQueue.size());
        }
    }
}
