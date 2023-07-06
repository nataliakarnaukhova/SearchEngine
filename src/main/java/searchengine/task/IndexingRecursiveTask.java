package searchengine.task;

import lombok.AllArgsConstructor;
import lombok.Getter;
import org.apache.log4j.Logger;
import org.jsoup.Connection;
import org.jsoup.HttpStatusException;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import searchengine.enums.Status;
import searchengine.models.Lemma;
import searchengine.models.Page;
import searchengine.models.Site;
import searchengine.services.IndexingService;
import searchengine.services.PageService;
import searchengine.services.RepositoryService;

import java.io.IOException;
import java.net.SocketTimeoutException;
import java.util.*;
import java.util.concurrent.ForkJoinTask;
import java.util.concurrent.RecursiveAction;

@AllArgsConstructor
@Getter
public class IndexingRecursiveTask extends RecursiveAction {
    private final Logger log = Logger.getLogger(IndexingService.class);
    private final RepositoryService repositoryService;
    private String url;
    private Site site;

    @Override
    protected void compute() {
        if (!IndexingService.isIndexingNow) {
            Thread.currentThread().interrupt();
        }

        Set<IndexingRecursiveTask> taskSet = Collections.synchronizedSet(new HashSet<>());
        Set<String> linkSet = Collections.synchronizedSet(new HashSet<>());

        try {
            Optional<Connection.Response> response = makeConnection(url);
            if (response.isEmpty()) return;

            Optional<Page> optionalPage = PageService.getPage(response.get(), new HashMap<>(Map.of(site, url)),
                    repositoryService);

            optionalPage.ifPresent(page -> {
                if (page.getCode() == 200) {
                    repositoryService.savePage(page);
                    Map<Page, Set<Lemma>> indexMap = repositoryService.saveLemma(page);
                    repositoryService.saveIndex(indexMap);
                }
            });

            parseUrl(response.get(), url).forEach(link -> {
                if (!linkSet.contains(link)) {
                    IndexingRecursiveTask task = new IndexingRecursiveTask(repositoryService, link, site);
                    taskSet.add(task);
                    linkSet.add(link);
                }
            });

            ForkJoinTask.invokeAll(taskSet);

        } catch (IOException e) {
            e.printStackTrace();
            saveSiteWithFiledStatus(site);
            IndexingService.isIndexingNow = false;
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
    }

    private void saveSiteWithFiledStatus(Site site) {
        site.setStatus(Status.FAILED);
        site.setStatusTime(new Date());
        repositoryService.saveSite(site);
    }

    private Optional<Connection.Response> makeConnection(String url) throws IOException, InterruptedException {
        try {
            log.info("Индексация страницы: " + url);

            int timeout = (int) Math.round(1 + Math.random() * 5);
            Thread.sleep(timeout);

            Connection connection = getConnection(url);

            Connection.Response execute = connection.execute();

            return Optional.of(execute.bufferUp());

        } catch (HttpStatusException exception) {
            processingErrorConnection(exception.getStatusCode(), exception);
            IndexingService.isIndexingNow = false;
        } catch (SocketTimeoutException exception) {
            processingErrorConnection(408, exception);
        }

        return Optional.empty();
    }

    private Connection getConnection(String url) {
        return Jsoup.connect(url)
                .timeout(10000)
                .userAgent("Mozilla/5.0 (compatible; Googlebot/2.1; +http://www.google.com/bot.html)")
                .referrer("http://www.google.com")
                .ignoreContentType(true)
                .method(Connection.Method.GET);
    }


    private List<String> parseUrl(Connection.Response response, String url) throws IOException {
        Document doc = response.parse();
        return doc.select("a[href]")
                .stream()
                .map(element -> element.absUrl("href"))
                .filter(link -> isCorrectLink(link, url))
                .distinct()
                .toList();
    }

    private boolean isCorrectLink(String link, String url) {
        return link.startsWith(url) && !link.equals(url)
                && !link.matches("([http|https|ftp:]+)/{2}([\\D\\d]+)[.]([doc|pdf|rtf|mp4|mp3]+)")
                && !link.contains("#")
                && !link.contains("?");
    }

    private void processingErrorConnection(Integer statusCode, Exception exception) {
        exception.printStackTrace();

        Page page = PageService.getPageWithError(site, url, statusCode);
        repositoryService.savePage(page);
        repositoryService.saveSiteWithLastErrorIndexing(site, statusCode, exception.getLocalizedMessage());

        log.error(exception.getClass().getName() + ": " + exception.getMessage());
    }
}
