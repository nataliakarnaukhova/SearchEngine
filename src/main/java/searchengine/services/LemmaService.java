package searchengine.services;

import org.apache.log4j.Logger;
import org.apache.lucene.morphology.LuceneMorphology;
import org.apache.lucene.morphology.russian.RussianLuceneMorphology;
import org.jsoup.Jsoup;
import searchengine.models.Page;

import java.io.IOException;
import java.util.*;
import java.util.function.Function;
import java.util.stream.Collectors;

public class LemmaService {
    private final static Logger log = Logger.getLogger(IndexingService.class);

    public static Map<String, Integer> getLemmas(Page page) {
        log.info("Лемматизация страницы: " + page.getSite().getUrl().concat(page.getPath()));

        Map<String, Integer> lemmaMap = new HashMap<>();
        String pageText;
        try {
            LemmaFinder lemmaFinder = new LemmaFinder();
            pageText = Jsoup.parse(page.getContext()).body().text();
            lemmaMap.putAll(lemmaFinder.getSequentialWordNumber(pageText));
        } catch (IOException ex) {
            ex.printStackTrace();
            log.error("Ошибка при лемматизации страницы " + page.getSite().getUrl().concat(page.getPath()) + ": "
                    + ex.getMessage());
        }

        return lemmaMap;
    }

    public static Set<String> getLemmasFromQuery(String query) throws IOException {
        LemmaFinder lemmaFinder = new LemmaFinder();
        return lemmaFinder.getSequentialWordNumberFromQuery(query);
    }

    private static class LemmaFinder {
        LuceneMorphology russianLuceneMorphology;

        public synchronized Map<String, Integer> getSequentialWordNumber(String text) throws IOException {
            russianLuceneMorphology = new RussianLuceneMorphology();

            List<String> wordList = Arrays
                    .stream(text.replaceAll("[^а-яА-ЯёЁ\\s]", " ")
                            .replaceAll("\\s{2,}", " ")
                            .trim()
                            .toLowerCase()
                            .split(" "))
                    .filter(word -> isCorrectWord(russianLuceneMorphology.getMorphInfo(word).toString()))
                    .map(russianLuceneMorphology::getNormalForms)
                    .map(list -> list.get(0))
                    .map(word -> word.replaceAll("ё", "е"))
                    .filter(word -> word.length() > 2)
                    .toList();

            Map<String, Long> wordMap = wordList.stream()
                    .collect(Collectors.groupingBy(Function.identity(), Collectors.counting()));

            return wordMap.entrySet()
                            .stream()
                            .collect(Collectors.toMap(
                                    Map.Entry::getKey,
                                    entry -> Integer.parseInt(String.valueOf(entry.getValue()))));
        }

        private Set<String> getSequentialWordNumberFromQuery(String query) throws IOException {
            russianLuceneMorphology = new RussianLuceneMorphology();
            List<String> wordList = Arrays.stream(
                            query.replaceAll("[^а-яА-ЯёЁ\\s]", " ")
                                    .replaceAll("\\s{2,}", " ")
                                    .trim()
                                    .toLowerCase()
                                    .split(" "))
                    .filter(word -> isCorrectWord(russianLuceneMorphology.getMorphInfo(word).toString())).toList();

            Set<String> lemmaQuerySet = new HashSet<>();
            wordList.forEach(word -> {
                List<String> queryLemmaList = russianLuceneMorphology.getNormalForms(word);
                lemmaQuerySet.addAll(queryLemmaList);
            });

            return new HashSet<>(lemmaQuerySet);
        }


        private synchronized boolean isCorrectWord(String word) {
            return !word.contains("СОЮЗ") && !word.contains("ПРЕДЛ") && !word.contains("МЕЖД");
        }
    }
}
