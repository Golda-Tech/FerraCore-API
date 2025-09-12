package com.goldatech.notificationservice.domain.repository;

import com.goldatech.notificationservice.domain.model.NotificationLog;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface NotificationLogRepository extends JpaRepository<NotificationLog, Long> {
    List<NotificationLog> findByUserIdOrderByTimestampDesc(String userId);
}
