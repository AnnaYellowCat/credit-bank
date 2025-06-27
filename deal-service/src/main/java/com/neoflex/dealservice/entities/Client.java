package com.neoflex.dealservice.entities;

import com.neoflex.dealservice.enums.Gender;
import com.neoflex.dealservice.enums.MaritalStatus;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

@Entity
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Client {
    @Id
    private UUID clientId;
    private String firstName;
    private String lastName;
    private String middleName;
    private LocalDate birthDate;
    private String email;
    @Enumerated(EnumType.STRING)
    private Gender gender;
    @Enumerated(EnumType.STRING)
    private MaritalStatus maritalStatus;
    private Integer dependentAmount;
    @JdbcTypeCode(SqlTypes.JSON)
    private Passport passport;
    @JdbcTypeCode(SqlTypes.JSON)
    private Employment employment;
    private String accountNumber;

    @OneToMany(mappedBy = "client")
    private List<Statement> statements;
}
