package org.cleverdevtest.test.service.mapper;

import org.cleverdevtest.test.dto.ClientDto;
import org.cleverdevtest.test.dto.NotesRequestDto;
import org.cleverdevtest.test.model.PatientProfile;
import org.springframework.stereotype.Component;

import java.time.LocalDate;

@Component
public class ClientMapper {
    public PatientProfile clientToNewPatient(ClientDto client) {
        return PatientProfile.builder()
                .firstName(client.getFirstName())
                .lastName(client.getLastName())
                .oldClientGuid(client.getGuid())
                .statusId((short) 200)
                .build();
    }

    public NotesRequestDto clientToNoteRequestDto(ClientDto client) {
        return NotesRequestDto.builder()
                .agency(client.getAgency())
                .dateFrom(LocalDate.of(2000, 1, 1))
                .dateTo(LocalDate.now())
                .clientGuid(client.getGuid())
                .build();
    }
}