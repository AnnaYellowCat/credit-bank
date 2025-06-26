package com.neoflex.dealservice.mappers;

import com.neoflex.dealservice.dto.EmploymentDto;
import com.neoflex.dealservice.dto.FinishRegistrationRequestDto;
import com.neoflex.dealservice.entities.Employment;
import com.neoflex.dealservice.entities.Passport;
import com.neoflex.dealservice.enums.EmploymentPosition;
import com.neoflex.dealservice.enums.EmploymentStatus;
import com.neoflex.dealservice.enums.Gender;
import com.neoflex.dealservice.enums.MaritalStatus;
import org.mapstruct.*;
import com.neoflex.dealservice.dto.LoanStatementRequestDto;
import com.neoflex.dealservice.entities.Client;

import java.util.UUID;

@Mapper(componentModel = "spring")
public interface ClientMapper {

    @Mapping(target = "clientId", ignore = true)
    @Mapping(target = "passport", source = ".", qualifiedByName = "mapPassport")
    Client toClient(LoanStatementRequestDto dto);

    @Mapping(target = "gender", ignore = true)
    @Mapping(target = "maritalStatus", ignore = true)
    @Mapping(target = "passport", ignore = true)
    @Mapping(target = "dependentAmount", source = "finishRegistrationRequest.dependentAmount")
    @Mapping(target = "accountNumber", source = "finishRegistrationRequest.accountNumber")
    @Mapping(target = "employment", source = "finishRegistrationRequest.employment", qualifiedByName = "createEmployment")
    Client updateClient(Client client, Passport passport, FinishRegistrationRequestDto finishRegistrationRequest);

    @Named("mapPassport")
    default Passport createPassport(LoanStatementRequestDto dto) {
        return Passport.builder()
                .passportId(UUID.randomUUID())
                .series(dto.getPassportSeries())
                .number(dto.getPassportNumber())
                .build();
    }

    @Named("createEmployment")
    default Employment createEmployment(EmploymentDto employmentDto) {
        return Employment.builder()
                .employmentId(UUID.randomUUID())
                .employmentStatus(EmploymentStatus.valueOf(employmentDto.getEmploymentStatus()))
                .employerInn(employmentDto.getEmployerINN())
                .salary(employmentDto.getSalary())
                .position(EmploymentPosition.valueOf(employmentDto.getPosition()))
                .workExperienceTotal(employmentDto.getWorkExperienceTotal())
                .workExperienceCurrent(employmentDto.getWorkExperienceCurrent())
                .build();
    }

    @AfterMapping
    default void setClientId(@MappingTarget Client client) {
        client.setClientId(UUID.randomUUID());
    }

    @AfterMapping
    default void updateEnumFieldsAnfPassport(FinishRegistrationRequestDto dto, Passport passport,
                                             @MappingTarget Client client) {
        client.setGender(Gender.valueOf(dto.getGender()));
        client.setMaritalStatus(MaritalStatus.valueOf(dto.getMaritalStatus()));
        passport.setIssueBranch(dto.getPassportIssueBranch());
        passport.setIssueDate(dto.getPassportIssueDate());
        client.setPassport(passport);
    }
}