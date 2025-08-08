package org.cleverdevtest.test.service.mapper;

import org.cleverdevtest.test.dto.ClientNoteDto;
import org.cleverdevtest.test.model.CompanyUser;
import org.cleverdevtest.test.model.PatientNote;
import org.cleverdevtest.test.model.PatientProfile;
import org.springframework.stereotype.Component;

@Component
public class NoteMapper {
    public PatientNote clientNoteToPatientNote(ClientNoteDto clientNote,
                                               CompanyUser doctor,
                                               PatientProfile patient) {
        return PatientNote.builder()
                .createdDateTime(clientNote.getCreatedDateTime())
                .lastModifiedDateTime(clientNote.getModifiedDateTime())
                .createdByUser(doctor)
                .lastModifiedByUser(doctor)
                .note(clientNote.getComments())
                .patient(patient)
                .oldNoteGuid(clientNote.getGuid())
                .build();
    }
}