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

@Mapper(componentModel = "spring", imports = UUID.class)
public interface ClientMapper {

    @Mapping(target = "clientId", expression = "java(UUID.randomUUID())")
    @Mapping(target = "passport", source = ".", qualifiedByName = "createPassport")
    Client toClient(LoanStatementRequestDto dto);

    @Mapping(target = "gender", source = "finishRegistrationRequest.gender")
    @Mapping(target = "maritalStatus", source = "finishRegistrationRequest.maritalStatus")
    @Mapping(target = "passport.issueBranch", source = "finishRegistrationRequest.passportIssueBranch")
    @Mapping(target = "passport.issueDate", source = "finishRegistrationRequest.passportIssueDate")
    @Mapping(target = "dependentAmount", source = "finishRegistrationRequest.dependentAmount")
    @Mapping(target = "accountNumber", source = "finishRegistrationRequest.accountNumber")
    @Mapping(target = "employment", source = "finishRegistrationRequest.employment", qualifiedByName = "createEmployment")
    Client updateClient(Client client, FinishRegistrationRequestDto finishRegistrationRequest);

    @Named("createPassport")
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
}