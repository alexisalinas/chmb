package com.banamex.cops.app.service.impl;

import com.banamex.cops.app.model.CopOfferKafkaData;
import com.banamex.cops.app.repository.CopOfferKafkaRepository;
import com.banamex.cops.app.service.CopOfferKafkaService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@RequiredArgsConstructor
public class CopOfferKafkaServiceImpl implements CopOfferKafkaService {

    private final CopOfferKafkaRepository repository;

    @Override
    public List<CopOfferKafkaData> findByWaveKeyAndOfferKey(String waveKey, String offerKey) {
        return repository.findByWaveKeyAndOfferKey(waveKey, offerKey);
    }
}
