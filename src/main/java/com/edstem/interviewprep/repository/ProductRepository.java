package com.edstem.interviewprep.repository;

import com.edstem.interviewprep.entity.Product;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;

public interface ProductRepository extends JpaRepository<Product, Long>, JpaSpecificationExecutor<Product> {

    @Modifying
    @Query("update versioned Product p set p.stock = p.stock - :quantity where p.id = :id and p.stock >= :quantity")
    int decrementStock(Long id, int quantity);

    @Modifying
    @Query("update versioned Product p set p.stock = p.stock + :quantity where p.id = :id")
    void incrementStock(Long id, int quantity);
}
