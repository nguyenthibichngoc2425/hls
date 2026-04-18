package com.rin.hlsserver.repository;

import com.rin.hlsserver.model.SystemLog;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface SystemLogRepository extends JpaRepository<SystemLog, Long> {

    @Query("SELECT s FROM SystemLog s ORDER BY s.createdAt DESC")
    List<SystemLog> findLatestLogs(org.springframework.data.domain.Pageable pageable);

    @Query("SELECT s FROM SystemLog s WHERE s.eventType IN :eventTypes ORDER BY s.createdAt DESC")
    List<SystemLog> findLatestLogsByEventTypes(@Param("eventTypes") List<String> eventTypes,
                                               org.springframework.data.domain.Pageable pageable);
}