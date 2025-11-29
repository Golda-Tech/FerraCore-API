package com.goldatech.paymentservice.domain.service;

import java.util.List;
import java.util.Optional;

import com.goldatech.paymentservice.domain.model.MomoConfigEntity;
import com.goldatech.paymentservice.domain.model.TelcoProvider;
import com.goldatech.paymentservice.domain.repository.MomoConfigRepository;
import org.springframework.stereotype.Service;


@Service
public class MomoConfigService {
    private final MomoConfigRepository repo;

    public MomoConfigService(MomoConfigRepository repo) {
        this.repo = repo;
    }
    public List<MomoConfigEntity> loadAllConfigs() {
        return repo.findAll();
    }

    public Optional<MomoConfigEntity> loadConfig() {
        return repo.findTopByProviderOrderByIdAsc(TelcoProvider.MTN);
    }
}
