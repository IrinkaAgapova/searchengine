package searchengine.services;

import lombok.RequiredArgsConstructor;
import org.apache.commons.lang3.tuple.Pair;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Isolation;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import searchengine.dto.ResponseStatusException;
import searchengine.model.Index;
import searchengine.model.Lemma;
import searchengine.model.Page;
import searchengine.model.Site;
import searchengine.repositories.IndexRepository;
import searchengine.repositories.LemmaRepository;
import searchengine.repositories.PageRepository;
import searchengine.repositories.SiteRepository;


import java.util.List;
@Service
@RequiredArgsConstructor
@Transactional(propagation = Propagation.REQUIRED, isolation = Isolation.SERIALIZABLE)
public class SinglePageInsert {

    private static final Logger logger = LoggerFactory.getLogger(IndexingService.class);
    private final SiteRepository siteRepository;
    private final PageRepository pageRepository;
    private final IndexRepository indexRepository;
    private final LemmaRepository lemmaRepository;

    public void singlePageInsert(Site site, Page page, Pair<List<Lemma>, List<Index>> lemmaAndIndex) {
        try {
            siteRepository.save(site);
            pageRepository.save(page);
            lemmaRepository.saveAll(lemmaAndIndex.getLeft());
            indexRepository.saveAll(lemmaAndIndex.getRight());
            logger.info("Сохранение страницы и её метаданных завершилось.");
        } catch (ResponseStatusException e) {
            logger.error("Ошибка при сохранении страницы: {}", e.getMessage());
        }
    }
}

