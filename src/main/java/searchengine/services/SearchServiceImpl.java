package searchengine.services;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import searchengine.dto.PageRelevance;
import searchengine.dto.ResponseStatusException;
import searchengine.dto.ResponseSearch;
import searchengine.dto.ResultSearch;
import searchengine.mapping.SnippetGenerator;
import searchengine.model.Index;
import searchengine.model.Lemma;
import searchengine.model.Page;
import searchengine.model.Site;
import searchengine.repositories.IndexRepository;
import searchengine.repositories.LemmaRepository;
import searchengine.repositories.PageRepository;
import searchengine.repositories.SiteRepository;
import java.util.*;
import java.util.stream.Collectors;


@Service
@RequiredArgsConstructor
@Slf4j
public class SearchServiceImpl implements SearchService{
    private final LemmaExtraction lemmaExtraction;
    private final SiteRepository siteRepository;
    private final PageRepository pageRepository;
    private final LemmaRepository lemmaRepository;
    private final IndexRepository indexRepository;
    private static final Logger logger = LoggerFactory.getLogger(SearchServiceImpl.class);
    public ResponseSearch systemSearch(String query, String siteUrl, Integer offset, Integer limit) {
        if (query.isBlank()) {
            return new ResponseSearch(false, "Задан пустой поисковый запрос");
        }

        try {
            Site site = siteRepository.findByUrl(siteUrl);
            logger.info("Получен поисковый запрос следующего содержания: {}", query);
            Set<String> uniqueLemma = lemmaExtraction.getLemmaSet(query);
            List<Lemma> filterLemma = calculatingLemmasOnPages(uniqueLemma, site);

            if (filterLemma.isEmpty()) {
                logger.info("Список лемм пуст, по запросу лемм в БД не найдено");
                return new ResponseSearch(true, 0, List.of());
            }

            List<Page> pages = indexRepository.findPagesByLemma(filterLemma.get(0).getLemma());

            for (Lemma lemma : filterLemma) {
                List<Page> pageWithLemma = indexRepository.findPagesByLemma(lemma.getLemma());
                pages = pages.stream()
                        .filter(pageWithLemma::contains)
                        .toList();
            }

            List<PageRelevance> resultRelevance = calculatedRelevance(filterLemma);
            resultRelevance.sort(Comparator.comparing(PageRelevance::absoluteRelevance).reversed());

            List<ResultSearch> resultSearchList = createdRequest(resultRelevance, query);

            int totalResultSearchCount = resultSearchList.size();
            List<ResultSearch> paginationResult = resultSearchList.stream()
                    .skip(offset)
                    .limit(limit)
                    .toList();
            return new ResponseSearch(true, totalResultSearchCount, paginationResult);
        } catch (ResponseStatusException e) {
            log.error("Ошибка при поиске {} :", e.getMessage());
            throw new RuntimeException(e);
        }

    }

    private List<PageRelevance> calculatedRelevance(List<Lemma> filterLemma) {
        List<PageRelevance> resultRelevance = new ArrayList<>();
        Map<Page, Double> pageToRelevance = new HashMap<>();

        List<Integer> lemmaIds = filterLemma.stream()
                .mapToInt(Lemma::getId)
                .boxed()
                .toList();

        List<Index> indexList = indexRepository.findByLemmaIdIn(lemmaIds);
        for (Index index : indexList) {
            Page page = index.getPage();
            double rank = index.getRank();
            pageToRelevance.put(page, pageToRelevance.getOrDefault(page, 0.0) + rank);
        }

        double maxAbsoluteRelevance = pageToRelevance.values().stream()
                .mapToDouble(Double::doubleValue)
                .max()
                .orElse(0.1);

        for (Map.Entry<Page, Double> entry : pageToRelevance.entrySet()) {
            Page page = entry.getKey();
            double absoluteRelevance = entry.getValue();
            double relativeRelevance = absoluteRelevance / maxAbsoluteRelevance;
            resultRelevance.add(new PageRelevance(page, absoluteRelevance, relativeRelevance));
        }

        return resultRelevance;
    }

    private List<Lemma> calculatingLemmasOnPages(Set<String> lemmas, Site site) {
        long totalPages = pageRepository.count();
        double threshold = 0.7;
        Map<String, Lemma> bestLemmas = new HashMap<>();

        for (String lemma1 : lemmas) {
            List<Lemma> lemmaList = site == null ? lemmaRepository.findByLemma(lemma1): lemmaRepository.findByLemmaToSiteId(lemma1, site);

            for (Lemma currentLemma : lemmaList) {
                if (currentLemma != null) {
                    long countPageToLemma = currentLemma.getFrequency();
                    double lemmaTotalPages = countPageToLemma/totalPages;
                    if (lemmaTotalPages <= threshold) {
                        bestLemmas.merge(
                                currentLemma.getLemma(),
                                currentLemma,
                                (oldValue, newValue) -> oldValue.getFrequency() > newValue.getFrequency() ? oldValue : newValue
                        );
                    }
                }
            }
        }
        return bestLemmas.values().stream().
                sorted(Comparator.comparing(Lemma::getFrequency).reversed()).
                collect(Collectors.toList());
    }

    private List<ResultSearch> createdRequest(List<PageRelevance> pageRelevance, String query) {
        return pageRelevance.stream()
                .map(page -> {
                    String url = page.page().getSite().getUrl();
                    String nameUrl = page.page().getSite().getName();
                    String uri = page.page().getPath();

                    Document document = Jsoup.parse(page.page().getContent());
                    String title = document.title();

                    String snippet = SnippetGenerator.generatedSnippet(query, page.page().getContent());

                    Double relevance = page.relativeRelevance();

                    return new ResultSearch(url, nameUrl, uri, title, snippet, relevance);
                }).collect(Collectors.toList());
    }
}
