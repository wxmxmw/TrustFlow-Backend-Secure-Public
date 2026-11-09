package com.trustflow.backend.repo;

import com.trustflow.backend.model.Session;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface SessionRepository extends JpaRepository<Session, Long> {
    List<Session> findByUserIdOrderByCreatedAtDesc(Long userId);
}

