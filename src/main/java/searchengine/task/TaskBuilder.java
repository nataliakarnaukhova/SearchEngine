package searchengine.task;

import org.apache.log4j.Logger;
import searchengine.models.Site;
import searchengine.enums.Status;
import searchengine.services.IndexingService;
import searchengine.utils.RepositoryUtils;

import java.util.List;
import java.util.Optional;
import java.util.concurrent.ForkJoinPool;

public class TaskBuilder {
    private static final Logger log = Logger.getLogger(IndexingService.class);

    public static void makeIndexingOneSiteTask(Site site, RepositoryUtils repositoryUtils) {
        repositoryUtils.oneSiteIndexingPrepare(site);
        repositoryUtils.addNewSiteToDB(site);

        IndexingRecursiveTask task = makeTask(site, repositoryUtils);
        new ForkJoinPool().invoke(task);

        setSiteIndexed(site, repositoryUtils);
        IndexingService.isIndexingNow = false;

        log.info("Индексация сайта " + site.getUrl() + " завершена");
    }

    public static synchronized void makeIndexingAllSiteTask(List<Site> siteList, RepositoryUtils repositoryUtils) {
        siteList.forEach(repositoryUtils::addNewSiteToDB);
        siteList.forEach(site -> {
            IndexingRecursiveTask task = makeTask(site, repositoryUtils);
            new ForkJoinPool().invoke(task);
            setSiteIndexed(site, repositoryUtils);

            List<Site> notIndexedSiteList = repositoryUtils.getSiteRepository()
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

    private static synchronized IndexingRecursiveTask makeTask(Site site, RepositoryUtils repositoryUtils) {
        log.info("Индексация страницы " + site.getUrl());
        return new IndexingRecursiveTask(site.getUrl(), site, repositoryUtils);
    }

    private static synchronized void setSiteIndexed(Site site, RepositoryUtils repositoryUtils) {
        Optional<Site> optionalSite = repositoryUtils.getSite(site.getUrl());
        optionalSite.ifPresent(existSite -> {
            if (!existSite.getStatus().equals(Status.FAILED)) {
                repositoryUtils.setIndexedSite(existSite);
            }
        });
    }
}
