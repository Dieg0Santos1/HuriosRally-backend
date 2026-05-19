package com.hurios.huriosbackend.repository;

import com.hurios.huriosbackend.entity.Sale;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface SaleRepository extends JpaRepository<Sale, Long> {
    List<Sale> findByUserId(Long userId);

    List<Sale> findByStatus(String status);
}
