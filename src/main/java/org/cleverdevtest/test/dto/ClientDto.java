package org.cleverdevtest.test.dto;

import lombok.Data;

@Data
public class ClientDto {
    private String agency;
    private String guid;
    private String firstName;
    private String lastName;
    private String status;
    private String dob;
    private String createdDateTime;
}