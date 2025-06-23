package com.neoflex.dealservice.dto;

import com.neoflex.dealservice.enums.ApplicationStatus;
import com.neoflex.dealservice.enums.ChangeType;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class StatementStatusHistoryDto {
    private ApplicationStatus status;
    private LocalDateTime time;
    private ChangeType changeType;
}
