package ru.utlc.financialmanagementservice.integration;

import io.github.resilience4j.circuitbreaker.annotation.CircuitBreaker;
import io.github.resilience4j.retry.annotation.Retry;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Mono;
import ru.utlc.partner.api.dto.*;
import ru.utlc.financialmanagementservice.exception.PartnerNotFoundException;

import java.time.Duration;

@Slf4j
@Service
@RequiredArgsConstructor
public class PartnerService {

    private static final String PARTNER_SERVICE_URL = "/api/v1/partners";
    private final WebClient webClient;

    /**
     * Fetch a PartnerDto from the Partner MS by ID.
     * Applies a circuit breaker, retry logic, and timeout.
     */
    @CircuitBreaker(name = "partnerServiceCircuitBreaker", fallbackMethod = "fallbackFindPartnerById")
    @Retry(name = "partnerServiceRetry")
    public Mono<PartnerDto> findById(Long partnerId) {
        return webClient.get()
                .uri(PARTNER_SERVICE_URL + "/{id}", partnerId)
                .retrieve()
                .bodyToMono(PartnerDto.class)
                .timeout(Duration.ofSeconds(5))
                .retry(3)
                .doOnError(e -> log.error("Error fetching partner id {}: {}", partnerId, e.getMessage()));
    }

    /**
     * Fallback when Partner MS is unavailable or returns error.
     * Throws a domain-specific exception so callers can handle "not found."
     */
    @SuppressWarnings("unused")
    private Mono<PartnerDto> fallbackFindPartnerById(Long partnerId, Throwable t) {
        log.error("PartnerService fallback for id {}: {}", partnerId, t.getMessage());
        return Mono.error(new PartnerNotFoundException("Partner not found or service unavailable: " + partnerId));
    }
}
