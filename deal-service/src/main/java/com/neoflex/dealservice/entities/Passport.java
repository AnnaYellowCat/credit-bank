package com.neoflex.dealservice.entities;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDate;
import java.util.UUID;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Passport {
    private UUID passportId;
    private String series;
    private String number;
    private String issueBranch;
    private LocalDate issueDate;
}
