package searchengine.services;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import searchengine.config.SitesListConfig;
import searchengine.dto.response.Response;
import searchengine.dto.response.StatisticsResponse;
import searchengine.dto.statistics.DetailedStatistics;
import searchengine.dto.statistics.StatisticsDto;
import searchengine.dto.statistics.TotalStatisticsDto;
import searchengine.models.Site;
import searchengine.repositoies.LemmaRepository;
import searchengine.repositoies.PageRepository;
import searchengine.repositoies.SiteRepository;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;

@Service
@RequiredArgsConstructor
public class StatisticsService {
    private final SiteRepository siteRepository;
    private final PageRepository pageRepository;
    private final LemmaRepository lemmaRepository;
    private final SitesListConfig sitesFromConfig;

    public Response getStatistics() {
        TotalStatisticsDto totalStatistic = new TotalStatisticsDto();
        List<Site> foundSites = siteRepository.findAll();
        totalStatistic.setSites(foundSites.size());
        totalStatistic.setIndexing(true);

        List<DetailedStatistics> detailed;

        if (foundSites.isEmpty()) {
            detailed = getEmptyStatistic(totalStatistic);
        } else {
            detailed = getStatisticsFromMemory(totalStatistic, foundSites);
        }

        StatisticsDto statisticsDto = StatisticsDto.builder()
                .total(totalStatistic)
                .detailed(detailed)
                .build();

        return new StatisticsResponse(statisticsDto);
    }

    private List<DetailedStatistics> getEmptyStatistic(TotalStatisticsDto total) {
        List<DetailedStatistics> detailed = new ArrayList<>();

        sitesFromConfig.getSites().forEach(site -> {
            DetailedStatistics itemSite = DetailedStatistics.builder()
                    .name(site.getName())
                    .url(site.getUrl())
                    .pages(0)
                    .lemmas(0)
                    .status(null)
                    .error("Сайт не проиндексирован")
                    .statusTime(new Date().getTime())
                    .build();

            detailed.add(itemSite);
        });

        total.setPages(0);
        total.setLemmas(0);

        return detailed;
    }

    private List<DetailedStatistics> getStatisticsFromMemory(TotalStatisticsDto total, List<Site> sites) {
        List<DetailedStatistics> detailedStatistics = new ArrayList<>();

        sites.forEach(site -> {
            DetailedStatistics itemSite = new DetailedStatistics();
            itemSite.setName(site.getName());
            itemSite.setUrl(site.getUrl());

            int pages = pageRepository.getCountId(site.getId());
            itemSite.setPages(pages);

            int lemmas = lemmaRepository.getCountBySiteId(site.getId());
            itemSite.setLemmas(lemmas);

            itemSite.setStatus(site.getStatus().toString());

            if (site.getLastError() != null) {
                itemSite.setError(site.getLastError());
            } else {
                itemSite.setError("");
            }

            itemSite.setStatusTime(site.getStatusTime().getTime());
            total.setPages(total.getPages() + pages);
            total.setLemmas(total.getLemmas() + lemmas);
            detailedStatistics.add(itemSite);
        });

        return detailedStatistics;
    }
}