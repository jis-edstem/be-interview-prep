package com.edstem.interviewprep.service;

import com.edstem.interviewprep.dto.PageResponse;
import com.edstem.interviewprep.dto.ProductResponse;
import com.edstem.interviewprep.repository.ProductRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class ProductService {

    private final ProductRepository repository;

    public PageResponse<ProductResponse> list(Pageable pageable) {
        return PageResponse.from(repository.findAll(pageable).map(ProductResponse::from));
    }
}
