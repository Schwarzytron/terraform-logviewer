package ru.konkurst1.ekb.terraform_logviewer.service;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.elasticsearch.core.ElasticsearchOperations;
import org.springframework.data.elasticsearch.core.SearchHit;
import org.springframework.data.elasticsearch.core.SearchHits;
import org.springframework.data.elasticsearch.core.query.Criteria;
import org.springframework.data.elasticsearch.core.query.CriteriaQuery;
import org.springframework.data.elasticsearch.core.query.Query;
import org.springframework.data.elasticsearch.core.query.UpdateQuery;
import org.springframework.stereotype.Service;
import ru.konkurst1.ekb.terraform_logviewer.dto.SearchFilters;
import ru.konkurst1.ekb.terraform_logviewer.model.LogEntry;
import ru.konkurst1.ekb.terraform_logviewer.repository.LogEntryRepository;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
public class LogSearchService {

    @Autowired
    private LogEntryRepository logEntryRepository;

    @Autowired
    private ElasticsearchOperations elasticsearchOperations;

    public Page<LogEntry> advancedSearch(SearchFilters filters, String logFileId) {
        Pageable pageable = PageRequest.of(
                filters.getPage(),
                filters.getSize(),
                Sort.by("timestamp").ascending()
        );

        Criteria criteria = new Criteria();

        if (logFileId != null && !logFileId.isEmpty()) {
            criteria = criteria.and(new Criteria("logFileId").is(logFileId));
        }

        // Free text search
        if (filters.getFreeText() != null && !filters.getFreeText().trim().isEmpty()) {
            String searchTerm = filters.getFreeText().toLowerCase();
            Criteria messageCriteria = new Criteria("message").contains(searchTerm)
                    .or(new Criteria("rawMessage").contains(searchTerm));
            criteria = criteria.and(messageCriteria);
        }

        // Resource type filter
        if (filters.getTfResourceType() != null && !filters.getTfResourceType().isEmpty()) {
            criteria = criteria.and(new Criteria("tfResourceType").is(filters.getTfResourceType()));
        }

        // Level filter
        if (filters.getLevel() != null) {
            criteria = criteria.and(new Criteria("level").is(filters.getLevel()));
        }

        // Section filter
        if (filters.getSection() != null && !filters.getSection().isEmpty()) {
            criteria = criteria.and(new Criteria("section").is(filters.getSection()));
        }

        // Request ID filter
        if (filters.getTfReqId() != null && !filters.getTfReqId().isEmpty()) {
            criteria = criteria.and(new Criteria("tfReqId").is(filters.getTfReqId()));
        }

        // Timestamp range
        if (filters.getTimestampFrom() != null) {
            criteria = criteria.and(new Criteria("timestamp").greaterThanEqual(filters.getTimestampFrom()));
        }
        if (filters.getTimestampTo() != null) {
            criteria = criteria.and(new Criteria("timestamp").lessThanEqual(filters.getTimestampTo()));
        }

        // Unread filter
        if (Boolean.TRUE.equals(filters.getOnlyUnread())) {
            criteria = criteria.and(new Criteria("isRead").is(false));
        }

        Query query = new CriteriaQuery(criteria).setPageable(pageable);
        SearchHits<LogEntry> searchHits = elasticsearchOperations.search(query, LogEntry.class);

        List<LogEntry> content = searchHits.getSearchHits().stream()
                .map(SearchHit::getContent)
                .collect(Collectors.toList());

        return new org.springframework.data.domain.PageImpl<>(
                content, pageable, searchHits.getTotalHits()
        );
    }

    public Map<String, List<LogEntry>> groupByRequestId(List<LogEntry> entries) {
        return entries.stream()
                .filter(entry -> entry.getTfReqId() != null)
                .collect(Collectors.groupingBy(LogEntry::getTfReqId));
    }

    public List<LogEntry> getRequestChain(String tfReqId) {
        return logEntryRepository.findByTfReqIdOrderByTimestamp(tfReqId);
    }

    public void markAsRead(List<String> entryIds) {
        List<UpdateQuery> updateQueries = entryIds.stream()
                .map(id -> UpdateQuery.builder(id)
                        .withScript("ctx._source.isRead = true")
                        .build())
                .collect(Collectors.toList());

        elasticsearchOperations.bulkUpdate(updateQueries, LogEntry.class);
    }


    public Map<String, Long> getUnreadStats() {
        Long totalUnread = logEntryRepository.countByIsRead(false);
        return Map.of("total", totalUnread != null ? totalUnread : 0L);
    }
}