package com.edstem.interviewprep.repository;

import com.edstem.interviewprep.entity.Product;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ProductRepository extends JpaRepository<Product, Long> {}
