package searchengine.services;

import lombok.RequiredArgsConstructor;
import org.apache.lucene.morphology.russian.RussianLuceneMorphology;
import org.jsoup.Jsoup;
import org.springframework.stereotype.Component;
import org.springframework.stereotype.Service;
import java.util.*;
import java.util.stream.Collectors;

@Service
@Component
@RequiredArgsConstructor
class LemmaExtraction {
    private static final String[] particles = new String[]{"МЕЖД", "ПРЕДЛ", "СОЮЗ", "ЧАСТ"};
    private final RussianLuceneMorphology luceneMorphology;
    private static final String PATTERN = "^[а-я]+$";

    public HashMap<String, Integer> searchLemma(String text) {
        Set<String> words = parsHtml(text);
        HashMap<String, Integer> lemmas = new HashMap<>();
        for (String word : words) {
            if (isWord(word)) {
                List<String> wordBaseForms = luceneMorphology.getMorphInfo(word);
                if (isWordBase(wordBaseForms)) {
                    continue;
                }
                List<String> normalForms = luceneMorphology.getNormalForms(word);
                if (!normalForms.isEmpty()) {
                    String normalWord = normalForms.get(0);
                    if (normalWord.length() >= 3) {
                        lemmas.merge(normalWord, 1, Integer::sum);
                    }
                }
            }
        }
        return lemmas;
    }

    private Set<String> parsHtml(String content) {
        String text = Jsoup.parse(content).text();
        String[] wordsArray = text.toLowerCase(Locale.ROOT)
                .replaceAll("([^а-яa-z\\s])", " ")
                .trim()
                .split("\\s+");

        return Arrays.stream(wordsArray).
                filter(w -> w.length() > 2 && !w.isBlank()).
                collect(Collectors.toCollection(HashSet::new));
    }

    private boolean isWord(String word) {
        return word.matches(PATTERN);
    }

    private boolean isWordBase(List<String> wordBaseForms) {
        return wordBaseForms.stream().anyMatch(this::isParticles);
    }

    private boolean isParticles(String wordBase) {
        for (String property : particles) {
            if (wordBase.toUpperCase().contains(property)) {
                return true;
            }
        }
        return false;
    }

    public Set<String> getLemmaSet(String text) {
        Set<String> textArray = parsHtml(text);
        Set<String> lemmaSet = new HashSet<>();
        for (String word : textArray) {
            if (word.length() >= 3) {
                if (isWord(word)) {
                    List<String> wordBaseForms = luceneMorphology.getMorphInfo(word);
                    if (isWordBase(wordBaseForms)) {
                        continue;
                    }
                    lemmaSet.addAll(luceneMorphology.getNormalForms(word));
                }
            }
        }
        return lemmaSet;
    }

}