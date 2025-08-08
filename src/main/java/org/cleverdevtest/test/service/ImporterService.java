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

@Service
@RequiredArgsConstructor
@Slf4j
public class ImporterService {
    private final OldSystemClient oldSystemClient;
    private final OldSystemNotesClient oldSystemNotesClient;
    private final PatientProfileRepository patientProfileRepository;
    private final CompanyUserRepository companyUserRepository;
    private final PatientNoteRepository patientNoteRepository;

    @Scheduled(cron = "0 15 1/2 * * ?") // Каждые 2 часа в 15 минут первого часа
    @Transactional
    public void importNotes() {
        log.info("Starting notes import process");

        // Статистика импорта
        int totalNotesProcessed = 0;
        int newNotesCreated = 0;
        int notesUpdated = 0;
        int errors = 0;

        try {
            // Получаем всех клиентов из старой системы
            List<ClientDto> oldClients = oldSystemClient.getAllClients();

            // Получаем всех активных пациентов из новой системы
            List<PatientProfile> activePatients = patientProfileRepository.findByStatusIdIn(
                    Set.of((short)200, (short)210, (short)230));

            // Создаем маппинг oldClientGuid -> PatientProfile
            Map<String, PatientProfile> guidToPatientMap = new HashMap<>();
            for (PatientProfile patient : activePatients) {
                if (patient.getOldClientGuid() != null) {
                    Arrays.stream(patient.getOldClientGuid().split(","))
                            .map(String::trim)
                            .forEach(guid -> guidToPatientMap.put(guid, patient));
                }
            }

            // Обрабатываем заметки для каждого клиента
            for (ClientDto client : oldClients) {
                if (!guidToPatientMap.containsKey(client.getGuid())) {
                    continue;
                }

                PatientProfile patient = guidToPatientMap.get(client.getGuid());

                try {
                    // Получаем заметки для клиента
                    NotesRequestDto request = new NotesRequestDto(
                            client.getAgency(),
                            LocalDate.now().minusYears(1).toString(),
                            LocalDate.now().toString(),
                            client.getGuid()
                    );

                    List<ClientNoteDto> clientNotes = oldSystemNotesClient.getClientNotes(request);

                    for (ClientNoteDto noteDto : clientNotes) {
                        try {
                            totalNotesProcessed++;

                            // Находим или создаем пользователя
                            CompanyUser user = companyUserRepository.findByLogin(noteDto.getLoggedUser())
                                    .orElseGet(() -> {
                                        CompanyUser newUser = new CompanyUser();
                                        newUser.setLogin(noteDto.getLoggedUser());
                                        return companyUserRepository.save(newUser);
                                    });

                            // Проверяем, существует ли уже заметка
                            Optional<PatientNote> existingNote = patientNoteRepository.findByOldNoteGuid(noteDto.getGuid());

                            if (existingNote.isPresent()) {
                                // Обновляем существующую заметку, если она была изменена
                                PatientNote note = existingNote.get();
                                LocalDateTime oldModifiedTime = parseDateTime(noteDto.getModifiedDateTime());

                                if (oldModifiedTime.isAfter(note.getLastModifiedDateTime())) {
                                    note.setNote(noteDto.getComments());
                                    note.setLastModifiedDateTime(oldModifiedTime);
                                    note.setLastModifiedByUser(user);
                                    patientNoteRepository.save(note);
                                    notesUpdated++;
                                }
                            } else {
                                // Создаем новую заметку
                                PatientNote newNote = new PatientNote();
                                newNote.setOldNoteGuid(noteDto.getGuid());
                                newNote.setNote(noteDto.getComments());
                                newNote.setCreatedDateTime(parseDateTime(noteDto.getCreatedDateTime()));
                                newNote.setLastModifiedDateTime(parseDateTime(noteDto.getModifiedDateTime()));
                                newNote.setCreatedByUser(user);
                                newNote.setLastModifiedByUser(user);
                                newNote.setPatient(patient);

                                patientNoteRepository.save(newNote);
                                newNotesCreated++;
                            }
                        } catch (Exception e) {
                            errors++;
                            log.error("Error processing note with guid {}: {}", noteDto.getGuid(), e.getMessage(), e);
                        }
                    }
                } catch (Exception e) {
                    errors++;
                    log.error("Error processing client with guid {}: {}", client.getGuid(), e.getMessage(), e);
                }
            }

            log.info("Notes import completed. Statistics: " +
                            "Total processed: {}, New created: {}, Updated: {}, Errors: {}",
                    totalNotesProcessed, newNotesCreated, notesUpdated, errors);
        } catch (Exception e) {
            log.error("Fatal error during notes import: {}", e.getMessage(), e);
        }
    }

    private LocalDateTime parseDateTime(String dateTimeStr) {
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");
        return LocalDateTime.parse(dateTimeStr, formatter);
    }
}