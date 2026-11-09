package com.trustflow.backend.repo;

import com.trustflow.backend.model.OnchainProof;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface OnchainProofRepository extends JpaRepository<OnchainProof, Long> {
    Optional<OnchainProof> findByTxHash(String txHash);

    Optional<OnchainProof> findByGenerationRecordId(Long generationRecordId);
}
