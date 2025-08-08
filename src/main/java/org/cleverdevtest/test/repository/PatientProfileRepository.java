package org.cleverdevtest.test.repository;

import org.cleverdevtest.test.model.PatientProfile;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface PatientProfileRepository extends JpaRepository<PatientProfile, Long> {
    Optional<PatientProfile> findByOldClientGuidContaining(String oldClientGuid);
}