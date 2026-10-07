package com.edstem.interviewprep.service;

import com.edstem.interviewprep.config.CacheConfig;
import com.edstem.interviewprep.dto.PageResponse;
import com.edstem.interviewprep.dto.ProductFilter;
import com.edstem.interviewprep.dto.ProductRequest;
import com.edstem.interviewprep.dto.ProductResponse;
import com.edstem.interviewprep.entity.Product;
import com.edstem.interviewprep.exception.InsufficientStockException;
import com.edstem.interviewprep.exception.ProductNotFoundException;
import com.edstem.interviewprep.exception.ProductVersionConflictException;
import com.edstem.interviewprep.repository.ProductRepository;
import com.edstem.interviewprep.repository.ProductSpecifications;
import lombok.RequiredArgsConstructor;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class ProductService {

    private static final String TIEBREAKER = "id";

    private final ProductRepository repository;

    public PageResponse<ProductResponse> list(ProductFilter filter, Pageable pageable) {
        return PageResponse.from(repository
                .findAll(ProductSpecifications.matching(filter), withTiebreaker(pageable))
                .map(ProductResponse::from));
    }

    @Cacheable(cacheNames = CacheConfig.PRODUCTS, key = "#id", sync = true)
    public ProductResponse get(Long id) {
        return ProductResponse.from(find(id));
    }

    @Transactional
    @CacheEvict(cacheNames = CacheConfig.PRODUCTS, key = "#id")
    public ProductResponse update(Long id, ProductRequest request) {
        Product product = find(id);
        if (!product.getVersion().equals(request.version())) {
            throw new ProductVersionConflictException(id, request.version(), product.getVersion());
        }
        product.update(request.name(), request.category(), request.price(), request.stock(), request.rating());
        repository.flush();
        return ProductResponse.from(product);
    }

    @Transactional
    @CacheEvict(cacheNames = CacheConfig.PRODUCTS, key = "#id")
    public void delete(Long id) {
        repository.delete(find(id));
    }

    @Transactional
    @CacheEvict(cacheNames = CacheConfig.PRODUCTS, key = "#id")
    public void reserveStock(Long id, int quantity) {
        if (repository.decrementStock(id, quantity) == 0) {
            throw repository.existsById(id)
                    ? new InsufficientStockException(id, quantity)
                    : new ProductNotFoundException(id);
        }
    }

    @Transactional
    @CacheEvict(cacheNames = CacheConfig.PRODUCTS, key = "#id")
    public void releaseStock(Long id, int quantity) {
        repository.incrementStock(id, quantity);
    }

    private static Pageable withTiebreaker(Pageable pageable) {
        if (pageable.getSort().getOrderFor(TIEBREAKER) != null) {
            return pageable;
        }
        return PageRequest.of(
                pageable.getPageNumber(),
                pageable.getPageSize(),
                pageable.getSort().and(Sort.by(TIEBREAKER)));
    }

    private Product find(Long id) {
        return repository.findById(id).orElseThrow(() -> new ProductNotFoundException(id));
    }
}
