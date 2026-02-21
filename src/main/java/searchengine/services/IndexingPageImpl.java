package searchengine.services;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.tuple.Pair;
import org.springframework.stereotype.Service;
import searchengine.config.AppConfigProperties;
import searchengine.config.Website;
import searchengine.config.SitesList;
import searchengine.dto.ResponseStatusException;
import searchengine.dto.Response;
import searchengine.mapping.SiteCrawler;
import searchengine.model.Index;
import searchengine.model.Lemma;
import searchengine.model.Page;
import searchengine.model.Site;
import searchengine.repositories.PageRepository;
import searchengine.repositories.SiteRepository;
import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;
import java.util.*;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.stream.Collectors;
import static searchengine.model.Status.INDEXED;

@Slf4j
@Service
@RequiredArgsConstructor
public class IndexingPageImpl implements IndexingPage {
  private final LemmaExtraction lemma;
    private final AppConfigProperties connectionSetting;
    private final PageRepository pageRepository;
    private final SitesList sitesList;
    private final SiteRepository siteRepository;
    private final SinglePageInsert singlePageInsert;
    private final AtomicBoolean indexingInProgress = new AtomicBoolean(false);
    public Response indexPage(String url) {
        String urlToPage = URLDecoder.decode(url.substring(url.indexOf("h")), StandardCharsets.UTF_8);

        Optional<Website> siteConfigOptional = checkPageToSiteConfig(urlToPage);
        if (siteConfigOptional.isEmpty()) {
            log.info("сайт {} находится за пределами конфигурационного файла: ", url);
            return new Response(false, "Данная страница находится за пределами сайтов, указанных в конфигурационном файле");
        }

        Website sitesConfig = siteConfigOptional.get();
        log.info("страница найдена в конфигурационном файле: {}", sitesConfig.getName());

        if (checkIndexingPage(urlToPage, sitesConfig)) {
            log.info("Cтраница {} есть в базе данных", urlToPage);
            String pageToDelete = urlToPage.substring(sitesConfig.getUrl().length());
            pageRepository.deletePageByPath(pageToDelete);
            log.info("Все данные которые были связаны со страницей: {} удалены", urlToPage);
        }

        Site site = siteRepository.findByUrl(sitesConfig.getUrl());
        if (site == null) {
            site = IndexingServiceImpl.createSite(sitesConfig);
            log.info("Сайт отсутствует в БД, создался новый сайт: {}", site.getName());
        }

        try {
            Page pages = new SiteCrawler(sitesConfig.getUrl(), urlToPage, connectionSetting,indexingInProgress).computePage();
            log.info("Страница проиндексирована: {}", urlToPage);
            pages.setSite(site);

        Pair<List<Lemma>, List<Index>> lemmaAndIndex = findLemmaForSinglePage(pages, site);

           site.setLemmaList(lemmaAndIndex.getLeft());
           log.info("Найдено лемм: {}", lemmaAndIndex.getLeft().size());
            site.setStatus(INDEXED);
            site.setLastError("");

            singlePageInsert.singlePageInsert(site, pages, lemmaAndIndex);

        } catch (ResponseStatusException e) {
            log.error(e.getMessage());
            site.setLastError(e.getMessage());
            siteRepository.save(site);
        }
        return new Response(true);
    }
    private Optional<Website> checkPageToSiteConfig(String url) {
        return sitesList.getSites().stream()
                .filter(sitesConfig -> url.startsWith(sitesConfig.getUrl()))
                .findFirst();
    }
    private boolean checkIndexingPage(String url, Website sitesConfig) {
        String path = url.substring(sitesConfig.getUrl().length());
        return pageRepository.existsByPath(path);
    }
    public Pair<List<Lemma>, List<Index>> findLemmaToText(Site site, List<Page> pages) {
        List<Pair<List<Lemma>, List<Index>>> results = pages.parallelStream()
                .map(page -> findLemmaForSinglePage(page, site)).toList();

        List<Lemma> oldLemmas = results.stream()
                .flatMap(pair -> pair.getLeft().stream())
                .collect(Collectors.toList());

        List<Lemma> combinedLemmas = results.stream()
                .flatMap(pair -> pair.getLeft().stream())
                .distinct()
                .collect(Collectors.toList());

        Map<Lemma, Lemma> lemmaMapping = new HashMap<>();
        oldLemmas.forEach(oldLemma -> {
            Lemma uniqueLemma = combinedLemmas.stream()
                    .filter(l -> l.equals(oldLemma))
                    .findFirst()
                    .orElse(null);
            if (uniqueLemma != null) {
                lemmaMapping.put(oldLemma, uniqueLemma);
            }
        });

        List<Index> combinedIndexes = results.stream()
                .flatMap(pair -> pair.getRight().stream())
                .collect(Collectors.toList());

        combinedIndexes.forEach(index -> {
            Lemma oldLemma = index.getLemma();
            Lemma newLemma = lemmaMapping.get(oldLemma);
            if (newLemma != null) {
                index.setLemma(newLemma);
            }
        });

        Map<String, Long> lemmaFrequencyMap = combinedIndexes.stream()
                .collect(Collectors.groupingBy(index -> index.getLemma().getLemma(),
                        Collectors.counting()));

        for (Lemma lemma : combinedLemmas) {
            long count = lemmaFrequencyMap.getOrDefault(lemma.getLemma(), 0L);
            lemma.setFrequency((int) count);
        }

        return Pair.of(combinedLemmas, combinedIndexes);
    }

    private Pair<List<Lemma>, List<Index>> findLemmaForSinglePage(Page page, Site site) {
        Map<String, Lemma> lemmasMap = new HashMap<>();
        List<Index> indexes = new ArrayList<>();
        Map<String, Integer> extractedLemmas = lemma.searchLemma(page.getContent());

        for (Map.Entry<String, Integer> entry : extractedLemmas.entrySet()) {
            String lemmaText = entry.getKey();
            int frequency = entry.getValue();

            Lemma newLemma = new Lemma();
            newLemma.setSite(site);
            newLemma.setLemma(lemmaText);
            newLemma.setFrequency(1);
            lemmasMap.put(lemmaText, newLemma);

            Index index = new Index();
            index.setPage(page);
            index.setLemma(newLemma);
            index.setRank((float) frequency);
            indexes.add(index);
        }
        return Pair.of(new ArrayList<>(lemmasMap.values()), indexes);
    }

}
