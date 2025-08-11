package org.cleverdevtest.test.service;

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
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;

import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.Optional;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyIterable;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.mockito.Mockito.when;

@SpringBootTest
class ImporterServiceTest {

    @MockBean
    private OldSystemClient oldSystemClient;

    @MockBean
    private CompanyUserRepository companyUserRepository;

    @MockBean
    private PatientProfileRepository patientProfileRepository;

    @MockBean
    private PatientNoteRepository patientNoteRepository;

    @Autowired
    private ImporterService importerService;

    private ClientDto testClient;
    private ClientNoteDto testClientNote;
    private PatientProfile testPatient;

    @Test
    void importClientsSuccessfulTest() {
        testClient = ClientDto.builder()
                .guid("test-guid")
                .build();

        when(oldSystemClient.getAllClients()).thenReturn(List.of(testClient));

        importerService.importNotes();

        verify(oldSystemClient, times(1)).getAllClients();
        verify(patientProfileRepository, times(1)).findByOldClientGuidContaining(anyString());
        verify(patientProfileRepository, times(1)).save(any(PatientProfile.class));
    }

    @Test
    void importClientsOldSystemIsNotAvailableTest() {
        testClient = ClientDto.builder().build();

        when(oldSystemClient.getAllClients()).thenThrow(new RuntimeException("Old system error"));

        importerService.importNotes();

        verify(oldSystemClient, times(1)).getAllClients();
    }

    @Test
    void patientFoundInDbTest() {
        testClient = ClientDto.builder()
                .guid("test-guid")
                .build();
        testPatient = PatientProfile.builder().build();

        when(oldSystemClient.getAllClients()).thenReturn(List.of(testClient));
        when(patientProfileRepository.findByOldClientGuidContaining(anyString()))
                .thenReturn(Optional.of(testPatient));

        importerService.importNotes();

        verify(oldSystemClient, times(1)).getAllClients();
        verify(patientProfileRepository, times(1)).findByOldClientGuidContaining(anyString());
    }

    @Test
    void patientNotFoundInDbTest() {
        testClient = ClientDto.builder()
                .guid("test-guid")
                .build();

        when(oldSystemClient.getAllClients()).thenReturn(List.of(testClient));
        when(patientProfileRepository.findByOldClientGuidContaining(anyString()))
                .thenReturn(Optional.empty());

        importerService.importNotes();

        verify(oldSystemClient, times(1)).getAllClients();
        verify(patientProfileRepository, times(1)).findByOldClientGuidContaining(anyString());
        verify(patientProfileRepository, times(1)).save(any(PatientProfile.class));
    }

    @Test
    void patientIsInactiveTest() {
        testClient = ClientDto.builder()
                .guid("test-guid")
                .build();
        testPatient = PatientProfile.builder()
                .statusId((short) 100)
                .build();

        when(oldSystemClient.getAllClients()).thenReturn(List.of(testClient));
        when(patientProfileRepository.findByOldClientGuidContaining(anyString()))
                .thenReturn(Optional.of(testPatient));

        importerService.importNotes();

        verify(oldSystemClient, times(1)).getAllClients();
        verify(patientProfileRepository, times(1)).findByOldClientGuidContaining(anyString());
    }

    @Test
    void importNotesSuccessfulTest() {
        testClient = ClientDto.builder()
                .guid("test-guid")
                .build();
        testPatient = PatientProfile.builder()
                .statusId((short) 200)
                .build();
        testClientNote = ClientNoteDto.builder().build();

        when(oldSystemClient.getAllClients()).thenReturn(List.of(testClient));
        when(patientProfileRepository.findByOldClientGuidContaining(anyString()))
                .thenReturn(Optional.of(testPatient));
        when(oldSystemClient.getClientNotes(any(NotesRequestDto.class))).thenReturn(List.of(testClientNote));

        importerService.importNotes();

        verify(oldSystemClient, times(1)).getAllClients();
        verify(patientProfileRepository, times(1)).findByOldClientGuidContaining(anyString());
        verify(oldSystemClient, times(1)).getClientNotes(any(NotesRequestDto.class));
    }

    @Test
    void userFoundInDbAndNoteIsNewTest() {
        testClient = ClientDto.builder()
                .guid("test-guid")
                .build();
        testPatient = PatientProfile.builder()
                .statusId((short) 200)
                .build();
        CompanyUser testUser = CompanyUser.builder().build();
        testClientNote = ClientNoteDto.builder()
                .loggedUser("Doctor Test")
                .comments("Test note")
                .guid("test-guid")
                .build();

        when(oldSystemClient.getAllClients()).thenReturn(List.of(testClient));
        when(patientProfileRepository.findByOldClientGuidContaining(anyString()))
                .thenReturn(Optional.of(testPatient));
        when(oldSystemClient.getClientNotes(any(NotesRequestDto.class))).thenReturn(List.of(testClientNote));
        when(companyUserRepository.findByLogin(anyString())).thenReturn(Optional.of(testUser));
        when(patientNoteRepository.findByOldNoteGuid(anyString())).thenReturn(Optional.empty());

        importerService.importNotes();

        verify(oldSystemClient, times(1)).getAllClients();
        verify(patientProfileRepository, times(1)).findByOldClientGuidContaining(anyString());
        verify(oldSystemClient, times(1)).getClientNotes(any(NotesRequestDto.class));
        verify(companyUserRepository, times(1)).findByLogin(anyString());
        verify(patientNoteRepository, times(1)).findByOldNoteGuid(anyString());
        verify(patientNoteRepository, times(1)).saveAll(anyIterable());
    }

    @Test
    void userDidNotFoundInDbAndNoteNeedsUpdateTest() {
        testClient = ClientDto.builder()
                .guid("test-guid")
                .build();
        testPatient = PatientProfile.builder()
                .statusId((short) 200)
                .build();
        testClientNote = ClientNoteDto.builder()
                .comments("Test note")
                .guid("test-guid")
                .loggedUser("Doctor Test")
                .modifiedDateTime(LocalDateTime.now())
                .build();
        PatientNote existingNote = PatientNote.builder()
                .lastModifiedDateTime(LocalDateTime.now().minus(1, ChronoUnit.DAYS))
                .build();

        when(oldSystemClient.getAllClients()).thenReturn(List.of(testClient));
        when(patientProfileRepository.findByOldClientGuidContaining(anyString()))
                .thenReturn(Optional.of(testPatient));
        when(oldSystemClient.getClientNotes(any(NotesRequestDto.class))).thenReturn(List.of(testClientNote));
        when(companyUserRepository.findByLogin(anyString())).thenReturn(Optional.empty());
        when(patientNoteRepository.findByOldNoteGuid(anyString())).thenReturn(Optional.of(existingNote));

        importerService.importNotes();

        verify(oldSystemClient, times(1)).getAllClients();
        verify(patientProfileRepository, times(1)).findByOldClientGuidContaining(anyString());
        verify(oldSystemClient, times(1)).getClientNotes(any(NotesRequestDto.class));
        verify(companyUserRepository, times(1)).findByLogin(anyString());
        verify(companyUserRepository, times(1)).save(any(CompanyUser.class));
        verify(patientNoteRepository, times(1)).findByOldNoteGuid(anyString());
        verify(patientNoteRepository, times(1)).saveAll(anyIterable());
    }

    @AfterEach
    void tearDown() {
        verifyNoMoreInteractions(oldSystemClient, companyUserRepository, patientProfileRepository, patientNoteRepository);
    }
}