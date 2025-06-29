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
import ru.utlc.financialmanagementservice.dto.payment.PaymentCreateUpdateDto;
import ru.utlc.financialmanagementservice.dto.payment.PaymentReadDto;
import ru.utlc.financialmanagementservice.exception.PaymentNotFoundException;
import ru.utlc.financialmanagementservice.exception.PaymentUpdateException;
import ru.utlc.financialmanagementservice.exception.ValidationException;
import ru.utlc.financialmanagementservice.mapper.PaymentMapper;
import ru.utlc.financialmanagementservice.model.Payment;
import ru.utlc.financialmanagementservice.repository.PaymentRepository;
import java.math.BigDecimal;
import java.util.Objects;

@Slf4j
@Service
@RequiredArgsConstructor
public class PaymentService {

    private final PaymentRepository paymentRepository;
    private final PaymentMapper paymentMapper;
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

    public Flux<PaymentReadDto> findAll() {
        return paymentRepository.findAll()
                .map(paymentMapper::toDto);
    }

    public Mono<PaymentReadDto> findById(Long paymentId) {
        return paymentRepository.findById(paymentId)
                .switchIfEmpty(Mono.error(new PaymentNotFoundException(paymentId)))
                .map(paymentMapper::toDto);
    }

    public Flux<PaymentReadDto> findByPartnerId(Long partnerId) {
        return paymentRepository.findAllByPartnerId(partnerId)
                .switchIfEmpty(Mono.error(new PaymentNotFoundException("error.payment.partner.notFound", partnerId)))
                .map(paymentMapper::toDto);
    }

    public Mono<PaymentReadDto> create(PaymentCreateUpdateDto dto) {
        return Mono.just(dto)
                .map(paymentMapper::toEntity)
                .map(this::calculateTotalAmountIfNeeded)
                .map(payment -> {
                    payment.setUnallocatedAmount(payment.getTotalAmount());
                    payment.setPaymentStatusId(1); // 'completed'
                    return payment;
                })
                .flatMap(paymentRepository::save)
                .flatMap(savedPayment ->
                        ledgerService.createPaymentLedgerRow(paymentMapper.toDto(savedPayment))
                                .thenReturn(savedPayment)
                )
                .map(paymentMapper::toDto)
                .as(operator()::transactional);
    }

    public Mono<PaymentReadDto> update(Long paymentId, PaymentCreateUpdateDto dto) {
        return paymentRepository.findById(paymentId)
                .switchIfEmpty(Mono.error(new PaymentNotFoundException(paymentId)))
                .flatMap(existing -> validateForUpdate(existing, dto).thenReturn(existing))
                .flatMap(existing -> {
                    BigDecimal oldTotal = existing.getTotalAmount() != null
                            ? existing.getTotalAmount()
                            : BigDecimal.ZERO;

                    // Update fields from DTO
                    paymentMapper.update(existing, dto);
                    calculateTotalAmountIfNeeded(existing);

                    BigDecimal newTotal = existing.getTotalAmount();
                    BigDecimal difference = newTotal.subtract(oldTotal);

                    return paymentRepository.save(existing)
                            .flatMap(updatedPayment -> {
                                if (difference.compareTo(BigDecimal.ZERO) != 0) {
                                    return ledgerService.createPaymentAdjustmentRow(paymentMapper.toDto(updatedPayment), difference)
                                            .thenReturn(updatedPayment);
                                }
                                return Mono.just(updatedPayment);
                            });
                })
                .map(paymentMapper::toDto)
                .as(operator()::transactional);
    }

    public Mono<Boolean> delete(Long paymentId) {
        return paymentRepository.findById(paymentId)
                .flatMap(existing -> {
                    if (existing.getPaymentStatusId() == 2) return Mono.empty();
                    if (existing.getUnallocatedAmount().compareTo(existing.getTotalAmount()) < 0) {
                        return Mono.error(new PaymentUpdateException());
                    }
                    existing.setPaymentStatusId(2); // 2 = cancelled
                    return ledgerService.createPaymentReversalRow(paymentMapper.toDto(existing))
                            .then(paymentRepository.save(existing))
                            .thenReturn(true);
                })
                .defaultIfEmpty(false)
                .as(operator()::transactional);
    }

    /*
     * -----------------------
     * PARTIAL ALLOCATIONS
     * -----------------------
     * Extract single-attempt logic so concurrency can re-fetch on retry
     */

    public Mono<Void> allocateFromPayment(Long paymentId, BigDecimal allocatedAmount) {
        if (allocatedAmount.compareTo(BigDecimal.ZERO) <= 0) {
            return Mono.error(new ValidationException("error.paymentInvoice.allocation.negativeOrZero"));
        }

        // We do a 'Mono.defer' around the single-attempt method so each retry re-reads from DB:
        return Mono.defer(() -> attemptAllocateFromPayment(paymentId, allocatedAmount))
                .retryWhen(
                        Retry.max(1).filter(OptimisticLockingFailureException.class::isInstance)
                )
                .as(operator()::transactional);
    }

    /**
     * Single-attempt method that does one read -> leftover check -> save
     */
    private Mono<Void> attemptAllocateFromPayment(Long paymentId, BigDecimal allocatedAmount) {
        return paymentRepository.findById(paymentId)
                .switchIfEmpty(Mono.error(new PaymentNotFoundException(paymentId)))
                .flatMap(payment -> {
                    // Payment must not be canceled
                    if (payment.getPaymentStatusId() == 2) {
                        return Mono.error(new PaymentUpdateException("error.payment.update.cancelled"));
                    }
                    // leftover >= allocatedAmount
                    if (allocatedAmount.compareTo(payment.getUnallocatedAmount()) > 0) {
                        return Mono.error(new ValidationException("error.paymentInvoice.allocatedAmountExceedsUnallocated"));
                    }

                    // leftover -= allocatedAmount
                    payment.setUnallocatedAmount(
                            payment.getUnallocatedAmount().subtract(allocatedAmount)
                    );

                    return paymentRepository.save(payment);
                })
                .onErrorMap(OptimisticLockingFailureException.class,
                        ex -> new ValidationException("error.payment.concurrentModification"))
                .then();
    }

    public Mono<Void> deallocateToPayment(Long paymentId, BigDecimal deallocatedAmount) {
        if (deallocatedAmount.compareTo(BigDecimal.ZERO) <= 0) {
            return Mono.error(new ValidationException("error.paymentInvoice.allocatedAmount.min"));
        }

        return Mono.defer(() -> attemptDeallocateToPayment(paymentId, deallocatedAmount))
                .retryWhen(Retry.max(1).filter(OptimisticLockingFailureException.class::isInstance))
                .as(operator()::transactional);
    }

    private Mono<Void> attemptDeallocateToPayment(Long paymentId, BigDecimal deallocatedAmount) {
        return paymentRepository.findById(paymentId)
                .switchIfEmpty(Mono.error(new PaymentNotFoundException(paymentId)))
                .flatMap(payment -> {
                    // Payment must not be canceled
                    if (payment.getPaymentStatusId() == 2) {
                        return Mono.error(new PaymentUpdateException("error.payment.update.cancelled"));
                    }

                    // leftover + deallocated <= total
                    BigDecimal newUnallocated = payment.getUnallocatedAmount().add(deallocatedAmount);
                    if (newUnallocated.compareTo(payment.getTotalAmount()) > 0) {
                        return Mono.error(new ValidationException("error.paymentInvoice.deallocateExceedsTotal"));
                    }

                    payment.setUnallocatedAmount(newUnallocated);
                    return paymentRepository.save(payment);
                })
                .onErrorMap(OptimisticLockingFailureException.class,
                        ex -> new ValidationException("error.payment.concurrentModification"))
                .then();
    }

    /*
     * --------------------------
     * UTILITY & VALIDATIONS
     * --------------------------
     */
    private Payment calculateTotalAmountIfNeeded(Payment payment) {
        payment.calculateTotalAmount();
        return payment;
    }

    public Mono<Void> validateForUpdate(Payment existing, PaymentCreateUpdateDto dto) {
        // If payment is cancelled => no updates
        if (existing.getPaymentStatusId() == 2) {
            return Mono.error(new PaymentUpdateException("error.payment.update.cancelled"));
        }

        // If partner, currency, or date changed => error
        boolean currencyChanged = !Objects.equals(existing.getCurrencyId(), dto.currencyId());
        boolean partnerChanged = !Objects.equals(existing.getPartnerId(), dto.partnerId());
        boolean paymentDateChanged = !existing.getPaymentDate().equals(dto.paymentDate());

        if (currencyChanged || partnerChanged || paymentDateChanged) {
            return Mono.error(new PaymentUpdateException("error.payment.update.partnerOrCurrencyOrDateChanged"));
        }

        // If there is already an allocation => no modifications
        if (existing.getUnallocatedAmount().compareTo(existing.getTotalAmount()) < 0) {
            return Mono.error(new PaymentUpdateException("error.payment.update.hasAllocation"));
        }

        return Mono.empty();
    }
}
