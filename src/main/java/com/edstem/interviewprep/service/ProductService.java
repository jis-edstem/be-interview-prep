package com.edstem.interviewprep.service;

import com.edstem.interviewprep.dto.PageResponse;
import com.edstem.interviewprep.dto.ProductFilter;
import com.edstem.interviewprep.dto.ProductRequest;
import com.edstem.interviewprep.dto.ProductResponse;
import com.edstem.interviewprep.entity.Product;
import com.edstem.interviewprep.exception.ProductNotFoundException;
import com.edstem.interviewprep.repository.ProductRepository;
import com.edstem.interviewprep.repository.ProductSpecifications;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class ProductService {

    private final ProductRepository repository;

    public PageResponse<ProductResponse> list(ProductFilter filter, Pageable pageable) {
        return PageResponse.from(repository
                .findAll(ProductSpecifications.matching(filter), pageable)
                .map(ProductResponse::from));
    }

    public ProductResponse get(Long id) {
        return ProductResponse.from(find(id));
    }

    @Transactional
    public ProductResponse update(Long id, ProductRequest request) {
        Product product = find(id);
        product.update(request.name(), request.category(), request.price(), request.stock(), request.rating());
        return ProductResponse.from(product);
    }

    @Transactional
    public void delete(Long id) {
        repository.delete(find(id));
    }

    private Product find(Long id) {
        return repository.findById(id).orElseThrow(() -> new ProductNotFoundException(id));
    }
}
