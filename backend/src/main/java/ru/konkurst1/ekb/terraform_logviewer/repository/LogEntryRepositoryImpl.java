package ru.konkurst1.ekb.terraform_logviewer.repository;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.data.elasticsearch.core.ElasticsearchOperations;
import org.springframework.data.elasticsearch.core.SearchHit;
import org.springframework.data.elasticsearch.core.SearchHits;
import org.springframework.data.elasticsearch.core.query.Criteria;
import org.springframework.data.elasticsearch.core.query.CriteriaQuery;
import org.springframework.data.elasticsearch.core.query.Query;
import org.springframework.stereotype.Repository;
import ru.konkurst1.ekb.terraform_logviewer.model.LogEntry;
import ru.konkurst1.ekb.terraform_logviewer.model.LogLevel;

import java.util.List;
import java.util.stream.Collectors;

@Repository
public class LogEntryRepositoryImpl implements LogEntryRepositoryCustom {

    @Autowired
    private ElasticsearchOperations elasticsearchOperations;

    @Override
    public Page<LogEntry> findByLogFileIdAndFilters(String logFileId, LogLevel level, String section, Boolean hasErrors, Pageable pageable) {
        Criteria criteria = new Criteria("logFileId").is(logFileId);
        
        if (level != null) {
            criteria = criteria.and(new Criteria("level").is(level));
        }
        
        if (section != null) {
            criteria = criteria.and(new Criteria("section").is(section));
        }
        
        if (hasErrors != null) {
            criteria = criteria.and(new Criteria("parsingError").is(hasErrors));
        }

        Query query = new CriteriaQuery(criteria).setPageable(pageable);
        SearchHits<LogEntry> searchHits = elasticsearchOperations.search(query, LogEntry.class);
        
        List<LogEntry> content = searchHits.getSearchHits().stream()
                .map(SearchHit::getContent)
                .collect(Collectors.toList());
        
        return new PageImpl<>(content, pageable, searchHits.getTotalHits());
    }
}