package com.am.common.amcommondata.service;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.stream.Collectors;

import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.stereotype.Service;

import com.am.common.amcommondata.document.security.SecurityDocument;
import com.am.common.amcommondata.mapper.SecurityModelMapper;
import com.am.common.amcommondata.model.security.SecurityModel;
import com.am.common.amcommondata.repository.security.SecurityRepository;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class SecurityService {
    
    private final SecurityRepository securityRepository;
    private final SecurityModelMapper securityMapper;
    private final AuditService auditService;
    private final MongoTemplate mongoTemplate;

    public SecurityModel save(SecurityModel securityModel) {
        SecurityDocument document = securityMapper.toDocument(securityModel);
        auditService.updateAudit(document);
        return securityMapper.toModel(mongoTemplate.save(document));
    }

    public Optional<SecurityModel> findById(UUID id) {
        return securityRepository.findById(id.toString())
                .map(securityMapper::toModel);
    }

    public Optional<SecurityModel> findBySymbol(String symbol) {
        return securityRepository.findBySymbol(symbol).stream()
                .findFirst()
                .map(securityMapper::toModel);
    }

    public Optional<SecurityModel> findByKey(String key) {
        return securityRepository.findByKey(key).stream()
                .findFirst()
                .map(securityMapper::toModel);
    }

    public Optional<SecurityModel> findByIsin(String isin) {
        return securityRepository.findByIsin(isin).stream()
                .findFirst()
                .map(securityMapper::toModel);
    }

    public List<SecurityModel> findActiveLargeCapsByMinMarketCapAndSector(Long minMarketCap, String sector) {
        return securityRepository.findActiveLargeCapsByMinMarketCapAndSector(minMarketCap, sector)
                .stream()
                .map(securityMapper::toModel)
                .collect(Collectors.toList());
    }

    public List<SecurityModel> findAllVersionsById(UUID id) {
        return securityRepository.findAllVersionsById(id.toString())
                .stream()
                .map(securityMapper::toModel)
                .collect(Collectors.toList());
    }

    public void deleteById(UUID id) {
        securityRepository.deleteById(id.toString());
    }

    public void deleteAll() {
        securityRepository.deleteAll();
    }

    public List<SecurityModel> saveAll(List<SecurityModel> securities) {
        List<SecurityDocument> documents = securities.stream()
                .map(securityMapper::toDocument)
                .peek(auditService::updateAudit)
                .collect(Collectors.toList());
        
        List<SecurityDocument> savedDocuments = new ArrayList<>();
        for (int i = 0; i < documents.size(); i += 100) {
            int end = Math.min(i + 100, documents.size());
            List<SecurityDocument> batch = documents.subList(i, end);
            savedDocuments.addAll(mongoTemplate.insertAll(batch));
        }
        
        return savedDocuments.stream()
                .map(securityMapper::toModel)
                .collect(Collectors.toList());
    }
    
    /**
     * Find securities by a list of symbols, returning the latest version of each security based on audit creation time.
     * 
     * @param symbols List of security symbols to search for
     * @return List of SecurityModel objects, with the latest version of each security
     */
    public List<SecurityModel> findBySymbols(List<String> symbols) {
        if (symbols == null || symbols.isEmpty()) {
            return new ArrayList<>();
        }
        
        // Get all matching securities sorted by audit.createdAt in descending order
        List<SecurityDocument> allSecurities = securityRepository.findBySymbols(symbols);
        
        // Group by symbol and take the first (latest) entry for each symbol
        Map<String, SecurityDocument> latestBySymbol = new HashMap<>();
        
        for (SecurityDocument doc : allSecurities) {
            String symbol = doc.getKey().getSymbol();
            if (!latestBySymbol.containsKey(symbol)) {
                latestBySymbol.put(symbol, doc);
            }
        }
        
        // Convert documents to models
        return latestBySymbol.values().stream()
                .map(securityMapper::toModel)
                .collect(Collectors.toList());
    }
}
