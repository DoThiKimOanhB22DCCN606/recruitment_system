package com.recruitment.job.repository.search;

import com.recruitment.job.document.JobDocument;
import com.recruitment.job.dto.request.JobSearchRequest;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Sort;
import org.springframework.data.elasticsearch.core.ElasticsearchOperations;
import org.springframework.data.elasticsearch.core.SearchHits;
import org.springframework.data.elasticsearch.core.query.CriteriaQuery;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class JobSearchRepositoryImplTest {

    @Mock
    private ElasticsearchOperations elasticsearchOperations;

    @InjectMocks
    private JobSearchRepositoryImpl jobSearchRepository;

    @Test
    @SuppressWarnings("unchecked")
    void searchWithKeywordSortsByScoreRelevance() {
        JobSearchRequest request = new JobSearchRequest();
        request.setKeyword("Developer");

        SearchHits<JobDocument> hits = mock(SearchHits.class);
        when(hits.getSearchHits()).thenReturn(List.of());
        when(hits.getTotalHits()).thenReturn(0L);
        when(elasticsearchOperations.search(any(CriteriaQuery.class), eq(JobDocument.class))).thenReturn(hits);

        Page<JobDocument> result = jobSearchRepository.search(request);

        ArgumentCaptor<CriteriaQuery> captor = ArgumentCaptor.forClass(CriteriaQuery.class);
        verify(elasticsearchOperations).search(captor.capture(), eq(JobDocument.class));

        CriteriaQuery capturedQuery = captor.getValue();
        Sort sort = capturedQuery.getPageable().getSort();
        assertThat(sort.getOrderFor("_score")).isNotNull();
        assertThat(sort.getOrderFor("_score").getDirection()).isEqualTo(Sort.Direction.DESC);
    }

    @Test
    @SuppressWarnings("unchecked")
    void searchWithoutKeywordSortsByPublishedAtDesc() {
        JobSearchRequest request = new JobSearchRequest();

        SearchHits<JobDocument> hits = mock(SearchHits.class);
        when(hits.getSearchHits()).thenReturn(List.of());
        when(hits.getTotalHits()).thenReturn(0L);
        when(elasticsearchOperations.search(any(CriteriaQuery.class), eq(JobDocument.class))).thenReturn(hits);

        Page<JobDocument> result = jobSearchRepository.search(request);

        ArgumentCaptor<CriteriaQuery> captor = ArgumentCaptor.forClass(CriteriaQuery.class);
        verify(elasticsearchOperations).search(captor.capture(), eq(JobDocument.class));

        CriteriaQuery capturedQuery = captor.getValue();
        Sort sort = capturedQuery.getPageable().getSort();
        assertThat(sort.getOrderFor("_score")).isNull();
        assertThat(sort.getOrderFor("publishedAt")).isNotNull();
        assertThat(sort.getOrderFor("publishedAt").getDirection()).isEqualTo(Sort.Direction.DESC);
    }
}
