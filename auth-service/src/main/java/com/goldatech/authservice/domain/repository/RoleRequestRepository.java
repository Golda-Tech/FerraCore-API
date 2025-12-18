package com.goldatech.authservice.domain.repository;

import com.goldatech.authservice.domain.model.RoleRequest;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.UUID;

public interface RoleRequestRepository extends JpaRepository<RoleRequest, Long> {
    List<RoleRequest> findByUserEmailOrderByRequestedAtDesc(String userEmail);
}