package org.cleverdevtest.test.client;

import org.cleverdevtest.test.dto.ClientDto;
import org.cleverdevtest.test.dto.ClientNoteDto;
import org.cleverdevtest.test.dto.NotesRequestDto;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;

import java.util.List;

@FeignClient(name = "oldSystem", url = "${old.system.url}")
public interface OldSystemClient {
    @PostMapping("/clients")
    List<ClientDto> getAllClients();

    @PostMapping("/notes")
    List<ClientNoteDto> getClientNotes(@RequestBody NotesRequestDto request);
}