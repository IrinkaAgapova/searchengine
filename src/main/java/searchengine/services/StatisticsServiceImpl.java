package searchengine.services;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import searchengine.config.Website;
import searchengine.config.SitesList;
import searchengine.dto.statistics.DetailedStatisticsItem;
import searchengine.dto.statistics.StatisticsData;
import searchengine.dto.statistics.StatisticsResponse;
import searchengine.dto.statistics.TotalStatistics;
import searchengine.repositories.LemmaRepository;
import searchengine.repositories.PageRepository;
import searchengine.repositories.SiteRepository;
import searchengine.model.Site;

import java.time.temporal.ChronoField;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;

@Service
@RequiredArgsConstructor
public class StatisticsServiceImpl implements StatisticsService {
    private final PageRepository pageRepository;
    private final LemmaRepository lemmaRepository;
    private final SiteRepository siteRepository;
    private final SitesList sites;

    @Override
    public StatisticsResponse getStatistics() {

        TotalStatistics total = new TotalStatistics();
        total.setSites(sites.getSites().size());
        total.setIndexing(true);
        List<DetailedStatisticsItem> detailed = new ArrayList<>();
        List<Website> sitesList = sites.getSites();
        for (int i = 0; i < sitesList.size(); i++) {
            Website site = sitesList.get(i);
            DetailedStatisticsItem item = new DetailedStatisticsItem();
            item.setName(site.getName());
            item.setUrl(site.getUrl());
            Site sites = siteRepository.findByUrl(site.getUrl());
            int pages = sites == null ? 0 : pageRepository.countPagesToSite(sites.getId());
            int lemmas = sites == null ? 0 : lemmaRepository.countLemmaToSite(sites.getId());
            item.setPages(pages);
            item.setLemmas(lemmas);
            item.setStatus(sites == null ? "Сайт не проиндексирован" : sites.getStatus().toString());
            item.setError(sites == null ? "Сайт не проиндексирован" : sites.getLastError());
            item.setStatusTime(sites == null ? 0 : sites.getStatusTime().getLong(ChronoField.MILLI_OF_SECOND));
            total.setPages(total.getPages() + pages);
            total.setLemmas(total.getLemmas() + lemmas);
            detailed.add(item);

        }

        StatisticsResponse response = new StatisticsResponse();
        StatisticsData data = new StatisticsData();
        data.setTotal(total);
        data.setDetailed(detailed);
        response.setStatistics(data);
        response.setResult(true);
        return response;
    }
}
