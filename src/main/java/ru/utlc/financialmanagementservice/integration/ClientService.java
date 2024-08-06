package ru.utlc.financialmanagementservice.integration;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Mono;
import ru.utlc.financialmanagementservice.dto.client.ClientReadDto;
import io.github.resilience4j.circuitbreaker.annotation.CircuitBreaker;
import io.github.resilience4j.retry.annotation.Retry;

import java.time.Duration;

@Slf4j
@Component
@RequiredArgsConstructor
public class ClientService {

    private final WebClient webClient;

    @CircuitBreaker(name = "clientServiceCircuitBreaker", fallbackMethod = "fallbackFindClientById")
    @Retry(name = "clientServiceRetry")
    public Mono<ClientReadDto> findClientById(Integer id, String language) {
        return webClient.get()
                .uri(uriBuilder -> uriBuilder.path("/api/v1/clients/{id}")
                        .queryParam("language", language)
                        .build(id))
                .retrieve()
                .bodyToMono(ClientReadDto.class)
                .timeout(Duration.ofSeconds(5)) // Set a reasonable timeout
                .retry(3) // Retry up to 3 times in case of failures
                .doOnError(e -> log.error("Error fetching client by id: {}", id, e));
    }

    private Mono<ClientReadDto> fallbackFindClientById(Integer id, String language, Throwable t) {
        log.error("Fallback for findClientById, id: {}, language: {}, error: {}", id, language, t.getMessage());
        return Mono.empty();
    }
}