package searchengine.task;

import org.apache.log4j.Logger;
import searchengine.enums.Status;
import searchengine.models.Site;
import searchengine.services.IndexingService;
import searchengine.services.RepositoryService;

import java.util.Date;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.ForkJoinPool;

public class TaskBuilder {
    private static final Logger log = Logger.getLogger(IndexingService.class);

    public static void makeTaskIndexingOneSite(Site site, RepositoryService repositoryService) {
        repositoryService.cleanDataSiteForIndexing(site);
        repositoryService.saveNewSite(site);

        IndexingRecursiveTask task = makeTask(site, repositoryService);
        new ForkJoinPool().invoke(task);

        setSiteIndexed(site, repositoryService);
        IndexingService.isIndexingNow = false;

        log.info("Индексация сайта " + site.getUrl() + " завершена");
    }

    public static synchronized void makeTaskIndexingAllSite(List<Site> siteList, RepositoryService repositoryService) {
        siteList.forEach(repositoryService::saveNewSite);
        siteList.forEach(site -> {
            IndexingRecursiveTask task = makeTask(site, repositoryService);
            new ForkJoinPool().invoke(task);
            setSiteIndexed(site, repositoryService);

            List<Site> notIndexedSiteList = repositoryService.getSiteRepository()
                    .findAll()
                    .stream()
                    .filter(siteDB -> siteDB.getStatus().equals(Status.INDEXING))
                    .toList();

            if (notIndexedSiteList.isEmpty()) {
                IndexingService.isIndexingNow = false;
            }
            log.info("Индексация сайта " + site.getUrl() + " завершена");
        });

    }

    private static synchronized IndexingRecursiveTask makeTask(Site site, RepositoryService repositoryService) {
        log.info("Индексация страницы " + site.getUrl());
        return new IndexingRecursiveTask(repositoryService, site.getUrl(), site);
    }

    private static synchronized void setSiteIndexed(Site site, RepositoryService repositoryService) {
        Optional<Site> optionalSite = repositoryService.getSite(site.getUrl());
        optionalSite.ifPresent(existSite -> {
            if (!existSite.getStatus().equals(Status.FAILED)) {
                existSite.setStatus(Status.INDEXED);
                existSite.setStatusTime(new Date());
                repositoryService.saveSite(existSite);
            }
        });
    }
}
