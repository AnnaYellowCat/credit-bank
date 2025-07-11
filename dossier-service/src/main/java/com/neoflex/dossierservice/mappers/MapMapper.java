package com.neoflex.dossierservice.mappers;

import com.neoflex.dossierservice.dto.EmailMessage;
import com.neoflex.dossierservice.enums.EmailMessageTheme;
import org.mapstruct.Mapper;
import org.mapstruct.Named;

import java.util.Map;
import java.util.UUID;

@Mapper(componentModel = "spring")
public interface MapMapper {
    @Named("toEmailMessage")
    default EmailMessage toEmailMessage(Map<String, Object> map) {
        return EmailMessage.builder()
                .address((String) map.get("address"))
                .theme(EmailMessageTheme.valueOf((String) map.get("theme")))
                .statementId(UUID.fromString((String) map.get("statementId")))
                .text((String) map.get("text"))
                .build();
    }
}
