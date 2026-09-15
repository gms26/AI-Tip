package com.aitip.repository;

import com.aitip.entity.PaymentSession;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;
import java.util.UUID;

@Repository
public interface PaymentSessionRepository extends JpaRepository<PaymentSession, UUID> {
    Optional<PaymentSession> findByIdAndUserId(UUID id, UUID userId);
}
