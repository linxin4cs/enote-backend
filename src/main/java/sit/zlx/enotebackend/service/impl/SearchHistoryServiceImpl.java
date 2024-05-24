package sit.zlx.enotebackend.service.impl;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import sit.zlx.enotebackend.domain.SearchHistoryDoc;
import sit.zlx.enotebackend.repository.SearchHistoryDocRepository;
import sit.zlx.enotebackend.service.SearchHistoryService;

import java.time.Instant;
import java.util.Comparator;
import java.util.Date;
import java.util.List;
import java.util.Optional;

@Service
public class SearchHistoryServiceImpl implements SearchHistoryService {
    private final SearchHistoryDocRepository searchHistoryDocRepository;

    @Autowired
    SearchHistoryServiceImpl(SearchHistoryDocRepository searchHistoryDocRepository) {
        this.searchHistoryDocRepository = searchHistoryDocRepository;
    }


    @Override
    public void saveSearchHistory(String userId, String keyword) {
        Optional<SearchHistoryDoc> searchHistoryDoc = searchHistoryDocRepository.findById(userId);
        searchHistoryDoc.ifPresentOrElse(
                searchHistoryDoc1 -> updateKeywords(searchHistoryDoc1, keyword),
                () -> {
                    SearchHistoryDoc newSearchHistoryDoc = new SearchHistoryDoc();
                    newSearchHistoryDoc.setId(userId);
                    newSearchHistoryDoc.setKeywords(List.of(new SearchHistoryDoc.Keyword(keyword, new Date().toInstant())));
                    searchHistoryDocRepository.save(newSearchHistoryDoc);
                }
        );
    }

    private void updateKeywords(SearchHistoryDoc searchHistoryDoc, String keyword) {
        List<SearchHistoryDoc.Keyword> keywords = searchHistoryDoc.getKeywords();
        SearchHistoryDoc.Keyword keywordObj = new SearchHistoryDoc.Keyword(keyword, new Date().toInstant());

        if (searchHistoryDoc.getKeywords() == null) {
            searchHistoryDoc.setKeywords(List.of(keywordObj));
        } else {
            for (SearchHistoryDoc.Keyword _keywordObj : keywords) {
                if (_keywordObj.getContent().equals(keyword)) {
                    Instant updatedAtInstant = new Date().toInstant();
                    _keywordObj.setUpdatedAtInstant(updatedAtInstant);
                    searchHistoryDoc.setKeywords(keywords);
                    searchHistoryDocRepository.save(searchHistoryDoc);
                    return;
                }
            }

            if (keywords.size() > 9) {
                keywords.sort(Comparator.comparing(SearchHistoryDoc.Keyword::getUpdatedAtInstant));
                keywords.remove(0);
            }

            // 更新关键词
            keywords.add(keywordObj);
            searchHistoryDoc.setKeywords(keywords);
        }

        searchHistoryDocRepository.save(searchHistoryDoc);
    }
}
