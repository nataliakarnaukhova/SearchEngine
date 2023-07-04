package searchengine.utils;

import lombok.AllArgsConstructor;
import org.jsoup.Connection;
import searchengine.models.Page;
import searchengine.models.Site;
import searchengine.repositoies.PageRepository;
import searchengine.repositoies.SiteRepository;

import java.io.IOException;
import java.util.HashMap;
import java.util.Optional;

@AllArgsConstructor
public class PageUtils {
    public static synchronized Optional<Page> getPage(Connection.Response response, HashMap<Site, String> siteMap,
                                                      RepositoryUtils repositoryUtils) throws IOException {
        Optional<Site> site = siteMap.keySet().stream().findFirst();
        Optional<String> url = siteMap.values().stream().findFirst();

        if (site.isEmpty()) {
            return Optional.empty();
        }
        String path = makePath(site.get(), url.get());

        if (checkForAlreadyExistPath(path, repositoryUtils, site.get().getUrl())) {
            return Optional.empty();
        }

        Page page = new Page();
        page.setPath(path);
        page.setSite(site.get());
        page.setCode(response.statusCode());
        page.setContext(response.parse().html());

        return Optional.of(page);
    }

    public static synchronized Page getPageWithError(Site site, String url, int statusCode) {
        Page page = new Page();
        page.setCode(statusCode);
        page.setSite(site);
        page.setPath(makePath(site, url));

        return page;
    }

    private static synchronized String makePath(Site site, String url) {
        return url.equals(site.getUrl()) ?
                url.replaceAll(site.getUrl(), "/") :
                url.replaceAll(site.getUrl(), "");
    }

    private static synchronized boolean checkForAlreadyExistPath(String path, RepositoryUtils repositoryUtils, String url) {
        SiteRepository siteRepository = repositoryUtils.getSiteRepository();
        PageRepository pageRepository = repositoryUtils.getPageRepository();

        Optional<Site> site = siteRepository.findByUrl(url);
        if (site.isEmpty()) {
            return false;
        }

        return pageRepository.findByPathAndSiteId(path, site.get().getId()).isPresent();
    }

}
