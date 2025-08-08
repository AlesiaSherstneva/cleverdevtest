package org.cleverdevtest.test.repository;

import org.cleverdevtest.test.model.PatientNote;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Optional;

public interface PatientNoteRepository extends JpaRepository<PatientNote, Long> {
    @Query("SELECT DISTINCT pn FROM PatientNote pn " +
            "JOIN FETCH pn.createdByUser " +
            "JOIN FETCH pn.lastModifiedByUser " +
            "JOIN FETCH pn.patient " +
            "WHERE pn.oldNoteGuid = :oldNoteGuid")
    Optional<PatientNote> findByOldNoteGuid(@Param("oldNoteGuid") String oldNoteGuid);
}