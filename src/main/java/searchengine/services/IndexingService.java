package searchengine.services;

import lombok.RequiredArgsConstructor;
import org.apache.log4j.Logger;
import org.springframework.stereotype.Service;
import searchengine.config.SitesListConfig;
import searchengine.dto.response.ErrorResponse;
import searchengine.dto.response.Response;
import searchengine.dto.response.SuccessResponse;
import searchengine.models.Site;
import searchengine.utils.RepositoryUtils;
import searchengine.task.TaskBuilder;

import java.util.List;
import java.util.Set;
import java.util.concurrent.ForkJoinPool;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class IndexingService {
    public static volatile boolean isIndexingNow;
    private final Logger log = Logger.getLogger(IndexingService.class);
    private final SitesListConfig sitesFromConfig;
    private final RepositoryUtils repositoryUtils;

    public Response startIndexing() {
        if (isIndexingNow) {
            log.error("Ошибка при запуске индексации. Индексация страницы уже запущена");
            return new ErrorResponse("Индексация уже запущена");
        }

        IndexingService.isIndexingNow = true;
        repositoryUtils.cleanRepositories();

        List<Site> siteList = sitesFromConfig.getSites()
                .stream()
                .map(item -> {
                    Site site = new Site();
                    site.setUrl(item.getUrl());
                    site.setName(item.getName());
                    return site;
                }).toList();

        new Thread(() -> TaskBuilder.makeIndexingAllSiteTask(siteList, repositoryUtils)).start();

        return new SuccessResponse();
    }

    public Response stopIndexing() {
        if (!isIndexingNow) {
            log.error("Ошибка при остановке индексации. Индексация страниц не запущена");
            return new ErrorResponse("Индексация не запущена");
        }

        ForkJoinPool.commonPool().shutdownNow();
        isIndexingNow = false;

        repositoryUtils.stopIndexing();

        return new SuccessResponse();
    }

    public Response startIndexingOneSite(String url) {
        String regex = "^((http|https)://)?(w{3}.)?";
        String destSite = url.replaceAll(regex, "");

        Set<String> sitesSet = sitesFromConfig.getSites()
                .stream()
                .map(siteConfig -> siteConfig.getUrl().replaceAll(regex, ""))
                .collect(Collectors.toSet());

        boolean isCorrectPage = sitesSet.contains(destSite);

        if (!isCorrectPage) {
            log.error("Сайт '" + url + "' находится за пределами сайтов, указанных в конфигурационном файле");
            return new ErrorResponse("Данная страница находится за пределами сайтов, указанных в конфигурационном файле");
        }

        if (isIndexingNow) {
            log.error("Ошибка при запуске индексации. Индексация страницы уже запущена");
            return new ErrorResponse("Индексация уже запущена");
        }

        isIndexingNow = true;

        Site site = new Site();
        sitesFromConfig.getSites()
                .stream()
                .filter(siteConfig -> siteConfig.getUrl().replaceAll(regex, "").equals(destSite))
                .forEach(siteConfig -> {
                    site.setName(siteConfig.getName());
                    site.setUrl(siteConfig.getUrl());
                });

        log.info("Сайт '" + site.getUrl() + "' добавлен в очередь на индексацию");
        new Thread(() -> TaskBuilder.makeIndexingOneSiteTask(site, repositoryUtils)).start();

        return new SuccessResponse();
    }
}