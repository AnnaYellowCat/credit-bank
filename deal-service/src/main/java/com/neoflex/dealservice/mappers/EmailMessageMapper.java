package com.neoflex.dealservice.mappers;

import com.neoflex.dealservice.dto.EmailMessage;
import org.mapstruct.Mapper;
import org.mapstruct.Named;

import java.util.Map;

@Mapper(componentModel = "spring")
public interface EmailMessageMapper {
    @Named("toMap")
    default Map<String, Object> toMap(EmailMessage message) {
        return Map.of(
                "address", message.getAddress(),
                "theme", message.getTheme(),
                "statementId", message.getStatementId(),
                "text", message.getText()
        );
    }
}
