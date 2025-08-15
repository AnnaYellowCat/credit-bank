package com.neoflex.dealservice.entities;

import com.neoflex.dealservice.dto.PaymentScheduleElementDto;
import com.neoflex.dealservice.enums.CreditStatus;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;

@Entity
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Credit {
    @Id
    private UUID creditId;
    private BigDecimal amount;
    private Integer term;
    private BigDecimal monthlyPayment;
    private BigDecimal rate;
    private BigDecimal psk;
    @JdbcTypeCode(SqlTypes.JSON)
    private List<PaymentScheduleElementDto> paymentSchedule;
    private Boolean insuranceEnabled;
    private Boolean salaryClient;
    @Enumerated(EnumType.STRING)
    private CreditStatus creditStatus;

    @OneToMany(mappedBy = "credit")
    private List<Statement> statements;
}
