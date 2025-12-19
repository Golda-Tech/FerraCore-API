package com.goldatech.authservice.domain.repository;

import com.goldatech.authservice.domain.model.OrgIdSequence;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.Instant;

public interface OrgIdSequenceRepository extends JpaRepository<OrgIdSequence, Long> {

    @Query("SELECT COALESCE(MAX(seq), 0) FROM OrgIdSequence")
    Integer findMaxSequence();

    @Modifying
    @Query(value = "INSERT INTO org_id_sequence(seq, created_at) VALUES (:seq, :ts)", nativeQuery = true)
    void saveSequence(@Param("seq") Integer seq, @Param("ts") Instant ts);
}