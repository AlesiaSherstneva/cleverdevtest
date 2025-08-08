package org.cleverdevtest.test.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class NotesRequestDto {
    private String agency;
    private String dateFrom;
    private String dateTo;
    private String clientGuid;
}