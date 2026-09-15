package com.aitip.repository;

import com.aitip.entity.PoolMember;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

@Repository
public interface PoolMemberRepository extends JpaRepository<PoolMember, UUID> {
    
    List<PoolMember> findByPoolId(UUID poolId);
}
