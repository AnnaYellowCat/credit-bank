package com.neoflex.dealservice.services;

import com.neoflex.dealservice.dto.StatementDto;
import com.neoflex.dealservice.entities.Statement;
import com.neoflex.dealservice.exceptions.StatementNotFoundException;
import com.neoflex.dealservice.mappers.StatementDtoMapper;
import com.neoflex.dealservice.repositories.StatementRepository;
import jakarta.persistence.EntityNotFoundException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class AdminStatementService {
    private final StatementRepository statementRepository;
    private final StatementDtoMapper statementDtoMapper;

    public StatementDto getStatement(String statementId) {
        Statement statement = statementRepository.getReferenceById(UUID.fromString(statementId));
        try {
            return statementDtoMapper.toStatementDto(statement);
        } catch (EntityNotFoundException | NullPointerException e) {
            log.error("Statement with id {} not found", statementId);
            throw new StatementNotFoundException("Statement not found");
        }
    }

    public List<StatementDto> getStatements() {
        return statementRepository.findAll()
                .stream()
                .map(statementDtoMapper::toStatementDto)
                .collect(Collectors.toList());
    }
}
