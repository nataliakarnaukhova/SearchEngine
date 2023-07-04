package searchengine.utils;

import org.apache.commons.lang3.StringUtils;
import org.jsoup.Jsoup;
import org.jsoup.select.Elements;
import searchengine.dto.searching.SearchingDto;
import searchengine.models.Lemma;
import searchengine.models.Page;
import searchengine.models.SearchIndex;
import searchengine.repositoies.SearchIndexRepository;

import java.io.IOException;
import java.text.DecimalFormat;
import java.util.*;
import java.util.concurrent.atomic.AtomicReference;
import java.util.regex.Matcher;
import java.util.regex.Pattern;


public class SearchingUtils {
    public static List<SearchingDto> makeDetailedSearchingItem(List<Page> uniquePageList, List<Lemma> lemmaSortedList,
                                                               RepositoryUtils repositoryUtils, String query) {
        List<SearchingDto> searchingItemList = new ArrayList<>();
        float maxRel = SearchingUtils.getMaxRelevance(uniquePageList, lemmaSortedList, repositoryUtils.getSearchIndexRepository());

        uniquePageList.forEach(page -> {
            List<Lemma> lemmaList = new ArrayList<>();
            SearchingDto searchingItem = new SearchingDto();
            AtomicReference<Float> absRelevance = new AtomicReference<>(0.0f);

            lemmaSortedList.forEach(lemma -> {
                Optional<SearchIndex> index = repositoryUtils.getSearchIndexRepository()
                        .findByLemmaId(page.getId(), lemma.getId());
                index.ifPresent(searchIndex -> {
                    absRelevance.set(absRelevance.get() + searchIndex.getLemmaRank());
                    lemmaList.add(lemma);
                });
            });

            DecimalFormat decimalFormat = new DecimalFormat("#.##");
            float relRelevance = Float.parseFloat(
                    decimalFormat.format(absRelevance.get() / maxRel).replaceAll(",", ".")
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
                searchingItemList.add(searchingItem);
            }
        });
        return searchingItemList;
    }

    private static float getMaxRelevance(List<Page> uniquePageList, List<Lemma> lemmaSortedList,
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

    private static String makeSnippet(Page page, String query) throws IOException {
        String[] queryStrArr = query.trim().split(" ");
        StringBuilder stringOriginalTextBuilder = new StringBuilder();
        StringBuilder stringTextWithoutEndingBuilder = new StringBuilder();

        Arrays.stream(queryStrArr).forEach(word -> stringOriginalTextBuilder.append(word).append(" "));
        Arrays.stream(queryStrArr).forEach(word -> stringTextWithoutEndingBuilder
                .append(getRussianWordRoot(word))
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

    private static String getRussianWordRoot(String word) {
        if (word.length() == 3) return word;
        if (StringUtils.endsWith(word, "ести")) return StringUtils.remove(word, "сти");

        String endingsRegex = "(ий|ей|ой|ая|ие|ый|ые|ть|ти|их|о|а)$";
        String suffixRegex = "(ова|ева|ец|иц)$";
        String suffixWithEnding = "(иться|аться|уться)$";
        return word.replaceAll(endingsRegex, "")
                .replaceAll(suffixRegex, "")
                .replaceAll(suffixWithEnding, "");
    }
}
