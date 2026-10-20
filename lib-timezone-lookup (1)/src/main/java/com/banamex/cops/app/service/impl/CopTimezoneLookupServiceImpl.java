package com.banamex.cops.app.service.impl;

import com.banamex.cops.app.model.CopTimezoneLookup;
import com.banamex.cops.app.repository.CopTimezoneLookupRepository;
import com.banamex.cops.app.service.CopTimezoneLookupService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@RequiredArgsConstructor
public class CopTimezoneLookupServiceImpl implements CopTimezoneLookupService {

    private final CopTimezoneLookupRepository repository;

    @Override
    public List<CopTimezoneLookup> findBySystemNameAndModeAndOperationType(
            String tzSystemName, String tzMode, String tzOperationType) {
        return repository.findBySystemNameAndModeAndOperationType(
                tzSystemName, tzMode, tzOperationType);
    }
}
