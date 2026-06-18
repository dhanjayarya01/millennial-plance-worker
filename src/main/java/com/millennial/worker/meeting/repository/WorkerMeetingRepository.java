package com.millennial.worker.meeting.repository;

import com.millennial.worker.meeting.entity.WorkerMeetingEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;

@Repository
public interface WorkerMeetingRepository extends JpaRepository<WorkerMeetingEntity, Long> {
    List<WorkerMeetingEntity> findByMeetingTimeBeforeAndAlerted1mFalse(LocalDateTime time);
}
