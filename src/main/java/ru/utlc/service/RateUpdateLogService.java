package ru.utlc.service;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Mono;
import ru.utlc.repository.RateUpdateLogRepository;

import java.time.LocalDate;
/*
 * Copyright (c) 2024, ООО Ю-ТЛК МОСКВА. All rights reserved.
 * Licensed under Proprietary License.
 *
 * Author: Алексей Ющенко, ООО Ю-ТЛК МОСКВА
 * Date: 2024-08-19
 */


@Service
@RequiredArgsConstructor
public class RateUpdateLogService {

    private final RateUpdateLogRepository rateUpdateLogRepository;

    public Mono<Boolean> checkIfRatesUpdatedForToday(LocalDate date) { return rateUpdateLogRepository.existsByUpdateDate(date); }

    public Mono<Void> logSuccessfulRateUpdate(LocalDate date) {
        return rateUpdateLogRepository.saveLogForToday(date);
    }
}
