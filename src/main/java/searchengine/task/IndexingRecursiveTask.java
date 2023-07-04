package searchengine.task;

import lombok.AllArgsConstructor;
import lombok.Getter;
import org.jsoup.Connection;
import searchengine.models.Lemma;
import searchengine.models.Page;
import searchengine.models.Site;
import searchengine.services.IndexingService;
import searchengine.utils.ConnectionUtils;
import searchengine.utils.PageUtils;
import searchengine.utils.RepositoryUtils;

import java.io.IOException;
import java.util.*;
import java.util.concurrent.ForkJoinTask;
import java.util.concurrent.RecursiveAction;

@AllArgsConstructor
@Getter
public class IndexingRecursiveTask extends RecursiveAction {
    private String url;
    private Site site;
    private RepositoryUtils repositoryUtils;

    @Override
    protected void compute() {
        if (!IndexingService.isIndexingNow) Thread.currentThread().interrupt();

        Set<IndexingRecursiveTask> taskSet = Collections.synchronizedSet(new HashSet<>());
        Set<String> linkSet = Collections.synchronizedSet(new HashSet<>());

        try {
            Optional<Connection.Response> response = ConnectionUtils.makeConnection(url, site, repositoryUtils);
            if (response.isEmpty()) return;

            Optional<Page> optionalPage = PageUtils.getPage(
                    response.get(),
                    new HashMap<>() {{
                        put(site, url);
                    }},
                    repositoryUtils
            );

            optionalPage.ifPresent(page -> {
                if (page.getCode() == 200) {
                    repositoryUtils.addPageToDB(page);
                    Map<Page, Set<Lemma>> indexMap = repositoryUtils.addLemmaToDBAndReturnData(page);
                    repositoryUtils.addIndexToDB(indexMap);
                }
            });

            ConnectionUtils.parseUrl(response.get(), url).forEach(link -> {
                if (!linkSet.contains(link)) {
                    IndexingRecursiveTask task = new IndexingRecursiveTask(link, site, repositoryUtils);
                    taskSet.add(task);
                    linkSet.add(link);
                }
            });

            ForkJoinTask.invokeAll(taskSet);

        } catch (IOException e) {
            e.printStackTrace();
            repositoryUtils.setFailedSite(site);
        } catch (InterruptedException ignored) {

        }
    }
}
