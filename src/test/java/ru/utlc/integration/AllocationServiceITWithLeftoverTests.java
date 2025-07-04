package ru.utlc.integration;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.RepeatedTest;
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
import reactor.core.publisher.Signal;
import reactor.core.scheduler.Schedulers;
import reactor.test.StepVerifier;
import ru.utlc.dto.invoice.InvoiceReadDto;
import ru.utlc.dto.payment.PaymentCreateUpdateDto;
import ru.utlc.dto.payment.PaymentReadDto;
import ru.utlc.dto.invoice.InvoiceCreateUpdateDto;
import ru.utlc.exception.InvoiceNotFoundException;
import ru.utlc.exception.PaymentNotFoundException;
import ru.utlc.model.InvoiceDirection;
import ru.utlc.service.AllocationService;
import ru.utlc.service.ExchangeRateService;
import ru.utlc.service.InvoiceService;
import ru.utlc.service.PaymentService;
import ru.utlc.partner.api.dto.PartnerDto;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.mockito.ArgumentMatchers.eq;

@Slf4j
@ExtendWith(SpringExtension.class)
@Testcontainers
@ActiveProfiles("test")
@SpringBootTest
@RequiredArgsConstructor
@DirtiesContext
class AllocationServiceITWithLeftoverTests extends IntegrationTestBase {

    @Autowired
    private DatabaseClient databaseClient;

    @Autowired
    private PaymentService paymentService;

    @Autowired
    private InvoiceService invoiceService;

    @Autowired
    private AllocationService allocationService;

    @Autowired
    private ExchangeRateService exchangeRateService;

    @MockBean
    private PartnerService partnerService;

    // Example IDs
    private static final long PARTNER_ID_1 = 1L;
    private static final long PARTNER_ID_2 = 2L;

    // Currencies
    private static final int RUB = 1;
    private static final int USD = 2;
    private static final int EUR = 3;

    // Example statuses
    private static final int PAYMENT_TYPE_ID = 1;
    private static final int SERVICE_TYPE_ID = 1;
    private static final int INVOICE_STATUS_ID = 1;

    @BeforeEach
    void resetDatabaseAndStubPartner() {
        databaseClient.sql("TRUNCATE TABLE transaction_ledger RESTART IDENTITY CASCADE").fetch().rowsUpdated().block();
        databaseClient.sql("TRUNCATE TABLE payment RESTART IDENTITY CASCADE").fetch().rowsUpdated().block();
        databaseClient.sql("TRUNCATE TABLE invoice RESTART IDENTITY CASCADE").fetch().rowsUpdated().block();

        PartnerDto p1 = new PartnerDto(
                (int) PARTNER_ID_1,
                "Partner One",
                "Partner One LLC",
                100,
                "123 Main St",
                List.of(),
                null,
                null
        );
        PartnerDto p2 = new PartnerDto(
                (int) PARTNER_ID_2,
                "Partner Two",
                "Partner Two LLC",
                200,
                "456 Elm St",
                List.of(),
                null,
                null
        );
        Mockito.when(partnerService.findById(eq(PARTNER_ID_1))).thenReturn(Mono.just(p1));
        Mockito.when(partnerService.findById(eq(PARTNER_ID_2))).thenReturn(Mono.just(p2));
    }

    private PaymentReadDto createPayment(long partnerId, int currencyId, BigDecimal amount) {
        PaymentCreateUpdateDto dto = new PaymentCreateUpdateDto(
                1,
                partnerId,
                LocalDate.now(),
                currencyId,
                amount,
                BigDecimal.ZERO,
                PAYMENT_TYPE_ID,
                "Test Payment"
        );
        return paymentService.create(dto).block();
    }

    private InvoiceReadDto createInvoice(Long partnerId, int currencyId, BigDecimal total) {
        InvoiceCreateUpdateDto dto = new InvoiceCreateUpdateDto(
                // new first-arg: direction (we’re testing AR allocations here)
                InvoiceDirection.RECEIVABLE,
                partnerId,
                1,                       // serviceTypeId
                total,
                LocalDate.now(),         // issueDate
                LocalDate.now().plusDays(30), // dueDate
                "Test Invoice",          // commentary
                currencyId,
                123L,                    // shipmentId
                1                        // statusId
        );
        return invoiceService.create(dto).block();
    }

    @Test
    void testAllocateLeftoverAndInvoicePaid_SingleThread() {
        PaymentReadDto payment = createPayment(PARTNER_ID_1, USD, BigDecimal.valueOf(200.00));
        assertNotNull(payment);
        assertEquals(0, BigDecimal.valueOf(200).compareTo(payment.unallocatedAmount()));

        InvoiceReadDto invoice = createInvoice(PARTNER_ID_1, USD, BigDecimal.valueOf(300.00));
        assertNotNull(invoice);
        assertEquals(0, BigDecimal.ZERO.compareTo(invoice.paidAmount()));

        allocationService.allocatePaymentToInvoice(payment.id(), invoice.id(), BigDecimal.valueOf(100.00)).block();

        PaymentReadDto paymentAfter1 = paymentService.findById(payment.id()).block();
        assertEquals(0, BigDecimal.valueOf(100).compareTo(paymentAfter1.unallocatedAmount()));

        InvoiceReadDto invoiceAfter1 = invoiceService.findById(invoice.id()).block();
        assertEquals(0, BigDecimal.valueOf(100).compareTo(invoiceAfter1.paidAmount()));

        allocationService.allocatePaymentToInvoice(payment.id(), invoice.id(), BigDecimal.valueOf(50.00)).block();

        PaymentReadDto paymentAfter2 = paymentService.findById(payment.id()).block();
        assertEquals(0, BigDecimal.valueOf(50).compareTo(paymentAfter2.unallocatedAmount()));

        InvoiceReadDto invoiceAfter2 = invoiceService.findById(invoice.id()).block();
        assertEquals(0, BigDecimal.valueOf(150).compareTo(invoiceAfter2.paidAmount()));
    }

    @Test
    void testCrossCurrencyAllocation_WithLeftoverAndBaseAmount() {
        // Arrange: create EUR payment and USD invoice
        PaymentReadDto paymentEur = createPayment(PARTNER_ID_1, EUR, BigDecimal.valueOf(100.00));
        assertNotNull(paymentEur);

        InvoiceReadDto invoiceUsd = createInvoice(PARTNER_ID_1, USD, BigDecimal.valueOf(300.00));
        assertNotNull(invoiceUsd);

        // Act: allocate 50 EUR to USD invoice
        allocationService.allocatePaymentToInvoice(paymentEur.id(), invoiceUsd.id(), BigDecimal.valueOf(50.00)).block();

        // Assert leftover on payment
        PaymentReadDto paymentAfter = paymentService.findById(paymentEur.id()).block();
        assertEquals(0, BigDecimal.valueOf(50.00).compareTo(paymentAfter.unallocatedAmount()));

        // Compute expected paid amount via exchange service
        BigDecimal expectedPaid = exchangeRateService
                .convertAmount(EUR, USD, BigDecimal.valueOf(50.00), LocalDate.now())
                .block();
        assertNotNull(expectedPaid, "Expected paid amount should not be null");

        // Assert invoice paid amount matches conversion result
        InvoiceReadDto invoiceAfter = invoiceService.findById(invoiceUsd.id()).block();
        assertEquals(0, expectedPaid.compareTo(invoiceAfter.paidAmount()),
                () -> String.format("Expected paid %s but was %s", expectedPaid, invoiceAfter.paidAmount()));
    }

    @RepeatedTest(10)
    void testConcurrentAllocations_PartialLeftover() {
        PaymentReadDto payment = createPayment(PARTNER_ID_1, USD, BigDecimal.valueOf(100.00));
        InvoiceReadDto invoice = createInvoice(PARTNER_ID_1, USD, BigDecimal.valueOf(500.00));

        Mono<Signal<Void>> sig1 = allocationService.allocatePaymentToInvoice(payment.id(), invoice.id(), BigDecimal.valueOf(60.00))
                .subscribeOn(Schedulers.parallel())
                .materialize();

        Mono<Signal<Void>> sig2 = allocationService.allocatePaymentToInvoice(payment.id(), invoice.id(), BigDecimal.valueOf(60.00))
                .subscribeOn(Schedulers.parallel())
                .materialize();

        StepVerifier.create(Mono.zip(sig1, sig2))
                .assertNext(tuple -> {
                    Signal<Void> firstSig = tuple.getT1();
                    Signal<Void> secondSig = tuple.getT2();

                    int successCount = (firstSig.isOnComplete() ? 1 : 0) + (secondSig.isOnComplete() ? 1 : 0);
                    int errorCount = (firstSig.isOnError() ? 1 : 0) + (secondSig.isOnError() ? 1 : 0);

                    assertEquals(1, successCount, "One allocation call should succeed");
                    assertEquals(1, errorCount, "One allocation call should fail");
                })
                .verifyComplete();

        PaymentReadDto updatedPayment = paymentService.findById(payment.id()).block();
        assertEquals(0, BigDecimal.valueOf(40).compareTo(updatedPayment.unallocatedAmount()));
    }

    @Test
    void testDeallocateRestoresLeftoverAndInvoicePaid() {
        PaymentReadDto payment = createPayment(PARTNER_ID_1, USD, BigDecimal.valueOf(200.00));
        InvoiceReadDto invoice = createInvoice(PARTNER_ID_1, USD, BigDecimal.valueOf(300.00));

        allocationService.allocatePaymentToInvoice(payment.id(), invoice.id(), BigDecimal.valueOf(150.00)).block();
        allocationService.deallocatePaymentFromInvoice(payment.id(), invoice.id(), BigDecimal.valueOf(50.00)).block();

        PaymentReadDto payAfter = paymentService.findById(payment.id()).block();
        assertEquals(0, BigDecimal.valueOf(100).compareTo(payAfter.unallocatedAmount()));

        InvoiceReadDto invAfter = invoiceService.findById(invoice.id()).block();
        assertEquals(0, BigDecimal.valueOf(100).compareTo(invAfter.paidAmount()));
    }

    // Uncovered scenario: payment not found
    @Test
    void testAllocate_PaymentNotFound_ShouldFail() {
        long nonExistentPaymentId = 9999L;
        InvoiceReadDto invoice = createInvoice(PARTNER_ID_1, USD, BigDecimal.valueOf(100));

        StepVerifier.create(
                        allocationService.allocatePaymentToInvoice(nonExistentPaymentId, invoice.id(), BigDecimal.valueOf(10))
                )
                .expectError(PaymentNotFoundException.class)
                .verify();
    }

    // Uncovered scenario: invoice not found
    @Test
    void testAllocate_InvoiceNotFound_ShouldFail() {
        PaymentReadDto payment = createPayment(PARTNER_ID_1, USD, BigDecimal.valueOf(100));
        long nonExistentInvoiceId = 8888L;

        StepVerifier.create(
                        allocationService.allocatePaymentToInvoice(payment.id(), nonExistentInvoiceId, BigDecimal.valueOf(10))
                )
                .expectError(InvoiceNotFoundException.class)
                .verify();
    }
}
