package org.cleverdevtest.test.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.cleverdevtest.test.client.OldSystemClient;
import org.cleverdevtest.test.dto.ClientDto;
import org.cleverdevtest.test.dto.ClientNoteDto;
import org.cleverdevtest.test.dto.NotesRequestDto;
import org.cleverdevtest.test.model.CompanyUser;
import org.cleverdevtest.test.model.PatientNote;
import org.cleverdevtest.test.model.PatientProfile;
import org.cleverdevtest.test.repository.CompanyUserRepository;
import org.cleverdevtest.test.repository.PatientNoteRepository;
import org.cleverdevtest.test.repository.PatientProfileRepository;
import org.cleverdevtest.test.service.mapper.ClientMapper;
import org.cleverdevtest.test.service.mapper.NoteMapper;
import org.cleverdevtest.test.service.statistics.ImportStatistics;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class ImporterService {
    private final OldSystemClient oldSystemClient;
    private final CompanyUserRepository companyUserRepository;
    private final PatientProfileRepository patientProfileRepository;
    private final PatientNoteRepository patientNoteRepository;
    private final ClientMapper clientMapper;
    private final NoteMapper noteMapper;

    private final ImportStatistics stats = new ImportStatistics();

    private static final Set<Short> ACTIVE_STATUSES = Set.of((short)200, (short)210, (short)230);

    @Scheduled(cron = "0 15 1/2 * * ?")
    public void importNotes() {
        stats.reset();

        log.info("=== Import started ===");

        List<ClientDto> oldClients = oldSystemClient.getAllClients();

        oldClients.forEach(client -> {
            try {
                processNotesList(client);
            } catch (Exception ex) {
                log.error("Import for client with guid {} failed. {}", client.getGuid(), ex.getMessage());
                stats.errorOccurred();
            }
        });

        log.info("=== Import completed ===");

        stats.logSummary();
    }

    @Transactional
    void processNotesList(ClientDto client) {
        PatientProfile patient = patientProfileRepository.findByOldClientGuidContaining(client.getGuid())
                .orElseGet(() -> {
                    PatientProfile newPatient = clientMapper.clientToNewPatient(client);
                    stats.patientCreated();
                    return patientProfileRepository.save(newPatient);
                });

        if (!ACTIVE_STATUSES.contains(patient.getStatusId())) {
            return;
        }

        NotesRequestDto requestForNotes = clientMapper.clientToNoteRequestDto(client);
        List<ClientNoteDto> clientNotes = oldSystemClient.getClientNotes(requestForNotes);

        Map<String, List<ClientNoteDto>> clientNotesByDoctor = clientNotes.stream()
                .collect(Collectors.groupingBy(ClientNoteDto::getLoggedUser));

        clientNotesByDoctor.forEach((doctorLogin, doctorNotes) -> {
            CompanyUser doctor = companyUserRepository.findByLogin(doctorLogin)
                    .orElseGet(() -> {
                        CompanyUser newDoctor = CompanyUser.builder()
                                .login(doctorLogin)
                                .build();
                        stats.userCreated();
                        return companyUserRepository.save(newDoctor);
                    });

            List<PatientNote> notesToSave = doctorNotes.stream()
                    .map(note -> processSingleNote(note, doctor, patient))
                    .filter(Optional::isPresent)
                    .map(Optional::get)
                    .toList();

            patientNoteRepository.saveAll(notesToSave);
        });
    }

    private Optional<PatientNote> processSingleNote(ClientNoteDto clientNote,
                                                    CompanyUser doctor,
                                                    PatientProfile patient) {
        Optional<PatientNote> noteInDb = patientNoteRepository.findByOldNoteGuid(clientNote.getGuid());

        if (noteInDb.isEmpty()) {
            stats.noteCreated();
            return Optional.of(noteMapper.clientNoteToPatientNote(clientNote, doctor, patient));
        } else if (clientNote.getModifiedDateTime().isAfter(noteInDb.get().getLastModifiedDateTime())) {
            PatientNote updatingNote = noteInDb.get();
            updatingNote.setLastModifiedDateTime(clientNote.getModifiedDateTime());
            updatingNote.setLastModifiedByUser(doctor);
            updatingNote.setNote(clientNote.getComments());

            stats.noteUpdated();
            return Optional.of(updatingNote);
        }

        return Optional.empty();
    }
}