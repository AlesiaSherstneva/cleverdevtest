package org.cleverdevtest.test.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.cleverdevtest.test.dto.ClientDto;
import org.cleverdevtest.test.dto.ClientNoteDto;
import org.cleverdevtest.test.dto.NotesRequestDto;
import org.cleverdevtest.test.model.CompanyUser;
import org.cleverdevtest.test.model.PatientNote;
import org.cleverdevtest.test.model.PatientProfile;
import org.cleverdevtest.test.repository.CompanyUserRepository;
import org.cleverdevtest.test.repository.PatientNoteRepository;
import org.cleverdevtest.test.repository.PatientProfileRepository;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

@Slf4j
@Service
@RequiredArgsConstructor
public class ImporterService {
    private final OldSystemClient oldSystemClient;
    private final OldSystemNotesClient oldSystemNotesClient;
    private final CompanyUserRepository companyUserRepository;
    private final PatientProfileRepository patientProfileRepository;
    private final PatientNoteRepository patientNoteRepository;

    @Scheduled(cron = "0 15 1/2 * * ?")
    @Transactional
    public void importNotes() {
        log.info("Starting notes import");

        List<ClientDto> oldClients = oldSystemClient.getAllClients();

        List<PatientProfile> activePatients = patientProfileRepository.findByStatusIdIn(Set.of((short)200, (short)210, (short)230));

        // 3. Сопоставляем GUID клиентов с пациентами
        Map<String, PatientProfile> guidToPatientMap = createGuidToPatientMap(activePatients);

        // 4. Обрабатываем заметки
        for (ClientDto client : oldClients) {
            if (!guidToPatientMap.containsKey(client.getGuid())) continue;

            PatientProfile patient = guidToPatientMap.get(client.getGuid());
            processClientNotes(client, patient);
        }
    }

    // Создаем маппинг old_client_guid -> PatientProfile
    private Map<String, PatientProfile> createGuidToPatientMap(List<PatientProfile> patients) {
        Map<String, PatientProfile> map = new HashMap<>();
        for (PatientProfile patient : patients) {
            if (patient.getOldClientGuid() != null) {
                Arrays.stream(patient.getOldClientGuid().split(","))
                        .map(String::trim)
                        .forEach(guid -> map.put(guid, patient));
            }
        }
        return map;
    }

    // Обрабатываем заметки для одного клиента
    private void processClientNotes(ClientDto client, PatientProfile patient) {
        try {
            // Запрашиваем заметки клиента
            NotesRequestDto request = new NotesRequestDto(
                    client.getAgency(),
                    LocalDate.now().minusYears(1).toString(),
                    LocalDate.now().toString(),
                    client.getGuid()
            );
            List<ClientNoteDto> notes = oldSystemNotesClient.getClientNotes(request);

            // Обрабатываем каждую заметку
            for (ClientNoteDto noteDto : notes) {
                // Находим или создаем врача
                CompanyUser doctor = companyUserRepository.findByLogin(noteDto.getLoggedUser())
                        .orElseGet(() -> createNewDoctor(noteDto.getLoggedUser()));

                // Обрабатываем заметку
                processNote(noteDto, patient, doctor);
            }
        } catch (Exception e) {
            log.error("Error processing client {}: {}", client.getGuid(), e.getMessage());
        }
    }

    // Создаем нового врача
    private CompanyUser createNewDoctor(String login) {
        CompanyUser doctor = new CompanyUser();
        doctor.setLogin(login);
        return companyUserRepository.save(doctor);
    }

    // Обновляем или создаем заметку
    private void processNote(ClientNoteDto noteDto, PatientProfile patient, CompanyUser doctor) {
        Optional<PatientNote> existingNote = patientNoteRepository.findByOldNoteGuid(noteDto.getGuid());

        if (existingNote.isPresent()) {
            updateExistingNote(existingNote.get(), noteDto, doctor);
        } else {
            createNewNote(noteDto, patient, doctor);
        }
    }

    private void updateExistingNote(PatientNote note, ClientNoteDto noteDto, CompanyUser doctor) {
        LocalDateTime oldModifiedTime = parseDateTime(noteDto.getModifiedDateTime());
        if (oldModifiedTime.isAfter(note.getLastModifiedDateTime())) {
            note.setNote(noteDto.getComments());
            note.setLastModifiedDateTime(oldModifiedTime);
            note.setLastModifiedByUser(doctor);
            patientNoteRepository.save(note);
        }
    }

    private void createNewNote(ClientNoteDto noteDto, PatientProfile patient, CompanyUser doctor) {
        PatientNote newNote = new PatientNote();
        newNote.setOldNoteGuid(noteDto.getGuid());
        newNote.setNote(noteDto.getComments());
        newNote.setCreatedDateTime(parseDateTime(noteDto.getCreatedDateTime()));
        newNote.setLastModifiedDateTime(parseDateTime(noteDto.getModifiedDateTime()));
        newNote.setCreatedByUser(doctor);
        newNote.setLastModifiedByUser(doctor);
        newNote.setPatient(patient);
        patientNoteRepository.save(newNote);
    }

    private LocalDateTime parseDateTime(String dateTimeStr) {
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");
        return LocalDateTime.parse(dateTimeStr, formatter);
    }
}