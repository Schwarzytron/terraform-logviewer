package ru.konkurst1.ekb.terraform_logviewer.repository;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.elasticsearch.annotations.Query;
import org.springframework.data.elasticsearch.repository.ElasticsearchRepository;
import org.springframework.stereotype.Repository;
import ru.konkurst1.ekb.terraform_logviewer.dto.LogFileInfo;
import ru.konkurst1.ekb.terraform_logviewer.model.LogEntry;
import ru.konkurst1.ekb.terraform_logviewer.model.LogLevel;

import java.util.List;

@Repository
public interface LogEntryRepository extends ElasticsearchRepository<LogEntry, String>, LogEntryRepositoryCustom {

    Page<LogEntry> findByLogFileId(String logFileId, Pageable pageable);

    Page<LogEntry> findByLogFileIdAndMessageContainingIgnoreCase(String logFileId, String query, Pageable pageable);

    @Query("""
        {
          "size": 0,
          "aggs": {
            "log_files": {
              "terms": {
                "field": "logFileId.keyword",
                "size": 1000
              },
              "aggs": {
                "max_timestamp": {
                  "max": {
                    "field": "timestamp"
                  }
                },
                "total_entries": {
                  "value_count": {
                    "field": "id"
                  }
                },
                "error_count": {
                  "filter": {
                    "term": {
                      "parsingError": true
                    }
                  }
                }
              }
            }
          }
        }
        """)
    List<LogFileInfo> findLogFileInfo();

    Long countByLogFileIdAndIsRead(String logFileId, Boolean isRead);

    List<LogEntry> findByTfReqIdOrderByTimestamp(String tfReqId);

    List<LogEntry> findByTfResourceType(String resourceType);

    Long countByIsRead(Boolean isRead);

    @Query("""
        {
          "aggs": {
            "distinct_resource_types": {
              "terms": {
                "field": "tfResourceType.keyword",
                "size": 1000
              }
            }
          },
          "size": 0
        }
        """)
    List<String> findDistinctTfResourceTypes();

    @Query("""
        {
          "aggs": {
            "distinct_req_ids": {
              "terms": {
                "field": "tfReqId.keyword",
                "size": 1000
              }
            }
          },
          "size": 0
        }
        """)
    List<String> findDistinctTfReqIds();
}