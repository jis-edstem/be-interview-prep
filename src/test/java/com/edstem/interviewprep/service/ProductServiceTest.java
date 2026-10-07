package com.edstem.interviewprep.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.then;
import static org.mockito.Mockito.mock;

import com.edstem.interviewprep.dto.ProductFilter;
import com.edstem.interviewprep.entity.Product;
import com.edstem.interviewprep.repository.ProductRepository;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;

class ProductServiceTest {

    private static final ProductFilter NO_FILTER = new ProductFilter(null, null, null, null, null);

    private final ProductRepository repository = mock(ProductRepository.class);
    private final ProductService service = new ProductService(repository);

    @Test
    void sortOnNonUniqueFieldGetsIdTiebreaker() {
        Pageable requested = PageRequest.of(1, 10, Sort.by(Sort.Order.desc("category")));

        assertThat(sortSentToRepository(requested)).containsExactly(Sort.Order.desc("category"), Sort.Order.asc("id"));
    }

    @Test
    void sortAlreadyOnIdIsLeftAsRequested() {
        Pageable requested = PageRequest.of(0, 10, Sort.by(Sort.Order.desc("id")));

        assertThat(sortSentToRepository(requested)).containsExactly(Sort.Order.desc("id"));
    }

    @SuppressWarnings("unchecked")
    private Sort sortSentToRepository(Pageable requested) {
        given(repository.findAll(any(Specification.class), any(Pageable.class))).willReturn(Page.<Product>empty());
        ArgumentCaptor<Pageable> sent = ArgumentCaptor.forClass(Pageable.class);

        service.list(NO_FILTER, requested);

        then(repository).should().findAll(any(Specification.class), sent.capture());
        assertThat(sent.getValue().getPageNumber()).isEqualTo(requested.getPageNumber());
        assertThat(sent.getValue().getPageSize()).isEqualTo(requested.getPageSize());
        return sent.getValue().getSort();
    }
}
