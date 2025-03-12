package ru.utlc.financialmanagementservice.integration;

import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.r2dbc.core.DatabaseClient;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.TestExecutionListeners;
import org.springframework.test.context.junit.jupiter.SpringExtension;
import org.springframework.test.context.support.DependencyInjectionTestExecutionListener;
import org.testcontainers.junit.jupiter.Testcontainers;
import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;
import ru.utlc.financialmanagementservice.dto.clientbalance.ClientBalanceReportDto;
import ru.utlc.financialmanagementservice.dto.invoice.InvoiceCreateUpdateDto;
import ru.utlc.financialmanagementservice.dto.invoice.InvoiceReadDto;
import ru.utlc.financialmanagementservice.dto.payment.PaymentCreateUpdateDto;
import ru.utlc.financialmanagementservice.dto.payment.PaymentReadDto;
import ru.utlc.financialmanagementservice.service.AllocationService;
import ru.utlc.financialmanagementservice.service.ClientBalanceService;
import ru.utlc.financialmanagementservice.service.InvoiceService;
import ru.utlc.financialmanagementservice.service.PaymentService;

import java.math.BigDecimal;
import java.time.LocalDate;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

/**
 * This test checks repeated cross-currency allocation/deallocation
 * to ensure the final client balance remains unchanged.
 */
@Slf4j
@TestExecutionListeners(
    listeners = {
        DependencyInjectionTestExecutionListener.class
    },
    mergeMode = TestExecutionListeners.MergeMode.REPLACE_DEFAULTS
)
@ExtendWith(SpringExtension.class)
@Testcontainers
@ActiveProfiles("test")
@SpringBootTest
public class CrossCurrencyRepeatedAllocDeallocIT extends IntegrationTestBase {

    @Autowired
    private DatabaseClient databaseClient;

    @Autowired
    private AllocationService allocationService;

    @Autowired
    private PaymentService paymentService;

    @Autowired
    private InvoiceService invoiceService;

    @Autowired
    private ClientBalanceService clientBalanceService;

    private static final long PARTNER_ID = 1L;
    private static final int RUB = 1;
    private static final int USD = 2;
    private static final int PAYMENT_STATUS_COMPLETED = 1;
    private static final int PAYMENT_TYPE_ID = 1;
    private static final int SERVICE_TYPE_ID = 1;
    private static final int INVOICE_STATUS_ID = 1;

    @BeforeEach
    void resetDatabase() {
        // Truncate Payment, Invoice, Partner, etc.
        databaseClient.sql("TRUNCATE TABLE payment RESTART IDENTITY CASCADE")
                .fetch()
                .rowsUpdated()
                .block();

        databaseClient.sql("TRUNCATE TABLE invoice RESTART IDENTITY CASCADE")
                .fetch()
                .rowsUpdated()
                .block();

        databaseClient.sql("TRUNCATE TABLE partner RESTART IDENTITY CASCADE")
                .fetch()
                .rowsUpdated()
                .block();

        // Insert partner #1
        databaseClient.sql("""
           INSERT INTO partner (id, partner_type_id, external_id, created_at, modified_at, created_by, modified_by)
           VALUES (1, 1, 1001, now(), now(), 'test', 'test')
        """)
                .fetch()
                .rowsUpdated()
                .block();
    }

    @Test
    void testRepeatAllocateDeallocateRubToUsd_100Times_ClientBalanceUnchanged() {
        // 1) Create Payment in RUB => leftover=100 (enough to allocate 1 rub 100 times)
        PaymentCreateUpdateDto paymentDto = new PaymentCreateUpdateDto(
                PAYMENT_STATUS_COMPLETED,
                PARTNER_ID,
                LocalDate.now(),
                RUB,
                BigDecimal.valueOf(100),
                BigDecimal.ZERO,
                PAYMENT_TYPE_ID,
                "Test Payment in RUB"
        );
        PaymentReadDto paymentRub = paymentService.create(paymentDto).block();
        assertNotNull(paymentRub, "Payment creation must succeed");
        assertEquals(0, BigDecimal.valueOf(100).compareTo(paymentRub.unallocatedAmount()),
                "Payment leftover should be 100 rub initially");

        // 2) Create Invoice in USD => large total so we don't max out
        InvoiceCreateUpdateDto invoiceDto = new InvoiceCreateUpdateDto(
                PARTNER_ID,
                SERVICE_TYPE_ID,
                BigDecimal.valueOf(1000),  // plenty big total
                LocalDate.now(),
                LocalDate.now().plusDays(30),
                "Test Invoice in USD",
                USD,
                9999L,
                INVOICE_STATUS_ID
        );
        InvoiceReadDto invoiceUsd = invoiceService.create(invoiceDto).block();
        assertNotNull(invoiceUsd, "Invoice creation must succeed");

        // 3) Check initial client balance in rub
        Mono<ClientBalanceReportDto> initialBalanceMono = clientBalanceService
                .getClientBalance(PARTNER_ID, LocalDate.now());

        // 4) Allocate 1 rub 100 times in a chain, then deallocate 1 rub 100 times,
        //    then get final client balance, then compare.
        StepVerifier.create(
            initialBalanceMono.flatMap(initialReport -> {
                BigDecimal initialLeftoverRub = initialReport.totalLeftoverRub();
                BigDecimal initialOutstandingRub = initialReport.totalOutstandingRub();

                // Build a Mono pipeline that runs all allocations, then all deallocations, then checks final balance
                Mono<Void> allocations = Mono.empty();
                for (int i = 0; i < 100; i++) {
                    final int idx = i;
                    allocations = allocations.then(
                       allocationService.allocatePaymentToInvoice(
                           paymentRub.id(),
                           invoiceUsd.id(),
                           BigDecimal.ONE // 1 rub each time
                       )
                    ).doOnSuccess(v -> log.info("Allocated #{} of 1 rub -> USD", idx+1));
                }

                Mono<Void> deallocations = Mono.empty();
                for (int i = 0; i < 100; i++) {
                    final int idx = i;
                    deallocations = deallocations.then(
                        allocationService.deallocatePaymentFromInvoice(
                            paymentRub.id(), 
                            invoiceUsd.id(), 
                            BigDecimal.ONE
                        )
                    ).doOnSuccess(v -> log.info("Deallocated #{} of 1 rub -> USD", idx+1));
                }

                // Combine it all: allocations -> deallocations -> final check
                return allocations
                    .then(deallocations)
                    .then(clientBalanceService.getClientBalance(PARTNER_ID, LocalDate.now()))
                    .map(finalReport -> {
                        // 5) Compare final leftover/outstanding to initial
                        BigDecimal finalLeftoverRub = finalReport.totalLeftoverRub();
                        BigDecimal finalOutstandingRub = finalReport.totalOutstandingRub();

                        log.info("Initial leftover={}  final leftover={}", initialLeftoverRub, finalLeftoverRub);
                        log.info("Initial outstanding={}  final outstanding={}", initialOutstandingRub, finalOutstandingRub);

                        // We expect the same leftover + same outstanding if the net effect was zero
                        assertEquals(0, initialLeftoverRub.compareTo(finalLeftoverRub),
                                "Leftover in rub should remain the same after repeated alloc/dealloc");
                        assertEquals(0, initialOutstandingRub.compareTo(finalOutstandingRub),
                                "Outstanding in rub should remain the same after repeated alloc/dealloc");

                        return finalReport;
                    });
            })
        ).expectNextCount(1)
         .verifyComplete();
    }
}
