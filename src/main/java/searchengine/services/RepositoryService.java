package searchengine.services;

import lombok.Getter;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Isolation;
import org.springframework.transaction.annotation.Transactional;
import searchengine.enums.Status;
import searchengine.models.Lemma;
import searchengine.models.Page;
import searchengine.models.SearchIndex;
import searchengine.models.Site;
import searchengine.repositoies.LemmaRepository;
import searchengine.repositoies.PageRepository;
import searchengine.repositoies.SearchIndexRepository;
import searchengine.repositoies.SiteRepository;

import java.util.*;

@Service
@RequiredArgsConstructor
@Getter
public class RepositoryService {
    private final SiteRepository siteRepository;
    private final PageRepository pageRepository;
    private final LemmaRepository lemmaRepository;
    private final SearchIndexRepository searchIndexRepository;

    @Transactional(isolation = Isolation.SERIALIZABLE)
    public void cleanAllRepositories() {
        siteRepository.deleteAll();
        pageRepository.deleteAll();
        lemmaRepository.deleteAll();
        searchIndexRepository.deleteAll();
    }

    @Transactional(isolation = Isolation.SERIALIZABLE)
    public void stopIndexing() {
        siteRepository.findAll().forEach(site -> {
            if (site.getStatus().equals(Status.INDEXING)) {
                site.setStatus(Status.FAILED);
                site.setLastError("Индексация остановлена пользователем");
                site.setStatusTime(new Date());
                siteRepository.save(site);
            }
        });
    }

    @Transactional(isolation = Isolation.SERIALIZABLE)
    public void cleanDataSiteForIndexing(Site site) {
        Optional<Site> optionalSite = siteRepository.findByUrl(site.getUrl());
        optionalSite.ifPresent(alreadyExistSite -> {
            List<Page> alreadyExistPageList = pageRepository.findAllBySite(alreadyExistSite);

            siteRepository.deleteById(alreadyExistSite.getId());
            pageRepository.deleteAllBySite(alreadyExistSite);
            lemmaRepository.deleteAllBySite(alreadyExistSite);
            alreadyExistPageList.forEach(page -> searchIndexRepository.deleteByPageId(page.getId()));
        });
    }

    @Transactional(isolation = Isolation.SERIALIZABLE)
    public void saveNewSite(Site site) {
        site.setStatus(Status.INDEXING);
        site.setStatusTime(new Date());
        site.setName(site.getName());
        site.setUrl(site.getUrl());
        siteRepository.saveAndFlush(site);
    }

    @Transactional(isolation = Isolation.SERIALIZABLE)
    public void saveSiteWithLastErrorIndexing(Site site, int statusCode, String message) {
        Optional<Site> optionalSite = siteRepository.findByUrl(site.getUrl());
        if(optionalSite.isPresent()) {
            site.setLastError("Ошибка подключения. Код ошибки - " + statusCode + " Причина: " + message);
            site.setStatusTime(new Date());
            siteRepository.saveAndFlush(site);
        }
    }

    @Transactional(isolation = Isolation.SERIALIZABLE)
    public void savePage(Page page) {
        pageRepository.saveAndFlush(page);
    }

    @Transactional(isolation = Isolation.SERIALIZABLE)
    public synchronized Map<Page, Set<Lemma>> saveLemma(Page page) {
        Map<String, Integer> lemmas = Collections.synchronizedMap(LemmaService.getLemmas(page));
        Set<Lemma> lemmaSet = Collections.synchronizedSet(new HashSet<>());

        lemmas.forEach((word, count) -> {
            Optional<Lemma> optionalLemma = lemmaRepository.findByLemmaAndSiteId(word, page.getSite().getId());
            if (optionalLemma.isEmpty()) {
                Lemma lemma = new Lemma();
                lemma.setFrequency(1);
                lemma.setLemma(word);
                lemma.setSite(page.getSite());
                lemma.setRank(count.floatValue());

                lemmaSet.add(lemma);
            } else {
                Lemma lemmaDB = optionalLemma.get();
                int existLemmaFrequency = lemmaDB.getFrequency();
                lemmaDB.setFrequency(++existLemmaFrequency);
                lemmaDB.setRank(count.floatValue());
                
                lemmaSet.add(lemmaDB);
            }
        });

        lemmaRepository.saveAllAndFlush(lemmaSet);
        return new HashMap<>() {{
            put(page, lemmaSet);
        }};
    }

    @Transactional(isolation = Isolation.SERIALIZABLE)
    public void saveSite(Site site) {
        siteRepository.saveAndFlush(site);
    }

    @Transactional(isolation = Isolation.SERIALIZABLE)
    public Optional<Site> getSite(String url) {
        return siteRepository.findByUrl(url);
    }

    @Transactional(isolation = Isolation.SERIALIZABLE)
    public void saveIndex(Map<Page, Set<Lemma>> lemmaMap) {
        Set<SearchIndex> indexSet = Collections.synchronizedSet(new HashSet<>());

        lemmaMap.forEach((page, lemmaSet) -> lemmaSet.forEach(lemma -> {
            SearchIndex index = new SearchIndex();
            index.setLemmaId(lemma.getId());
            index.setPageId(page.getId());
            index.setLemmaRank(lemma.getRank());
            indexSet.add(index);
        }));

        searchIndexRepository.saveAllAndFlush(indexSet);
    }
}
