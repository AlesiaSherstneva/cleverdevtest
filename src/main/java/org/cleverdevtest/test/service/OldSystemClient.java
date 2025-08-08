package org.cleverdevtest.test.service;

import org.cleverdevtest.test.dto.ClientDto;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.PostMapping;

import java.util.List;

@FeignClient(name = "oldSystemClient", url = "${old.system.url}")
public interface OldSystemClient {
    @PostMapping("/clients")
    List<ClientDto> getAllClients();
}