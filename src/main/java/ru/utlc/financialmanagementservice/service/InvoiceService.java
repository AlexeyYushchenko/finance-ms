package ru.utlc.financialmanagementservice.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.dao.OptimisticLockingFailureException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.ReactiveTransactionManager;
import org.springframework.transaction.reactive.TransactionalOperator;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import reactor.util.retry.Retry;
import ru.utlc.financialmanagementservice.dto.invoice.InvoiceCreateUpdateDto;
import ru.utlc.financialmanagementservice.dto.invoice.InvoiceReadDto;
import ru.utlc.financialmanagementservice.exception.InvoiceNotFoundException;
import ru.utlc.financialmanagementservice.exception.InvoiceUpdateException;
import ru.utlc.financialmanagementservice.exception.ValidationException;
import ru.utlc.financialmanagementservice.mapper.InvoiceMapper;
import ru.utlc.financialmanagementservice.model.Invoice;
import ru.utlc.financialmanagementservice.repository.InvoiceRepository;

import java.math.BigDecimal;

@Slf4j
@Service
@RequiredArgsConstructor
public class InvoiceService {

    private final InvoiceRepository invoiceRepository;
    private final InvoiceMapper invoiceMapper;
    private final TransactionLedgerService ledgerService;

    private final ReactiveTransactionManager txManager;
    private TransactionalOperator operator() {
        return TransactionalOperator.create(txManager);
    }

    /*
     * --------------------
     * BASIC CRUD
     * --------------------
     */
    public Flux<InvoiceReadDto> findAll() {
        return invoiceRepository.findAll().map(invoiceMapper::toDto);
    }

    public Mono<InvoiceReadDto> findById(Long invoiceId) {
        return invoiceRepository.findById(invoiceId)
                .switchIfEmpty(Mono.error(new InvoiceNotFoundException(invoiceId)))
                .map(invoiceMapper::toDto);
    }

    public Flux<InvoiceReadDto> findByPartnerId(Long partnerId) {
        return invoiceRepository.findAllByPartnerId(partnerId)
                .switchIfEmpty(Mono.error(new InvoiceNotFoundException("error.invoice.partner.notFound", partnerId)))
                .map(invoiceMapper::toDto);
    }

    public Mono<InvoiceReadDto> create(InvoiceCreateUpdateDto dto) {
        return Mono.just(dto)
                .map(invoiceMapper::toEntity)
                .flatMap(invoiceRepository::save)
                .flatMap(saved ->
                        ledgerService.createInvoiceLedgerRow(invoiceMapper.toDto(saved))
                                .thenReturn(saved)
                )
                .flatMap(saved -> findById(saved.getId()))
                .as(operator()::transactional);
    }

    public Mono<InvoiceReadDto> update(Long invoiceId, InvoiceCreateUpdateDto dto) {
        return invoiceRepository.findById(invoiceId)
                .switchIfEmpty(Mono.error(new InvoiceNotFoundException(invoiceId)))
                .flatMap(existing -> {
                    if (existing.getPaidAmount().compareTo(BigDecimal.ZERO) != 0) {
                        return Mono.error(new InvoiceUpdateException("error.invoice.update.hasPayment"));
                    }
                    return validateForUpdate(existing, dto).thenReturn(existing);
                })
                .flatMap(existing -> {
                    BigDecimal oldTotal = existing.getTotalAmount() != null
                            ? existing.getTotalAmount()
                            : BigDecimal.ZERO;

                    invoiceMapper.update(existing, dto);
                    BigDecimal newTotal = existing.getTotalAmount();
                    BigDecimal difference = newTotal.subtract(oldTotal);

                    return invoiceRepository.save(existing)
                            .flatMap(updated -> {
                                if (difference.compareTo(BigDecimal.ZERO) != 0) {
                                    return ledgerService.createInvoiceAdjustmentRow(
                                            invoiceMapper.toDto(updated),
                                            difference
                                    ).thenReturn(updated);
                                }
                                return Mono.just(updated);
                            });
                })
                .flatMap(updated -> findById(updated.getId()))
                .as(operator()::transactional);
    }

    public Mono<Boolean> delete(Long invoiceId) {
        return invoiceRepository.findById(invoiceId)
                .flatMap(existing -> {
                    if (existing.getStatusId() == 6) return Mono.empty();
                    if (existing.getPaidAmount().compareTo(BigDecimal.ZERO) != 0) {
                        return Mono.error(new InvoiceUpdateException());
                    }
                    existing.setStatusId(6); // 6 = "CANCELED"
                    return ledgerService.createInvoiceReversalRow(invoiceMapper.toDto(existing))
                            .then(invoiceRepository.save(existing))
                            .thenReturn(true);
                })
                .defaultIfEmpty(false)
                .as(operator()::transactional);
    }

    /*
     * -----------------------
     * PARTIAL PAID AMOUNTS
     * -----------------------
     * Similar concurrency approach
     */

    public Mono<Void> addToPaidAmount(Long invoiceId, BigDecimal amt) {
        if (amt.compareTo(BigDecimal.ZERO) <= 0) {
            return Mono.error(new ValidationException("error.invoice.invalidPaidAmount"));
        }

        return Mono.defer(() -> attemptAddToPaidAmount(invoiceId, amt))
                .retryWhen(Retry.max(1).filter(OptimisticLockingFailureException.class::isInstance))
                .as(operator()::transactional);
    }

    private Mono<Void> attemptAddToPaidAmount(Long invoiceId, BigDecimal amt) {
        return invoiceRepository.findById(invoiceId)
                .switchIfEmpty(Mono.error(new InvoiceNotFoundException(invoiceId)))
                .flatMap(invoice -> {
                    if (invoice.getStatusId() == 6) {
                        return Mono.error(new InvoiceUpdateException("error.invoice.update.cancelled"));
                    }
                    BigDecimal newPaid = invoice.getPaidAmount().add(amt);
                    if (newPaid.compareTo(invoice.getTotalAmount()) > 0) {
                        return Mono.error(new ValidationException("error.invoice.overPayment"));
                    }
                    invoice.setPaidAmount(newPaid);

                    return invoiceRepository.save(invoice);
                })
                .onErrorMap(OptimisticLockingFailureException.class,
                        ex -> new ValidationException("error.invoice.concurrentModification", ex))
                .then();
    }

    public Mono<Void> subtractFromPaidAmount(Long invoiceId, BigDecimal amt) {
        if (amt.compareTo(BigDecimal.ZERO) <= 0) {
            return Mono.error(new ValidationException("error.invoice.invalidPaidAmount"));
        }

        return Mono.defer(() -> attemptSubtractFromPaidAmount(invoiceId, amt))
                .retryWhen(Retry.max(1).filter(OptimisticLockingFailureException.class::isInstance))
                .as(operator()::transactional);
    }

    private Mono<Void> attemptSubtractFromPaidAmount(Long invoiceId, BigDecimal amt) {
        return invoiceRepository.findById(invoiceId)
                .switchIfEmpty(Mono.error(new InvoiceNotFoundException(invoiceId)))
                .flatMap(invoice -> {
                    if (invoice.getStatusId() == 6) {
                        return Mono.error(new InvoiceUpdateException("error.invoice.update.cancelled"));
                    }
                    BigDecimal newPaid = invoice.getPaidAmount().subtract(amt);
                    if (newPaid.compareTo(BigDecimal.ZERO) < 0) {
                        return Mono.error(new ValidationException("error.invoice.negativePaidAmount"));
                    }
                    invoice.setPaidAmount(newPaid);
                    return invoiceRepository.save(invoice);
                })
                .onErrorMap(OptimisticLockingFailureException.class,
                        ex -> new ValidationException("error.invoice.concurrentModification", ex))
                .then();
    }

    /*
     * --------------------------
     * VALIDATION
     * --------------------------
     */
    private Mono<Void> validateForUpdate(Invoice existing, InvoiceCreateUpdateDto dto) {
        if (existing.getStatusId() == 6) {
            return Mono.error(new InvoiceUpdateException("error.invoice.update.cancelled"));
        }
        boolean partnerChanged = !existing.getPartnerId().equals(dto.partnerId());
        boolean currencyChanged = !existing.getCurrencyId().equals(dto.currencyId());
        boolean issueDateChanged = !existing.getIssueDate().equals(dto.issueDate());

        if (partnerChanged || currencyChanged || issueDateChanged) {
            return Mono.error(new InvoiceUpdateException("error.invoice.update.partnerOrCurrencyOrDateChanged"));
        }

        return Mono.empty();
    }
}
