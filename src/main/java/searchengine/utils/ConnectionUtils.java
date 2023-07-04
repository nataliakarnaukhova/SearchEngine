package searchengine.utils;

import org.apache.log4j.Logger;
import org.jsoup.Connection;
import org.jsoup.HttpStatusException;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import searchengine.models.Page;
import searchengine.models.Site;
import searchengine.services.IndexingService;

import java.io.IOException;
import java.net.SocketTimeoutException;
import java.util.List;
import java.util.Optional;

public class ConnectionUtils {
    private final static Logger log = Logger.getLogger(IndexingService.class);

    public static Optional<Connection.Response> makeConnection(
            String url, Site site, RepositoryUtils repositoryUtils) throws IOException, InterruptedException {

        try {
            log.info("Индексация страницы: " + url);

            int timeout = (int) Math.round(1 + Math.random() * 5);
            Thread.sleep(timeout);

            Connection connection = Jsoup.connect(url)
                    .timeout(10000)
                    .userAgent("Mozilla/5.0 (compatible; Googlebot/2.1; +http://www.google.com/bot.html)")
                    .referrer("http://www.google.com")
                    .ignoreContentType(true)
                    .method(Connection.Method.GET);

            Connection.Response execute = connection.execute();

            return Optional.of(execute.bufferUp());

        } catch (HttpStatusException ex) {
            ex.printStackTrace();
            Page page = PageUtils.getPageWithError(site, url, ex.getStatusCode());
            repositoryUtils.addPageToDB(page);
            repositoryUtils.setIndexingLastError(site, ex.getStatusCode(), ex.getLocalizedMessage());

            log.error("HttpStatusException: " + ex.getMessage());


        } catch (SocketTimeoutException ex) {
            ex.printStackTrace();
            Page page = PageUtils.getPageWithError(site, url, 408);
            repositoryUtils.addPageToDB(page);
            repositoryUtils.setIndexingLastError(site, 408, ex.getLocalizedMessage());

            log.error("SocketTimeoutException: " + ex.getMessage());
        }

        return Optional.empty();
    }

    public static List<String> parseUrl(Connection.Response response, String url) throws IOException {
        Document doc = response.parse();
        return doc.select("a[href]")
                .stream()
                .map(element -> element.absUrl("href"))
                .filter(link -> isCorrectLink(link, url))
                .distinct()
                .toList();
    }


    private static boolean isCorrectLink(String link, String url) {
        return link.startsWith(url) && !link.equals(url)
                && !link.matches("([http|https|ftp:]+)/{2}([\\D\\d]+)[.]([doc|pdf|rtf|mp4|mp3]+)")
                && !link.contains("#")
                && !link.contains("?");
    }
}
