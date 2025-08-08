package org.cleverdevtest.test.repository;

import org.cleverdevtest.test.model.PatientNote;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface PatientNoteRepository extends JpaRepository<PatientNote, Long> {
    Optional<PatientNote> findByOldNoteGuid(String oldNoteGuid);
}