package org.cleverdevtest.test.dto;

import lombok.Data;

@Data
public class ClientNoteDto {
    private String comments;
    private String guid;
    private String modifiedDateTime;
    private String clientGuid;
    private String datetime;
    private String loggedUser;
    private String createdDateTime;
}