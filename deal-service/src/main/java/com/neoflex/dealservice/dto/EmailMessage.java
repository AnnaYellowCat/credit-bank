package com.neoflex.dealservice.dto;

import com.neoflex.dealservice.enums.EmailMessageTheme;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.UUID;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class EmailMessage {
    private String address;
    private EmailMessageTheme theme;
    private UUID statementId;
    private String text;
}
