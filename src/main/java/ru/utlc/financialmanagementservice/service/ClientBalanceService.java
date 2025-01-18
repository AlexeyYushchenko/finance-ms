package ru.utlc.financialmanagementservice.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import ru.utlc.financialmanagementservice.dto.clientbalance.ClientBalanceReadDto;
import ru.utlc.financialmanagementservice.exception.ClientNotFoundException;
import ru.utlc.financialmanagementservice.mapper.ClientBalanceMapper;
import ru.utlc.financialmanagementservice.model.ClientBalance;
import ru.utlc.financialmanagementservice.model.Invoice;
import ru.utlc.financialmanagementservice.model.Payment;
import ru.utlc.financialmanagementservice.repository.ClientBalanceRepository;

import java.math.BigDecimal;

@Slf4j
@Service
@RequiredArgsConstructor
public class ClientBalanceService {

    private final ClientBalanceRepository clientBalanceRepository;
    private final ClientBalanceMapper clientBalanceMapper;

    public Flux<ClientBalanceReadDto> findAll() {
        return clientBalanceRepository.findAll()
                .map(clientBalanceMapper::toDto);
    }

    public Flux<ClientBalanceReadDto> findByClientId(Integer clientId) {
        return clientBalanceRepository.findAllByClientId(clientId)
                .switchIfEmpty(Mono.error(new ClientNotFoundException("error.client.notFound", clientId)))
                .map(clientBalanceMapper::toDto);
    }

    public Mono<ClientBalanceReadDto> findByClientIdAndCurrencyId(Integer clientId, Integer currencyId) {
        return clientBalanceRepository.findByClientIdAndCurrencyId(clientId, currencyId)
                .map(clientBalanceMapper::toDto);
    }

    // General balance adjustment method
    public Mono<Void> adjustBalance(Integer clientId, Integer currencyId, BigDecimal amount) {
        return clientBalanceRepository.findByClientIdAndCurrencyId(clientId, currencyId)
                .flatMap(existingBalance -> {
                    existingBalance.setBalance(existingBalance.getBalance().add(amount));
                    return clientBalanceRepository.save(existingBalance);
                })
                .switchIfEmpty(clientBalanceRepository.save(
                        new ClientBalance(clientId, currencyId, amount)
                ))
                .then();
    }

    public Mono<Payment> adjustBalance(Payment payment) {
        return adjustBalance(payment.getClientId(), payment.getCurrencyId(), payment.getTotalAmount())
                .thenReturn(payment);
    }

    public Mono<Invoice> adjustBalance(Invoice invoice) {
        return adjustBalance(invoice.getClientId(), invoice.getCurrencyId(), invoice.getTotalAmount().negate())
                .thenReturn(invoice);
    }

    public Mono<Payment> negateExistingPayment(Payment payment) {
        return adjustBalance(payment.getClientId(), payment.getCurrencyId(), payment.getTotalAmount().negate())
                .thenReturn(payment);
    }

    public Mono<Invoice> negateExistingInvoice(Invoice invoice) {
        return adjustBalance(invoice.getClientId(), invoice.getCurrencyId(), invoice.getTotalAmount())
                .thenReturn(invoice);
    }

    public Mono<Payment> updateBalanceForNewPayment(Payment payment) {
        log.info("ClientBalanceService.updateBalanceForNewPayment()");
        return adjustBalance(payment).thenReturn(payment);
    }

    public Mono<Invoice> updateBalanceForNewInvoice(Invoice invoice) {
        return adjustBalance(invoice).thenReturn(invoice);
    }

    public Mono<Void> adjustBalanceForPaymentDeletion(Payment payment) {
        return adjustBalance(payment.getClientId(), payment.getCurrencyId(), payment.getTotalAmount().negate());
    }
    public Mono<Void> adjustBalanceForInvoiceDeletion(Invoice invoice) {
        return adjustBalance(invoice.getClientId(), invoice.getCurrencyId(), invoice.getTotalAmount());
    }

    public Mono<Void> adjustBalanceForInvoiceDeletion(Integer clientId, Integer currencyId, BigDecimal amount) {
        // Add the invoice amount back to the balance
        return adjustBalance(clientId, currencyId, amount);
    }
}
