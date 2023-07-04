package searchengine.services;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;
import searchengine.dto.response.ErrorResponse;
import searchengine.dto.response.Response;
import searchengine.dto.response.SearchingResponse;
import searchengine.dto.searching.SearchingDto;
import searchengine.models.Lemma;
import searchengine.models.Page;
import searchengine.utils.QueryLemmaBuilder;
import searchengine.utils.RepositoryUtils;
import searchengine.utils.SearchingUtils;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Optional;

@Service
@RequiredArgsConstructor
public class SearchingService {
    private final RepositoryUtils repositoryUtils;

    public Response searching(String query, String url, int offset, int limit) {
        if (!isValidQuery(query)) {
            return new ErrorResponse("Задан пустой поисковый запрос");
        }

        try {
            List<Lemma> lemmaSortedList = new ArrayList<>(QueryLemmaBuilder
                    .makeLemmaSortedList(query, url, repositoryUtils));

            List<Page> pageList = new ArrayList<>();
            lemmaSortedList.forEach(lemma -> updatePageList(pageList, lemma));
            List<Page> uniquePageList = pageList.stream()
                    .distinct()
                    .toList();

            List<SearchingDto> searchingItemList = SearchingUtils.makeDetailedSearchingItem(uniquePageList, lemmaSortedList,
                    repositoryUtils, query);

            List<SearchingDto> sortedSearchingItemList = searchingItemList.stream()
                    .sorted(Comparator.comparingDouble(SearchingDto::getRelevance).reversed())
                    .toList();

            int resultCount = sortedSearchingItemList.size();

            if (offset > 0 && sortedSearchingItemList.size() > offset) {
                sortedSearchingItemList = sortedSearchingItemList.stream().skip(offset).toList();
            }

            return new SearchingResponse(resultCount,
                    sortedSearchingItemList.stream()
                            .limit(limit)
                            .toList());

        } catch (IOException ex) {
            ex.printStackTrace();
            return new ErrorResponse("Задан пустой поисковый запрос");
        }
    }

    private void updatePageList(List<Page> pageList, Lemma lemma) {
        pageList.addAll(repositoryUtils.getSearchIndexRepository().findPageIdByLemma(lemma.getId())
                .stream()
                .map(pageId -> repositoryUtils.getPageRepository().findById(pageId))
                .filter(Optional::isPresent)
                .map(Optional::get)
                .toList());
    }

    private boolean isValidQuery(String query) {
        if (!StringUtils.hasLength(query)) {
            return false;
        }

        return repositoryUtils.getLemmaRepository().getCount() != 0 &&
                repositoryUtils.getSiteRepository().findCount() != 0;
    }
}
