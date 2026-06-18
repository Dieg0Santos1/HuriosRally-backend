package com.hurios.huriosbackend.repository;

import com.hurios.huriosbackend.entity.Sale;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface SaleRepository extends JpaRepository<Sale, Long> {
    @EntityGraph(attributePaths = {"user", "items", "items.product"})
    List<Sale> findByUserId(Long userId);

    @EntityGraph(attributePaths = {"user", "items", "items.product"})
    List<Sale> findAll();

    @EntityGraph(attributePaths = {"user", "items", "items.product"})
    Optional<Sale> findById(Long id);

    List<Sale> findByStatus(String status);
}
