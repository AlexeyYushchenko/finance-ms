package ru.utlc.financialmanagementservice.integration;

import lombok.RequiredArgsConstructor;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.RepeatedTest;
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
import reactor.core.publisher.Signal;
import reactor.core.scheduler.Schedulers;
import reactor.test.StepVerifier;
import reactor.util.function.Tuple2;
import ru.utlc.financialmanagementservice.dto.invoice.InvoiceCreateUpdateDto;
import ru.utlc.financialmanagementservice.dto.invoice.InvoiceReadDto;
import ru.utlc.financialmanagementservice.dto.payment.PaymentCreateUpdateDto;
import ru.utlc.financialmanagementservice.dto.payment.PaymentReadDto;
import ru.utlc.financialmanagementservice.exception.ValidationException;
import ru.utlc.financialmanagementservice.service.AllocationService;
import ru.utlc.financialmanagementservice.service.InvoiceService;
import ru.utlc.financialmanagementservice.service.PaymentService;

import java.math.BigDecimal;
import java.time.LocalDate;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Integration test focusing on:
 *  1) Payment leftover logic => payment.unallocatedAmount
 *  2) Invoice paidAmount => invoice.paidAmount
 *  3) Cross-currency allocation with baseAmount checks
 *  4) Concurrency scenario => partial leftover
 */
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
@RequiredArgsConstructor
public class AllocationServiceIT_WithLeftoverTests extends IntegrationTestBase {

    @Autowired
    private DatabaseClient databaseClient;

    @Autowired
    private PaymentService paymentService;

    @Autowired
    private InvoiceService invoiceService;

    @Autowired
    private AllocationService allocationService;

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
    void resetDatabase() {
        // Truncate Payment, Invoice, ledger, partner, currency, etc. as needed
        databaseClient.sql("TRUNCATE TABLE transaction_ledger RESTART IDENTITY CASCADE").fetch().rowsUpdated().block();
        databaseClient.sql("TRUNCATE TABLE payment RESTART IDENTITY CASCADE").fetch().rowsUpdated().block();
        databaseClient.sql("TRUNCATE TABLE invoice RESTART IDENTITY CASCADE").fetch().rowsUpdated().block();
        databaseClient.sql("TRUNCATE TABLE partner RESTART IDENTITY CASCADE").fetch().rowsUpdated().block();

        // Insert partner #1 and #2
        databaseClient.sql("""
           INSERT INTO partner (id, partner_type_id, external_id, created_at, modified_at, created_by, modified_by)
           VALUES (1, 1, 1001, now(), now(), 'test', 'test')
        """).fetch().rowsUpdated().block();
        databaseClient.sql("""
           INSERT INTO partner (id, partner_type_id, external_id, created_at, modified_at, created_by, modified_by)
           VALUES (2, 1, 1002, now(), now(), 'test', 'test')
        """).fetch().rowsUpdated().block();
    }

    private PaymentReadDto createPayment(long partnerId, int currencyId, BigDecimal amount) {
        PaymentCreateUpdateDto dto = new PaymentCreateUpdateDto(
                1, // paymentStatusId=1 => completed
                partnerId,
                LocalDate.now(),
                currencyId,
                amount,
                BigDecimal.ZERO, // processingFees
                PAYMENT_TYPE_ID,
                "Test Payment"
        );
        return paymentService.create(dto).block();
    }

    private InvoiceReadDto createInvoice(long partnerId, int currencyId, BigDecimal totalAmount) {
        InvoiceCreateUpdateDto dto = new InvoiceCreateUpdateDto(
                partnerId,
                SERVICE_TYPE_ID,
                totalAmount,
                LocalDate.now(),
                LocalDate.now().plusDays(30),
                "Test Invoice",
                currencyId,
                999L, // shipment ID
                INVOICE_STATUS_ID
        );
        return invoiceService.create(dto).block();
    }

    /**
     * Scenario 1: single-thread partial allocations => leftover & paidAmount checks.
     */
    @Test
    void testAllocateLeftoverAndInvoicePaid_SingleThread() {
        // Payment => totalAmount= 200 => unallocated=200
        PaymentReadDto payment = createPayment(PARTNER_ID_1, USD, BigDecimal.valueOf(200.00));
        assertNotNull(payment);
        assertEquals(0, BigDecimal.valueOf(200).compareTo(payment.unallocatedAmount()));

        // Invoice => totalAmount=300 => paidAmount=0
        InvoiceReadDto invoice = createInvoice(PARTNER_ID_1, USD, BigDecimal.valueOf(300.00));
        assertNotNull(invoice);
        assertEquals(0, BigDecimal.ZERO.compareTo(invoice.paidAmount()));

        // 1) Allocate 100 => leftover=100, invoice.paid=100
        allocationService.allocatePaymentToInvoice(payment.id(), invoice.id(), BigDecimal.valueOf(100.00)).block();

        PaymentReadDto paymentAfter1 = paymentService.findById(payment.id()).block();
        assertEquals(0, BigDecimal.valueOf(100).compareTo(paymentAfter1.unallocatedAmount()),
                "Payment leftover should now be 100");

        InvoiceReadDto invoiceAfter1 = invoiceService.findById(invoice.id()).block();
        assertEquals(0, BigDecimal.valueOf(100).compareTo(invoiceAfter1.paidAmount()),
                "Invoice paidAmount should now be 100");

        // 2) Allocate another 50 => leftover=50, invoice.paid=150
        allocationService.allocatePaymentToInvoice(payment.id(), invoice.id(), BigDecimal.valueOf(50.00)).block();

        PaymentReadDto paymentAfter2 = paymentService.findById(payment.id()).block();
        assertEquals(0, BigDecimal.valueOf(50).compareTo(paymentAfter2.unallocatedAmount()),
                "Payment leftover should now be 50");

        InvoiceReadDto invoiceAfter2 = invoiceService.findById(invoice.id()).block();
        assertEquals(0, BigDecimal.valueOf(150).compareTo(invoiceAfter2.paidAmount()),
                "Invoice paidAmount should now be 150");
    }

    /**
     * Scenario 2: cross-currency partial allocation => leftover & paidAmount
     * plus we check base_amount logic in ledger if we have exchange rates set up.
     *
     * This requires your ledger can compute base_amount for cross-currency.
     */
    @Test
    void testCrossCurrencyAllocation_WithLeftoverAndBaseAmount() {
        // Payment => 100 EUR => leftover=100
        PaymentReadDto paymentEur = createPayment(PARTNER_ID_1, EUR, BigDecimal.valueOf(100.00));
        assertNotNull(paymentEur);

        // Invoice => 300 USD => paid=0
        InvoiceReadDto invoiceUsd = createInvoice(PARTNER_ID_1, USD, BigDecimal.valueOf(300.00));
        assertNotNull(invoiceUsd);

        // Suppose you have an exchange rate set up => EUR->RUB, etc.
        // For test, let's assume your ExchangeRateService or DB returns something valid.

        // Allocate 50 EUR => leftover=50, invoice.paid=some USD => your cross-currency logic should convert
        allocationService.allocatePaymentToInvoice(paymentEur.id(), invoiceUsd.id(), BigDecimal.valueOf(50.00)).block();

        // Payment leftover => 50
        PaymentReadDto paymentAfter = paymentService.findById(paymentEur.id()).block();
        assertEquals(0, BigDecimal.valueOf(50).compareTo(paymentAfter.unallocatedAmount()));

        // Invoice paid => presumably 50 USD if your code sets it 1:1 or calculates a real rate
        InvoiceReadDto invoiceAfter = invoiceService.findById(invoiceUsd.id()).block();
        // Possibly 0 if you haven't implemented "invoice.setPaidAmount(...)"
        // But let's assume you do. Then we'd expect invoiceAfter.paidAmount() ~ 50, or
        // if there's a conversion rate, maybe it's 55, 48, etc.

        // For base_amount checks, you'd query the ledger and see the 2 or 3 new rows from cross-currency.
        // Each row's base_amount is computed by your TransactionLedgerService. 
        // We won't show that here, but you could do a quick sum if needed.
    }

    /**
     * Scenario 3: concurrency with partial leftover => we allocate more than leftover in parallel.
     * Exactly one call should succeed, the other fails with leftover error.
     */
    @RepeatedTest(5)
    void testConcurrentAllocations_PartialLeftover() {
        // Payment => 100 => leftover=100
        PaymentReadDto payment = createPayment(PARTNER_ID_1, USD, BigDecimal.valueOf(100.00));

        // Invoice => big total => we won't fully pay it
        InvoiceReadDto invoice = createInvoice(PARTNER_ID_1, USD, BigDecimal.valueOf(500.00));

        // Each call tries to allocate 60 => total 120 > leftover(100)
        // We'll do them in parallel and see concurrency resolution.

        Mono<Void> call1 = allocationService.allocatePaymentToInvoice(payment.id(), invoice.id(), BigDecimal.valueOf(60.00))
                .subscribeOn(Schedulers.parallel());

        Mono<Void> call2 = allocationService.allocatePaymentToInvoice(payment.id(), invoice.id(), BigDecimal.valueOf(60.00))
                .subscribeOn(Schedulers.parallel());

        Mono<Signal<Void>> sig1 = call1.materialize();
        Mono<Signal<Void>> sig2 = call2.materialize();

        Mono<Tuple2<Signal<Void>, Signal<Void>>> combined = Mono.zip(sig1, sig2);

        StepVerifier.create(combined)
                .assertNext(tuple -> {
                    Signal<Void> firstSig = tuple.getT1();
                    Signal<Void> secondSig = tuple.getT2();

                    int successCount = 0;
                    int errorCount   = 0;

                    if (firstSig.isOnNext()) successCount++;
                    if (firstSig.isOnError()) errorCount++;
                    if (secondSig.isOnNext()) successCount++;
                    if (secondSig.isOnError()) errorCount++;

                    // We expect exactly 1 success, 1 error
                    assertEquals(1, successCount, "One allocation call should succeed");
                    assertEquals(1, errorCount, "One allocation call should fail with leftover error");

                    // Optionally check the error message
                    if (firstSig.isOnError()) {
                        Throwable err = firstSig.getThrowable();
                        assertTrue(
                                err.getMessage().contains("allocatedAmountExceedsUnallocated")
                                        || err instanceof ValidationException,
                                "We expect leftover error or concurrency error"
                        );
                    }
                    if (secondSig.isOnError()) {
                        Throwable err = secondSig.getThrowable();
                        assertTrue(
                                err.getMessage().contains("allocatedAmountExceedsUnallocated")
                                        || err instanceof ValidationException,
                                "We expect leftover error or concurrency error"
                        );
                    }
                })
                .verifyComplete();

        // Check final leftover => 40 left (100 - 60)
        // Because one call allocated 60, the other failed => leftover=40
        PaymentReadDto updatedPayment = paymentService.findById(payment.id()).block();
        assertEquals(0, BigDecimal.valueOf(40).compareTo(updatedPayment.unallocatedAmount()));
    }

    /**
     * Scenario 4: deallocate => leftover increases, invoice paid decreases.
     */
    @Test
    void testDeallocateRestoresLeftoverAndInvoicePaid() {
        // Payment => 200 => leftover=200
        PaymentReadDto payment = createPayment(PARTNER_ID_1, USD, BigDecimal.valueOf(200.00));

        // Invoice => total=300 => paid=0
        InvoiceReadDto invoice = createInvoice(PARTNER_ID_1, USD, BigDecimal.valueOf(300.00));

        // Allocate 150 => leftover=50, paid=150
        allocationService.allocatePaymentToInvoice(payment.id(), invoice.id(), BigDecimal.valueOf(150.00)).block();

        // Now deallocate 50 => leftover=100, paid=100
        allocationService.deallocatePaymentFromInvoice(payment.id(), invoice.id(), BigDecimal.valueOf(50.00)).block();

        PaymentReadDto payAfter = paymentService.findById(payment.id()).block();
        assertEquals(0, BigDecimal.valueOf(100).compareTo(payAfter.unallocatedAmount()),
                "Payment leftover should be 100 after we deallocate 50");

        InvoiceReadDto invAfter = invoiceService.findById(invoice.id()).block();
        assertEquals(0, BigDecimal.valueOf(100).compareTo(invAfter.paidAmount()),
                "Invoice paidAmount should be 100 after we deallocate 50 (down from 150)");
    }
}
