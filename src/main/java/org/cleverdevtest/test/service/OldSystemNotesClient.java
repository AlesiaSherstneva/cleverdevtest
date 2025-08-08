package org.cleverdevtest.test.service;

import org.cleverdevtest.test.dto.ClientNoteDto;
import org.cleverdevtest.test.dto.NotesRequestDto;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;

import java.util.List;

@FeignClient(name = "oldSystemNotesClient", url = "${old.system.url}")
public interface OldSystemNotesClient {
    @PostMapping("/notes")
    List<ClientNoteDto> getClientNotes(@RequestBody NotesRequestDto request);
}