package ru.utlc.integration;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.r2dbc.core.DatabaseClient;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.junit.jupiter.SpringExtension;
import org.testcontainers.junit.jupiter.Testcontainers;
import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;
import ru.utlc.dto.partnerbalance.PartnerBalanceReportDto;
import ru.utlc.dto.invoice.InvoiceCreateUpdateDto;
import ru.utlc.dto.invoice.InvoiceReadDto;
import ru.utlc.dto.payment.PaymentCreateUpdateDto;
import ru.utlc.dto.payment.PaymentReadDto;
import ru.utlc.model.InvoiceDirection;
import ru.utlc.partner.api.dto.PartnerDto;
import ru.utlc.service.AllocationService;
import ru.utlc.service.PartnerBalanceService;
import ru.utlc.service.InvoiceService;
import ru.utlc.service.PaymentService;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

import static org.mockito.ArgumentMatchers.eq;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

@Slf4j
@ExtendWith(SpringExtension.class)
@Testcontainers
@ActiveProfiles("test")
@SpringBootTest
@RequiredArgsConstructor
@DirtiesContext
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
    private PartnerBalanceService partnerBalanceService;

    @MockBean
    private PartnerService partnerService;

    private static final long PARTNER_ID = 1L;
    private static final int RUB = 1;
    private static final int USD = 2;
    private static final int PAYMENT_STATUS_COMPLETED = 1;
    private static final int PAYMENT_TYPE_ID = 1;
    private static final int SERVICE_TYPE_ID = 1;
    private static final int INVOICE_STATUS_ID = 1;

    @BeforeEach
    void resetDatabaseAndStubPartner() {
        // Truncate FMS tables
        databaseClient.sql("TRUNCATE TABLE transaction_ledger RESTART IDENTITY CASCADE").fetch().rowsUpdated().block();
        databaseClient.sql("TRUNCATE TABLE payment RESTART IDENTITY CASCADE").fetch().rowsUpdated().block();
        databaseClient.sql("TRUNCATE TABLE invoice RESTART IDENTITY CASCADE").fetch().rowsUpdated().block();

        // Stub PartnerService
        PartnerDto stubPartner = new PartnerDto(
                (int) PARTNER_ID,
                "Test Partner",
                "Test Partner Inc.",
                100,
                "Test Address",
                List.of(),
                null,
                null
        );
        Mockito.when(partnerService.findById(eq(PARTNER_ID)))
                .thenReturn(Mono.just(stubPartner));
    }

    @Test
    void testRepeatAllocateDeallocateRubToUsd_100Times_ClientBalanceUnchanged() {
        // Arrange: create RUB payment and USD invoice
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
        assertNotNull(paymentRub);
        assertEquals(0, BigDecimal.valueOf(100).compareTo(paymentRub.unallocatedAmount()));

        InvoiceCreateUpdateDto invoiceDto = new InvoiceCreateUpdateDto(
                InvoiceDirection.RECEIVABLE,
                PARTNER_ID,
                SERVICE_TYPE_ID,
                BigDecimal.valueOf(1000),
                LocalDate.now(),
                LocalDate.now().plusDays(30),
                "Test Invoice in USD",
                USD,
                9999L,
                INVOICE_STATUS_ID
        );

        InvoiceReadDto invoiceUsd = invoiceService.create(invoiceDto).block();
        assertNotNull(invoiceUsd);

        // Act & Assert: repeat allocate/deallocate then compare balances
        Mono<PartnerBalanceReportDto> initial = partnerBalanceService.getPartnerBalance(PARTNER_ID, LocalDate.now());
        StepVerifier.create(
                        initial.flatMap(initReport -> {
                            BigDecimal initLeft = initReport.totalLeftoverRub();
                            BigDecimal initOut  = initReport.totalOutstandingRub();

                            Mono<Void> allocateSteps = Mono.empty();
                            for (int i = 0; i < 100; i++) {
                                allocateSteps = allocateSteps.then(
                                        allocationService.allocatePaymentToInvoice(
                                                paymentRub.id(), invoiceUsd.id(), BigDecimal.ONE
                                        )
                                );
                            }
                            Mono<Void> deallocateSteps = Mono.empty();
                            for (int i = 0; i < 100; i++) {
                                deallocateSteps = deallocateSteps.then(
                                        allocationService.deallocatePaymentFromInvoice(
                                                paymentRub.id(), invoiceUsd.id(), BigDecimal.ONE
                                        )
                                );
                            }

                            return allocateSteps
                                    .then(deallocateSteps)
                                    .then(partnerBalanceService.getPartnerBalance(PARTNER_ID, LocalDate.now()))
                                    .map(finalReport -> {
                                        BigDecimal finalLeft = finalReport.totalLeftoverRub();
                                        BigDecimal finalOut  = finalReport.totalOutstandingRub();

                                        assertEquals(0, initLeft.compareTo(finalLeft), "Leftover should be unchanged");
                                        assertEquals(0, initOut.compareTo(finalOut), "Outstanding should be unchanged");
                                        return finalReport;
                                    });
                        })
                )
                .expectNextCount(1)
                .verifyComplete();
    }
}
