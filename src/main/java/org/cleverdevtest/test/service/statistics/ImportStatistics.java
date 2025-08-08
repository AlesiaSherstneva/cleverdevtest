package org.cleverdevtest.test.service.statistics;

import lombok.Getter;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Getter
public class ImportStatistics {
    private int usersCreated;
    private int patientsCreated;
    private int notesCreated;
    private int notesUpdated;
    private int failures;

    public void userCreated() {
        usersCreated++;
    }

    public void patientCreated() {
        patientsCreated++;
    }

    public void noteCreated() {
        notesCreated++;
    }

    public void noteUpdated() {
        notesUpdated++;
    }

    public void errorOccurred() {
        failures++;
    }

    public void reset() {
        usersCreated = 0;
        patientsCreated = 0;
        notesCreated = 0;
        notesUpdated = 0;
        failures = 0;
    }

    public void logSummary() {
        log.info("=== Import Summary ===");
        log.info("Users created: {}", usersCreated);
        log.info("Patients created: {}", patientsCreated);
        log.info("Notes created: {}", notesCreated);
        log.info("Notes updated: {}", notesUpdated);
        log.info("Processing errors: {}", failures);
    }
}