package com.aitip.repository;

import com.aitip.entity.TipPool;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface TipPoolRepository extends JpaRepository<TipPool, UUID> {
    
    List<TipPool> findAllByUserIdOrderByCreatedAtDesc(UUID userId);
    
    Optional<TipPool> findByIdAndUserId(UUID id, UUID userId);
    
    boolean existsByIdAndUserId(UUID id, UUID userId);
}
