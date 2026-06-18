package com.millennial.worker.notification.repository;

import com.millennial.worker.notification.entity.WorkerNotificationEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface WorkerNotificationRepository extends JpaRepository<WorkerNotificationEntity, Long> {
    boolean existsByUserIdAndTaskIdAndType(Long userId, Long taskId, String type);
    List<WorkerNotificationEntity> findByUserIdOrderByCreatedAtDesc(Long userId);
    List<WorkerNotificationEntity> findByUserIdAndIsReadOrderByCreatedAtDesc(Long userId, boolean isRead);
}
