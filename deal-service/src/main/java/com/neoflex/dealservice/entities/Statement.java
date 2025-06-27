package com.neoflex.dealservice.entities;

import com.neoflex.dealservice.dto.LoanOfferDto;
import com.neoflex.dealservice.dto.StatementStatusHistoryDto;
import com.neoflex.dealservice.enums.ApplicationStatus;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

@Entity
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Statement {
    @Id
    private UUID statementId;
    @ManyToOne
    @JoinColumn(name = "client_id", referencedColumnName = "clientId")
    private Client client;
    @ManyToOne(cascade = CascadeType.ALL)
    @JoinColumn(name = "credit_id", referencedColumnName = "creditId")
    private Credit credit;
    @Enumerated(EnumType.STRING)
    private ApplicationStatus status;
    private LocalDateTime creationDate;
    @JdbcTypeCode(SqlTypes.JSON)
    private LoanOfferDto appliedOffer;
    private LocalDateTime signDate;
    private String sesCode;
    @JdbcTypeCode(SqlTypes.JSON)
    private List<StatementStatusHistoryDto> statusHistory;
}
