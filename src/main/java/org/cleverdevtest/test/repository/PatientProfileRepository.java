package org.cleverdevtest.test.repository;

import org.cleverdevtest.test.model.PatientProfile;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Set;

public interface PatientProfileRepository extends JpaRepository<PatientProfile, Long> {
    List<PatientProfile> findByStatusIdIn(Set<Short> statusIds);
}