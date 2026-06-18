package com.millennial.worker.common;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface LookupTaskRepository extends JpaRepository<LookupTaskEntity, Long> {
    List<LookupTaskEntity> findByStatusNot(String status);
}
