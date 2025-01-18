package ru.utlc.financialmanagementservice.service;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Mono;
import ru.utlc.financialmanagementservice.model.PaymentInvoice;
import ru.utlc.financialmanagementservice.repository.PaymentInvoiceRepository;

import java.math.BigDecimal;

@Service
@RequiredArgsConstructor
public class PaymentAllocationService {

    private final PaymentInvoiceRepository paymentInvoiceRepository;

    public Mono<Boolean> hasAllocationsForPayment(Long paymentId) {
        return paymentInvoiceRepository.existsByPaymentId(paymentId);
    }

    public Mono<Boolean> hasAllocationsForInvoice(Long invoiceId) {
        return paymentInvoiceRepository.existsByInvoiceId(invoiceId);
    }

    public Mono<BigDecimal> getTotalAllocatedForPayment(Long paymentId) {
        return paymentInvoiceRepository.findAllByPaymentId(paymentId)
                .map(PaymentInvoice::getAllocatedAmount)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
    }

    public Mono<BigDecimal> getTotalAllocatedForInvoice(Long invoiceId) {
        return paymentInvoiceRepository.findAllByInvoiceId(invoiceId)
                .map(PaymentInvoice::getConvertedAmount)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
    }
}
