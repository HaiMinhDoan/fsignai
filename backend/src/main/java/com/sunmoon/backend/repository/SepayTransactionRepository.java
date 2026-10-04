package com.sunmoon.backend.repository;

import com.sunmoon.backend.entity.payment.SepayTransaction;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.UUID;

@Repository
public interface SepayTransactionRepository extends JpaRepository<SepayTransaction, UUID> {

    boolean existsBySepayId(Long sepayId);
}
