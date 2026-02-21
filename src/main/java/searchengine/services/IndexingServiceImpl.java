package searchengine.services;

import org.apache.commons.lang3.tuple.Pair;
import org.hibernate.Hibernate;
import org.springframework.jdbc.core.BatchPreparedStatementSetter;
import searchengine.config.Website;
import searchengine.config.SitesList;
import searchengine.dto.IndexingResponse;
import searchengine.dto.ResponseStatusException;
import searchengine.mapping.SiteCrawler;
import searchengine.model.*;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import searchengine.repositories.LemmaRepository;
import searchengine.repositories.PageRepository;
import searchengine.repositories.SiteRepository;

import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.time.LocalDateTime;
import java.util.List;
import java.util.concurrent.ForkJoinPool;
import java.util.concurrent.atomic.AtomicBoolean;

import static searchengine.model.Status.*;

import searchengine.config.AppConfigProperties;
import org.springframework.jdbc.core.JdbcTemplate;

@Slf4j
@Service
@RequiredArgsConstructor
public class IndexingServiceImpl implements IndexingService {
    private final AtomicBoolean indexingInProgress = new AtomicBoolean(false);
    private ForkJoinPool forkJoinPool;
    private final SiteRepository siteRepository;
    private final PageRepository pageRepository;
    private final LemmaRepository lemmaRepository;
    private final SitesList sitesList;
    private final AppConfigProperties connectionSetting;
    private final IndexingPageImpl indexingPageImpl;
    private final JdbcTemplate jdbcTemplate;

    public IndexingResponse startIndexing() {
        if (indexingInProgress.compareAndSet(false, true)) {
            log.info("Запуск индексации");
            forkJoinPool = new ForkJoinPool(Runtime.getRuntime().availableProcessors(),
                    ForkJoinPool.defaultForkJoinWorkerThreadFactory, null, true);
            for (Website sitesConfig : sitesList.getSites()) {
                forkJoinPool.submit(() -> indexSite(sitesConfig));
            }
            return new IndexingResponse(true);
        } else return new IndexingResponse(false, "Индексация уже запущена");
    }

    public IndexingResponse stopIndexing() {
        if (!forkJoinPool.isShutdown()) {
            forkJoinPool.shutdown();
            indexingInProgress.compareAndSet(true, false);
            log.info("Индексация была остановлена пользователем");
            return new IndexingResponse(true);
        } else {
            return new IndexingResponse(false, "Индексация не запущена");
        }
    }

    public void indexSite(Website sitesUrl) {
        Site site;
        if (siteRepository.existsByUrl(sitesUrl.getUrl())) {
            try {
                log.info("Этот сайт уже обрабатывался: {}", sitesUrl.getUrl());
                site = siteRepository.findByUrl(sitesUrl.getUrl());
                siteRepository.delete(site);
                log.info("Удаляем сайт из БД {}", sitesUrl.getUrl());
            } catch (ResponseStatusException e) {
                log.error("Ошибка удаления {}", e.getMessage());
                throw e;
            }
        }
        site = createSite(sitesUrl);
        siteRepository.save(site);

        try {
            log.info("Началась индексация сайта: {}", sitesUrl.getUrl());
            List<Page> pages = new SiteCrawler(sitesUrl.getUrl(), sitesUrl.getUrl(),
                    connectionSetting, indexingInProgress).compute();
            addSiteToPage(site, pages);
            Pair<List<Lemma>, List<Index>> lemmaAndIndex = indexingPageImpl.findLemmaToText(site, pages);
            site.setStatus(forkJoinPool.isShutdown() ? FAILED : INDEXED);
            site.setStatusTime(LocalDateTime.now());
            site.setLastError(forkJoinPool.isShutdown() ? "Индексация остановлена пользователем" : "");
            allInsert(site, pages, lemmaAndIndex);

        } catch (ResponseStatusException e) {
            log.error("Ошибка при индексации сайта: {}", sitesUrl.getUrl() + " - " + e.getMessage());
            site.setStatus(FAILED);
            site.setStatusTime(LocalDateTime.now());
            site.setLastError(e.getMessage());
            siteRepository.save(site);
        }
        log.info("Сайт проиндексирован: {}", sitesUrl.getUrl());
    }

    public static Site createSite(Website sitesUrl) {
        Site site = new Site();
        site.setUrl(sitesUrl.getUrl());
        site.setName(sitesUrl.getName());
        site.setStatusTime(LocalDateTime.now());
        site.setLastError("");
        site.setStatus(INDEXING);
        return site;
    }

    private List<Page> addSiteToPage(Site site, List<Page> pages) {
        pages.forEach(p -> p.setSite(site));
        return pages;
    }

    private void allInsert(Site site, List<Page> pages, Pair<List<Lemma>, List<Index>> lemmaAndIndex) {
        forkJoinPool.execute(() -> {
            String string = forkJoinPool.isShutdown() ? String.format("Сохранение сайта %s с остановленной индексацией", site.getName()) : String.format("Сохранение проиндексированного сайта %s", site.getName());
            log.info(string);
            siteRepository.save(site);
            pageRepository.saveAll(pages);
            lemmaRepository.saveAll(lemmaAndIndex.getLeft());
            batchIndexInsert(lemmaAndIndex.getRight());
        });

    }

    private void batchIndexInsert(List<Index> indexList) {
        String sql = "INSERT INTO indexes (page_id,lemma_id,`runk`) VALUES (?,?,?)";
        jdbcTemplate.batchUpdate(sql, new BatchPreparedStatementSetter() {
            @Override
            public void setValues(PreparedStatement ps, int i) throws SQLException {
                Index index = indexList.get(i);
                Hibernate.initialize(index.getPage());
                Hibernate.initialize(index.getLemma());
                ps.setObject(1, index.getPage().getId());
                ps.setObject(2, index.getLemma().getId());
                ps.setFloat(3, index.getRank());
            }

            @Override
            public int getBatchSize() {  //  возвращает размер списка индексов, который соответствует количеству записей, обрабатываемых одним пакетом
                return indexList.size();
            }
        });

    }
}





