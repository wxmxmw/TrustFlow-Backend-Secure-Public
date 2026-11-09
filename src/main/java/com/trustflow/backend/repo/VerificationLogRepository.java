package com.trustflow.backend.repo;

import com.trustflow.backend.model.VerificationLog;
import org.springframework.data.jpa.repository.JpaRepository;

public interface VerificationLogRepository extends JpaRepository<VerificationLog, Long> {
}
