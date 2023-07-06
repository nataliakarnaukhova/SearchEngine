package searchengine.services;

import lombok.RequiredArgsConstructor;
import org.jsoup.Jsoup;
import org.jsoup.select.Elements;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;
import searchengine.dto.response.ErrorResponse;
import searchengine.dto.response.Response;
import searchengine.dto.response.SearchingResponse;
import searchengine.dto.searching.SearchingDto;
import searchengine.models.Lemma;
import searchengine.models.Page;
import searchengine.models.SearchIndex;
import searchengine.models.Site;
import searchengine.repositoies.SearchIndexRepository;

import java.io.IOException;
import java.text.DecimalFormat;
import java.util.*;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class SearchingService {
    private final RepositoryService repositoryService;
    private final AtomicInteger siteId = new AtomicInteger(-1);

    public Response searchText(String query, String url, int offset, int limit) {
        if (!isValidQuery(query)) {
            return new ErrorResponse("Задан пустой поисковый запрос");
        }

        try {
            List<Lemma> lemmas = getSortedLemmas(query, url);
            List<Lemma> lemmaSortedList = new ArrayList<>(lemmas);

            List<Page> pageList = new ArrayList<>();
            lemmaSortedList.forEach(lemma -> updatePageList(pageList, lemma));
            List<Page> uniquePageList = pageList.stream()
                    .distinct()
                    .toList();

            List<SearchingDto> searchingItemList = makeDetailedSearchingItem(uniquePageList, lemmaSortedList,
                    repositoryService, query);

            List<SearchingDto> sortedSearchingItemList = searchingItemList.stream()
                    .sorted(Comparator.comparingDouble(SearchingDto::getRelevance).reversed())
                    .toList();

            int resultCount = sortedSearchingItemList.size();

            if (offset > 0 && sortedSearchingItemList.size() > offset) {
                sortedSearchingItemList = sortedSearchingItemList.stream().skip(offset).toList();
            }

            return new SearchingResponse(resultCount, sortedSearchingItemList.stream().limit(limit).toList());

        } catch (IOException ex) {
            ex.printStackTrace();
            return new ErrorResponse("Задан пустой поисковый запрос");
        }
    }

    private void updatePageList(List<Page> pageList, Lemma lemma) {
        pageList.addAll(repositoryService.getSearchIndexRepository().findPageIdsByLemma(lemma.getId())
                .stream()
                .map(pageId -> repositoryService.getPageRepository().findById(pageId))
                .filter(Optional::isPresent)
                .map(Optional::get)
                .toList());
    }

    private boolean isValidQuery(String query) {
        if (!StringUtils.hasLength(query)) {
            return false;
        }

        return repositoryService.getLemmaRepository().getCountId() != 0 &&
                repositoryService.getSiteRepository().getCountId() != 0;
    }

    private List<SearchingDto> makeDetailedSearchingItem(List<Page> uniquePageList, List<Lemma> lemmaSortedList,
                                                         RepositoryService repositoryService, String query) {
        List<SearchingDto> searchingResult = new ArrayList<>();
        float maxRel = getMaxRelevance(uniquePageList, lemmaSortedList, repositoryService.getSearchIndexRepository());

        uniquePageList.forEach(page -> {
            List<Lemma> lemmaList = new ArrayList<>();
            SearchingDto searchingItem = new SearchingDto();
            AtomicReference<Float> absRelevance = new AtomicReference<>(0.0f);

            lemmaSortedList.forEach(lemma -> getIndexByLemmaId(page.getId(), lemma.getId())
                    .ifPresent(searchIndex -> {
                        absRelevance.set(absRelevance.get() + searchIndex.getLemmaRank());
                        lemmaList.add(lemma);
                    }));

            DecimalFormat decimalFormat = new DecimalFormat("#.##");
            float relRelevance = Float.parseFloat(decimalFormat
                    .format(absRelevance.get() / maxRel).replaceAll(",", ".")
            );

            searchingItem.setRelevance(relRelevance);
            searchingItem.setUri(page.getPath());
            searchingItem.setTitle(Jsoup.parse(page.getContext()).title());

            String snippet = "";
            try {
                snippet = makeSnippet(page, query);
            } catch (IOException e) {
                e.printStackTrace();
            }

            if (!snippet.equals("")) {
                searchingItem.setSnippet(snippet);
                searchingItem.setSite(lemmaList.get(0).getSite().getUrl());
                searchingItem.setSiteName(lemmaList.get(0).getSite().getName());
                searchingResult.add(searchingItem);
            }
        });

        return searchingResult;
    }

    private float getMaxRelevance(List<Page> uniquePageList, List<Lemma> lemmaSortedList,
                                  SearchIndexRepository searchIndexRepository) {
        List<Float> absRelevanceList = new ArrayList<>();
        uniquePageList.forEach(page -> {
            AtomicReference<Float> absRelevance = new AtomicReference<>(0.0f);
            lemmaSortedList.forEach(lemma -> {
                Optional<SearchIndex> index = searchIndexRepository.findByLemmaId(page.getId(), lemma.getId());
                index.ifPresent(searchIndex -> absRelevance.set(absRelevance.get() + searchIndex.getLemmaRank()));
            });
            absRelevanceList.add(absRelevance.get());
        });

        OptionalDouble optionalDouble = absRelevanceList.stream().mapToDouble(Float::floatValue).max();
        float maxRel = 0.0f;
        if ((optionalDouble.isPresent())) {
            maxRel = (float) optionalDouble.getAsDouble();
        }

        return maxRel;
    }

    private Optional<SearchIndex> getIndexByLemmaId(Integer pageId, Integer lemmaId) {
        return repositoryService.getSearchIndexRepository().findByLemmaId(pageId, lemmaId);
    }

    private String makeSnippet(Page page, String query) throws IOException {
        String[] queryStrArr = query.trim().split(" ");
        StringBuilder stringOriginalTextBuilder = new StringBuilder();
        StringBuilder stringTextWithoutEndingBuilder = new StringBuilder();

        Arrays.stream(queryStrArr).forEach(word -> stringOriginalTextBuilder.append(word).append(" "));
        Arrays.stream(queryStrArr).forEach(word -> stringTextWithoutEndingBuilder.append(getRussianWordRoot(word))
                .append("[а-яА-ЯёЁ]*")
                .append(" "));

        String queryText = stringOriginalTextBuilder.toString().trim();
        String queryTextWithoutEnding = stringTextWithoutEndingBuilder.toString().trim();
        String regex = "(" + queryText + "|" + queryTextWithoutEnding + ")";
        Pattern pattern = Pattern.compile(regex, Pattern.CASE_INSENSITIVE | Pattern.UNICODE_CASE);

        String pageText = page.getContext().replaceAll("ё", "е");
        Elements elements = Jsoup.parse(pageText).body().getElementsMatchingOwnText(pattern);

        StringBuilder snippet = new StringBuilder();
        Matcher matcher = pattern.matcher(elements.text());
        if (matcher.find()) {
            int start = matcher.start();
            int end = matcher.end();

            String targetQuery = elements.text().substring(start, end);
            String textBefore = elements.text().substring(0, start);
            String textAfter = elements.text().substring(end);

            if (textBefore.length() > 100) {
                textBefore = " . . . " + elements.text().substring(start - 100, start);
            }
            if (textAfter.length() > 100) {
                textAfter = elements.text().substring(end, end + 100) + " . . . ";
            }

            snippet.append(textBefore)
                    .append(targetQuery.replaceAll(targetQuery, "<b>" + targetQuery + "</b>"))
                    .append(textAfter);
        }

        return snippet.toString();
    }

    private String getRussianWordRoot(String word) {
        if (word.length() == 3) return word;
        if (org.apache.commons.lang3.StringUtils.endsWith(word, "ести"))
            return org.apache.commons.lang3.StringUtils.remove(word, "сти");

        String endingsRegex = "(ий|ей|ой|ая|ие|ый|ые|ть|ти|их|о|а)$";
        String suffixRegex = "(ова|ева|ец|иц)$";
        String suffixWithEnding = "(иться|аться|уться)$";

        return word.replaceAll(endingsRegex, "")
                .replaceAll(suffixRegex, "")
                .replaceAll(suffixWithEnding, "");
    }

    private List<Lemma> getSortedLemmas(String query, String url) throws IOException {
        final int frequencyLimit = Math.round(repositoryService.getLemmaRepository().getMaxFrequency());
        Set<String> queryLemmaSet = LemmaService.getLemmasFromQuery(query);

        if (!StringUtils.hasLength(url)) {
            return getLemmasAllSites(queryLemmaSet, frequencyLimit);
        }
        Optional<Site> optionalSite = repositoryService.getSiteRepository().findByUrl(url);
        optionalSite.ifPresent(site -> siteId.set(site.getId()));

        return makeLemmaSetOneSite(queryLemmaSet, frequencyLimit);
    }

    private List<Lemma> getLemmasAllSites(Set<String> queryLemmaSet, int frequencyLimit) {
        Set<Lemma> lemmaSet = new HashSet<>();
        queryLemmaSet.forEach(lemma -> lemmaSet.addAll(getLemmas(lemma)
                                        .stream()
                                        .filter(lemmaDB -> lemmaDB.getFrequency() <= frequencyLimit)
                                        .collect(Collectors.toSet())));
        return lemmaSet.stream()
                .sorted(Comparator.comparing(Lemma::getFrequency))
                .toList();
    }

    private List<Lemma> makeLemmaSetOneSite(Set<String> queryLemmaSet, int frequencyLimit) {
        Set<Lemma> lemmaSet = new HashSet<>();
        queryLemmaSet.forEach(lemma -> lemmaSet.addAll(getLemmasByLemmaAndSiteId(lemma, siteId.get())
                        .stream()
                        .filter(lemmaDB -> lemmaDB.getFrequency() <= frequencyLimit)
                        .collect(Collectors.toSet()))
        );

        return lemmaSet.stream()
                .sorted(Comparator.comparing(Lemma::getFrequency))
                .toList();
    }

    private List<Lemma> getLemmas(String lemma) {
        return repositoryService.getLemmaRepository().findByLemma(lemma);
    }

    private List<Lemma> getLemmasByLemmaAndSiteId(String lemma, Integer siteId) {
        return repositoryService.getLemmaRepository().findAllByLemmaAndSiteId(lemma, siteId);
    }
}
