package ru.utlc.financialmanagementservice.integration;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.*;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.r2dbc.core.DatabaseClient;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.TestExecutionListeners;
import org.springframework.test.context.junit.jupiter.SpringExtension;
import org.springframework.test.context.support.DependencyInjectionTestExecutionListener;
import org.springframework.test.context.support.DirtiesContextTestExecutionListener;
import org.testcontainers.junit.jupiter.Testcontainers;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import reactor.core.publisher.Signal;
import reactor.core.scheduler.Schedulers;
import reactor.test.StepVerifier;
import reactor.util.function.Tuple2;
import reactor.util.function.Tuple3;
import ru.utlc.financialmanagementservice.dto.invoice.InvoiceCreateUpdateDto;
import ru.utlc.financialmanagementservice.dto.invoice.InvoiceReadDto;
import ru.utlc.financialmanagementservice.dto.payment.PaymentCreateUpdateDto;
import ru.utlc.financialmanagementservice.dto.payment.PaymentReadDto;
import ru.utlc.financialmanagementservice.dto.paymentinvoice.PaymentInvoiceCreateDto;
import ru.utlc.financialmanagementservice.dto.paymentinvoice.PaymentInvoiceReadDto;
import ru.utlc.financialmanagementservice.dto.paymentinvoice.PaymentInvoiceUpdateDto;
import ru.utlc.financialmanagementservice.exception.*;
import ru.utlc.financialmanagementservice.service.InvoiceService;
import ru.utlc.financialmanagementservice.service.PaymentInvoiceService;
import ru.utlc.financialmanagementservice.service.PaymentService;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.stream.Collectors;

@TestExecutionListeners(
        listeners = {
                DependencyInjectionTestExecutionListener.class,
                DirtiesContextTestExecutionListener.class
        },
        mergeMode = TestExecutionListeners.MergeMode.REPLACE_DEFAULTS
)
@ExtendWith(SpringExtension.class)
@Slf4j
@Testcontainers
@ActiveProfiles("test")
@SpringBootTest
@RequiredArgsConstructor
public class PaymentInvoiceServiceIT extends IntegrationTestBase {

    @Autowired
    private PaymentInvoiceService paymentInvoiceService;

    @Autowired
    private PaymentService paymentService;

    @Autowired
    private InvoiceService invoiceService;

    @Autowired
    private DatabaseClient databaseClient;

    private PaymentReadDto paymentSameCurrency;                 // USD->USD
    private PaymentReadDto paymentRub;
    private InvoiceReadDto invoiceSameCurrency;

    private PaymentReadDto paymentDifferentCurrency;            // EUR->USD
    private PaymentReadDto threeHundredPaymentDifferentCurrency;
    private InvoiceReadDto invoiceDifferentCurrency;

    private static final Integer CURRENCY_RUB = 1;
    private static final Integer CURRENCY_USD = 2;
    private static final Integer CURRENCY_EUR = 3;

    private static final Integer CLIENT_ID = 1;
    private static final Integer SERVICE_TYPE_ID = 1;
    private static final Integer PAYMENT_TYPE_ID = 1;
    private static final Integer INVOICE_STATUS_ID = 1;

    @BeforeEach
    public void setUp() {
        // Insert some exchange rates for today's date
        insertExchangeRate(CURRENCY_EUR, CURRENCY_USD, LocalDate.now(),
                new BigDecimal("1.10"), new BigDecimal("1.10"), new BigDecimal("1.10"))
                .block();
        insertExchangeRate(CURRENCY_USD, CURRENCY_EUR, LocalDate.now(),
                new BigDecimal("0.90"), new BigDecimal("0.90"), new BigDecimal("0.90"))
                .block();

        // Payment & Invoice (both USD)
        paymentSameCurrency = paymentService.create(
                new PaymentCreateUpdateDto(
                        CLIENT_ID,
                        LocalDate.now(),
                        CURRENCY_USD,
                        new BigDecimal("100.00"),
                        BigDecimal.ZERO,
                        PAYMENT_TYPE_ID,
                        null
                )
        ).block();

        invoiceSameCurrency = invoiceService.create(
                new InvoiceCreateUpdateDto(
                        CLIENT_ID,
                        SERVICE_TYPE_ID,
                        new BigDecimal("200.00"),
                        LocalDate.now(),
                        LocalDate.now().plusDays(30),
                        null,
                        CURRENCY_USD,
                        null,
                        INVOICE_STATUS_ID
                )
        ).block();

        paymentRub = paymentService.create(
                new PaymentCreateUpdateDto(
                        CLIENT_ID,
                        LocalDate.now(),
                        CURRENCY_RUB,
                        new BigDecimal("1000.00"),
                        BigDecimal.ZERO,
                        PAYMENT_TYPE_ID,
                        null
                )
        ).block();

        // Payment (EUR) -> Invoice (USD)
        paymentDifferentCurrency = paymentService.create(
                new PaymentCreateUpdateDto(
                        CLIENT_ID,
                        LocalDate.now(),
                        CURRENCY_EUR,
                        new BigDecimal("150.00"),
                        BigDecimal.ZERO,
                        PAYMENT_TYPE_ID,
                        null
                )
        ).block();

        threeHundredPaymentDifferentCurrency = paymentService.create(
                new PaymentCreateUpdateDto(
                        CLIENT_ID,
                        LocalDate.now(),
                        CURRENCY_EUR,
                        new BigDecimal("300.00"),
                        BigDecimal.ZERO,
                        PAYMENT_TYPE_ID,
                        null
                )
        ).block();

        invoiceDifferentCurrency = invoiceService.create(
                new InvoiceCreateUpdateDto(
                        CLIENT_ID,
                        SERVICE_TYPE_ID,
                        new BigDecimal("300.00"),
                        LocalDate.now(),
                        LocalDate.now().plusDays(30),
                        null,
                        CURRENCY_USD,
                        null,
                        INVOICE_STATUS_ID
                )
        ).block();
    }

    @AfterEach
    public void tearDown() {
        // Cleanup Exchange Rates
        databaseClient.sql("DELETE FROM exchange_rate").fetch().rowsUpdated().block();

        // Deallocate any allocations
        paymentInvoiceService.findAll()
                .flatMap(dto -> paymentInvoiceService.deallocatePaymentFromInvoice(dto.id()))
                .blockLast();

        // Delete created payments and invoices
        if (paymentSameCurrency != null) {
            paymentService.delete(paymentSameCurrency.id()).block();
        }
        if (invoiceSameCurrency != null) {
            invoiceService.delete(invoiceSameCurrency.id()).block();
        }

        if (paymentDifferentCurrency != null) {
            paymentService.delete(paymentDifferentCurrency.id()).block();
        }
        if (invoiceDifferentCurrency != null) {
            invoiceService.delete(invoiceDifferentCurrency.id()).block();
        }
        if (threeHundredPaymentDifferentCurrency != null) {
            paymentService.delete(threeHundredPaymentDifferentCurrency.id()).block();
        }
    }

    /*
     * ------------------------------------------------------------------
     * EXAMPLES OF EXISTING TESTS (unchanged)
     * ------------------------------------------------------------------
     */

    @Test
    public void testAllocatePaymentToInvoice_SameCurrency_Success() {
        PaymentInvoiceCreateDto createDto = PaymentInvoiceCreateDto.builder()
                .paymentId(paymentSameCurrency.id())
                .invoiceId(invoiceSameCurrency.id())
                .allocatedAmount(new BigDecimal("50.00"))
                .build();

        Mono<PaymentInvoiceReadDto> result = paymentInvoiceService.allocatePaymentToInvoice(createDto);

        StepVerifier.create(result)
                .assertNext(dto -> {
                    Assertions.assertEquals(createDto.paymentId(), dto.paymentId());
                    Assertions.assertEquals(createDto.invoiceId(), dto.invoiceId());
                    Assertions.assertEquals(createDto.allocatedAmount(), dto.allocatedAmount());
                    Assertions.assertEquals(createDto.allocatedAmount(), dto.convertedAmount());
                    Assertions.assertEquals(0, dto.exchangeRate().compareTo(BigDecimal.ONE));
                    Assertions.assertEquals(paymentSameCurrency.currencyId(), dto.currencyFromId());
                    Assertions.assertEquals(invoiceSameCurrency.currencyId(), dto.currencyToId());
                })
                .verifyComplete();
    }

    @Test
    public void testAllocatePaymentToInvoice_ConvertedAmountBelowMin() {
        PaymentInvoiceCreateDto createDto = PaymentInvoiceCreateDto.builder()
                .paymentId(paymentRub.id())
                .invoiceId(invoiceDifferentCurrency.id())
                .allocatedAmount(new BigDecimal("0.01"))
                .build();

        Mono<PaymentInvoiceReadDto> result = paymentInvoiceService.allocatePaymentToInvoice(createDto);

        StepVerifier.create(result)
                .expectErrorMatches(throwable ->
                        throwable instanceof ValidationException &&
                                throwable.getMessage().contains("validation.paymentInvoice.convertedAmount.min"))
                .verify();
    }

    @Test
    public void testUpdateAllocation_ConvertedAmountBelowMin() {
        PaymentInvoiceCreateDto createDto = PaymentInvoiceCreateDto.builder()
                .paymentId(paymentRub.id())
                .invoiceId(invoiceSameCurrency.id())
                .allocatedAmount(new BigDecimal("10.00"))
                .build();

        PaymentInvoiceReadDto allocation = paymentInvoiceService.allocatePaymentToInvoice(createDto).block();

        PaymentInvoiceUpdateDto updateDto = PaymentInvoiceUpdateDto.builder()
                .allocatedAmount(new BigDecimal("0.01"))
                .build();

        Mono<PaymentInvoiceReadDto> result = paymentInvoiceService.updateAllocation(allocation.id(), updateDto);

        StepVerifier.create(result)
                .expectErrorMatches(throwable ->
                        throwable instanceof ValidationException &&
                                throwable.getMessage().contains("validation.paymentInvoice.convertedAmount.min"))
                .verify();
    }

    @Test
    public void testAllocatePaymentToInvoice_PaymentNotFound() {
        PaymentInvoiceCreateDto createDto = PaymentInvoiceCreateDto.builder()
                .paymentId(9999999L)
                .invoiceId(invoiceSameCurrency.id())
                .allocatedAmount(new BigDecimal("10.00"))
                .build();

        Mono<PaymentInvoiceReadDto> result = paymentInvoiceService.allocatePaymentToInvoice(createDto);

        StepVerifier.create(result)
                .expectErrorMatches(throwable ->
                        throwable instanceof PaymentNotFoundException
                                && throwable.getMessage().contains("notFound"))
                .verify();
    }

    @Test
    public void testAllocatePaymentToInvoice_InvoiceNotFound() {
        PaymentInvoiceCreateDto createDto = PaymentInvoiceCreateDto.builder()
                .paymentId(paymentSameCurrency.id())
                .invoiceId(9999999L)
                .allocatedAmount(new BigDecimal("10.00"))
                .build();

        Mono<PaymentInvoiceReadDto> result = paymentInvoiceService.allocatePaymentToInvoice(createDto);

        StepVerifier.create(result)
                .expectErrorMatches(throwable ->
                        throwable instanceof InvoiceNotFoundException
                                && throwable.getMessage().contains("notFound"))
                .verify();
    }

    @Test
    public void testUpdateAllocation_AllocationNotFound() {
        PaymentInvoiceUpdateDto updateDto = PaymentInvoiceUpdateDto.builder()
                .allocatedAmount(new BigDecimal("20.00"))
                .build();

        Mono<PaymentInvoiceReadDto> result = paymentInvoiceService.updateAllocation(9999999L, updateDto);

        StepVerifier.create(result)
                .expectErrorMatches(throwable ->
                        throwable instanceof AllocationNotFoundException
                                && throwable.getMessage().contains("NotFound"))
                .verify();
    }

    @Test
    public void testDeallocatePaymentFromInvoice_Success() {
        PaymentInvoiceCreateDto createDto = PaymentInvoiceCreateDto.builder()
                .paymentId(paymentSameCurrency.id())
                .invoiceId(invoiceSameCurrency.id())
                .allocatedAmount(new BigDecimal("50.00"))
                .build();

        PaymentInvoiceReadDto allocation = paymentInvoiceService.allocatePaymentToInvoice(createDto).block();

        Mono<Void> result = paymentInvoiceService.deallocatePaymentFromInvoice(allocation.id());

        StepVerifier.create(result)
                .verifyComplete();

        Mono<PaymentInvoiceReadDto> findResult = paymentInvoiceService.findById(allocation.id());

        StepVerifier.create(findResult)
                .expectErrorMatches(throwable ->
                        throwable instanceof AllocationNotFoundException)
                .verify();
    }

    @Test
    public void testAllocatePaymentToInvoice_InvalidExchangeRate() {
        LocalDate testDate = LocalDate.of(2099, 12, 31);
        deleteExchangeRate(CURRENCY_EUR, CURRENCY_USD, testDate).block();

        PaymentReadDto paymentNoRate = paymentService.create(
                new PaymentCreateUpdateDto(
                        CLIENT_ID,
                        testDate,
                        CURRENCY_EUR,
                        new BigDecimal("150.00"),
                        BigDecimal.ZERO,
                        PAYMENT_TYPE_ID,
                        null
                )
        ).block();

        InvoiceReadDto invoiceNoRate = invoiceService.create(
                new InvoiceCreateUpdateDto(
                        CLIENT_ID,
                        SERVICE_TYPE_ID,
                        new BigDecimal("300.00"),
                        testDate,
                        testDate.plusDays(30),
                        null,
                        CURRENCY_USD,
                        null,
                        INVOICE_STATUS_ID
                )
        ).block();

        PaymentInvoiceCreateDto createDto = PaymentInvoiceCreateDto.builder()
                .paymentId(paymentNoRate.id())
                .invoiceId(invoiceNoRate.id())
                .allocatedAmount(new BigDecimal("100.00"))
                .build();

        Mono<PaymentInvoiceReadDto> result = paymentInvoiceService.allocatePaymentToInvoice(createDto);

        StepVerifier.create(result)
                .expectErrorMatches(throwable ->
                        throwable instanceof ExchangeRateRetrievalFailedException)
                .verify();
    }

    @Test
    public void testAllocatePaymentToInvoice_DifferentClients() {
        PaymentReadDto paymentForClient1 = paymentService.create(
                new PaymentCreateUpdateDto(
                        CLIENT_ID,
                        LocalDate.now(),
                        CURRENCY_USD,
                        new BigDecimal("100.00"),
                        BigDecimal.ZERO,
                        PAYMENT_TYPE_ID,
                        null
                )
        ).block();

        InvoiceReadDto invoiceForClient2 = invoiceService.create(
                new InvoiceCreateUpdateDto(
                        CLIENT_ID + 1,
                        SERVICE_TYPE_ID,
                        new BigDecimal("200.00"),
                        LocalDate.now(),
                        LocalDate.now().plusDays(30),
                        null,
                        CURRENCY_USD,
                        null,
                        INVOICE_STATUS_ID
                )
        ).block();

        PaymentInvoiceCreateDto createDto = PaymentInvoiceCreateDto.builder()
                .paymentId(paymentForClient1.id())
                .invoiceId(invoiceForClient2.id())
                .allocatedAmount(new BigDecimal("50.00"))
                .build();

        Mono<PaymentInvoiceReadDto> result = paymentInvoiceService.allocatePaymentToInvoice(createDto);

        StepVerifier.create(result)
                .expectErrorMatches(throwable ->
                        throwable instanceof ValidationException &&
                                throwable.getMessage().contains("error.paymentInvoice.paymentAndInvoiceClientMismatch"))
                .verify();

        if (paymentForClient1 != null) {
            paymentService.delete(paymentForClient1.id()).block();
        }
        if (invoiceForClient2 != null) {
            invoiceService.delete(invoiceForClient2.id()).block();
        }
    }

    @Test
    public void testFindAllByClientId() {
        Integer client1Id = CLIENT_ID;
        Integer client2Id = CLIENT_ID + 1;

        PaymentReadDto payment1Client1 = paymentService.create(
                new PaymentCreateUpdateDto(client1Id, LocalDate.now(), CURRENCY_USD, new BigDecimal("100.00"), BigDecimal.ZERO, PAYMENT_TYPE_ID, null)
        ).block();
        PaymentReadDto payment2Client1 = paymentService.create(
                new PaymentCreateUpdateDto(client1Id, LocalDate.now(), CURRENCY_EUR, new BigDecimal("200.00"), BigDecimal.ZERO, PAYMENT_TYPE_ID, null)
        ).block();

        PaymentReadDto payment1Client2 = paymentService.create(
                new PaymentCreateUpdateDto(client2Id, LocalDate.now(), CURRENCY_USD, new BigDecimal("150.00"), BigDecimal.ZERO, PAYMENT_TYPE_ID, null)
        ).block();

        InvoiceReadDto invoiceClient1 = invoiceService.create(
                new InvoiceCreateUpdateDto(client1Id, SERVICE_TYPE_ID, new BigDecimal("300.00"), LocalDate.now(), LocalDate.now().plusDays(30), null, CURRENCY_USD, null, INVOICE_STATUS_ID)
        ).block();
        InvoiceReadDto invoiceClient2 = invoiceService.create(
                new InvoiceCreateUpdateDto(client2Id, SERVICE_TYPE_ID, new BigDecimal("400.00"), LocalDate.now(), LocalDate.now().plusDays(30), null, CURRENCY_USD, null, INVOICE_STATUS_ID)
        ).block();

        // Allocate a few Payment->Invoice pairs for clients 1 and 2
        paymentInvoiceService.allocatePaymentToInvoice(
                PaymentInvoiceCreateDto.builder()
                        .paymentId(payment1Client1.id())
                        .invoiceId(invoiceClient1.id())
                        .allocatedAmount(new BigDecimal("50.00"))
                        .build()
        ).block();

        paymentInvoiceService.allocatePaymentToInvoice(
                PaymentInvoiceCreateDto.builder()
                        .paymentId(payment2Client1.id())
                        .invoiceId(invoiceClient1.id())
                        .allocatedAmount(new BigDecimal("75.00"))
                        .build()
        ).block();

        paymentInvoiceService.allocatePaymentToInvoice(
                PaymentInvoiceCreateDto.builder()
                        .paymentId(payment1Client2.id())
                        .invoiceId(invoiceClient2.id())
                        .allocatedAmount(new BigDecimal("100.00"))
                        .build()
        ).block();

        /*
         * We want to verify:
         *   - For client1, all returned PaymentInvoiceReadDto records
         *     have Payment + Invoice also referencing client1.
         *   - For client2, the same check.
         *
         * We'll do that by "zipping" each PaymentInvoiceReadDto with
         * its Payment + Invoice reactively, all within the StepVerifier.
         */

        // 1) Test for client1
        Flux<Tuple3<PaymentInvoiceReadDto, PaymentReadDto, InvoiceReadDto>> client1Flux =
                paymentInvoiceService.findAllByClientId(client1Id)
                        .flatMap(allocation ->
                                Mono.zip(
                                        Mono.just(allocation),
                                        paymentService.findById(allocation.paymentId()),
                                        invoiceService.findById(allocation.invoiceId())
                                )
                        );

        StepVerifier.create(client1Flux)
                .assertNext(tuple -> {
                    PaymentInvoiceReadDto alloc = tuple.getT1();
                    PaymentReadDto payment = tuple.getT2();
                    InvoiceReadDto invoice = tuple.getT3();

                    // Validate both Payment + Invoice belong to client1
                    Assertions.assertEquals(client1Id, payment.clientId());
                    Assertions.assertEquals(client1Id, invoice.clientId());
                })
                .assertNext(tuple -> {
                    PaymentInvoiceReadDto alloc = tuple.getT1();
                    PaymentReadDto payment = tuple.getT2();
                    InvoiceReadDto invoice = tuple.getT3();

                    Assertions.assertEquals(client1Id, payment.clientId());
                    Assertions.assertEquals(client1Id, invoice.clientId());
                })
                .verifyComplete();

        // 2) Test for client2
        Flux<Tuple3<PaymentInvoiceReadDto, PaymentReadDto, InvoiceReadDto>> client2Flux =
                paymentInvoiceService.findAllByClientId(client2Id)
                        .flatMap(allocation ->
                                Mono.zip(
                                        Mono.just(allocation),
                                        paymentService.findById(allocation.paymentId()),
                                        invoiceService.findById(allocation.invoiceId())
                                )
                        );

        StepVerifier.create(client2Flux)
                .assertNext(tuple -> {
                    PaymentInvoiceReadDto alloc = tuple.getT1();
                    PaymentReadDto payment = tuple.getT2();
                    InvoiceReadDto invoice = tuple.getT3();

                    Assertions.assertEquals(client2Id, payment.clientId());
                    Assertions.assertEquals(client2Id, invoice.clientId());
                })
                .verifyComplete();
    }

    /*
     * -----------------------------------------------
     * NEW TESTS: findAllByPaymentId(...)
     * -----------------------------------------------
     */

    @Test
    public void testFindAllByPaymentId_ReturnsOnlyTargetAllocations() {
        // 1) Create two separate payments
        PaymentReadDto targetPayment = paymentService.create(
                new PaymentCreateUpdateDto(
                        CLIENT_ID,
                        LocalDate.now(),
                        CURRENCY_USD,
                        BigDecimal.valueOf(1000.00),
                        BigDecimal.ZERO,
                        PAYMENT_TYPE_ID,
                        null
                )
        ).block();

        PaymentReadDto otherPayment = paymentService.create(
                new PaymentCreateUpdateDto(
                        CLIENT_ID,
                        LocalDate.now(),
                        CURRENCY_USD,
                        BigDecimal.valueOf(500.00),
                        BigDecimal.ZERO,
                        PAYMENT_TYPE_ID,
                        null
                )
        ).block();

        // 2) Create an invoice
        InvoiceReadDto invoice = invoiceService.create(
                new InvoiceCreateUpdateDto(
                        CLIENT_ID,
                        SERVICE_TYPE_ID,
                        BigDecimal.valueOf(2000.00),
                        LocalDate.now(),
                        LocalDate.now().plusDays(30),
                        null,
                        CURRENCY_USD,
                        null,
                        INVOICE_STATUS_ID
                )
        ).block();

        // 3) Create unique allocations (NO duplicate payment-invoice pairs)
        //    (1) targetPayment -> invoice
        paymentInvoiceService.allocatePaymentToInvoice(
                PaymentInvoiceCreateDto.builder()
                        .paymentId(targetPayment.id())
                        .invoiceId(invoice.id())
                        .allocatedAmount(new BigDecimal("100.00"))
                        .build()
        ).block();

        //    (2) otherPayment -> invoice
        paymentInvoiceService.allocatePaymentToInvoice(
                PaymentInvoiceCreateDto.builder()
                        .paymentId(otherPayment.id())
                        .invoiceId(invoice.id())
                        .allocatedAmount(new BigDecimal("70.00"))
                        .build()
        ).block();

        // 4) Act: retrieve allocations for targetPayment
        Flux<PaymentInvoiceReadDto> resultFlux = paymentInvoiceService.findAllByPaymentId(targetPayment.id());

        // 5) Assert
        StepVerifier.create(resultFlux)
                .recordWith(ArrayList::new)
                .thenConsumeWhile(dto -> true)
                .consumeRecordedWith(list -> {
                    // We expect exactly 1 allocation for the targetPayment
                    Assertions.assertEquals(1, list.size());

                    // Check the returned allocation has targetPayment
                    list.forEach(alloc ->
                            Assertions.assertEquals(targetPayment.id(), alloc.paymentId(),
                                    "Found an allocation referencing the wrong paymentId.")
                    );
                })
                .verifyComplete();
    }

    @Test
    public void testFindAllByPaymentId_NoAllocationsForPayment_ThrowsError() {
        // Payment that has no allocations
        PaymentReadDto unusedPayment = paymentService.create(
                new PaymentCreateUpdateDto(
                        CLIENT_ID,
                        LocalDate.now(),
                        CURRENCY_USD,
                        BigDecimal.valueOf(300.00),
                        BigDecimal.ZERO,
                        PAYMENT_TYPE_ID,
                        null
                )
        ).block();

        Flux<PaymentInvoiceReadDto> resultFlux = paymentInvoiceService.findAllByPaymentId(unusedPayment.id());

        StepVerifier.create(resultFlux)
                .expectErrorMatches(throwable ->
                        throwable instanceof AllocationNotFoundException
                                && throwable.getMessage().contains("error.paymentInvoice.payment.notFound"))
                .verify();
    }

    @Test
    public void testFindAllByInvoiceId_Scenarios() {
        /*
         * -----------------------------------------
         * SCENARIO 1: One client, multiple invoices & payments
         * -----------------------------------------
         */
        Integer singleClientId = CLIENT_ID; // e.g., 1
        PaymentReadDto paymentA = paymentService.create(
                new PaymentCreateUpdateDto(
                        singleClientId,
                        LocalDate.now(),
                        CURRENCY_USD,
                        BigDecimal.valueOf(1000.00),
                        BigDecimal.ZERO,
                        PAYMENT_TYPE_ID,
                        null
                )
        ).block();
        PaymentReadDto paymentB = paymentService.create(
                new PaymentCreateUpdateDto(
                        singleClientId,
                        LocalDate.now(),
                        CURRENCY_EUR,
                        BigDecimal.valueOf(2000.00),
                        BigDecimal.ZERO,
                        PAYMENT_TYPE_ID,
                        null
                )
        ).block();

        InvoiceReadDto invoiceX = invoiceService.create(
                new InvoiceCreateUpdateDto(
                        singleClientId,
                        SERVICE_TYPE_ID,
                        BigDecimal.valueOf(300.00),
                        LocalDate.now(),
                        LocalDate.now().plusDays(30),
                        null,
                        CURRENCY_USD,
                        null,
                        INVOICE_STATUS_ID
                )
        ).block();
        InvoiceReadDto invoiceY = invoiceService.create(
                new InvoiceCreateUpdateDto(
                        singleClientId,
                        SERVICE_TYPE_ID,
                        BigDecimal.valueOf(400.00),
                        LocalDate.now(),
                        LocalDate.now().plusDays(30),
                        null,
                        CURRENCY_EUR,
                        null,
                        INVOICE_STATUS_ID
                )
        ).block();

        // Allocations (all under the same client):
        // invoiceX: pA -> iX, pB -> iX
        paymentInvoiceService.allocatePaymentToInvoice(
                PaymentInvoiceCreateDto.builder()
                        .paymentId(paymentA.id())
                        .invoiceId(invoiceX.id())
                        .allocatedAmount(BigDecimal.valueOf(50.00))
                        .build()
        ).block();

        paymentInvoiceService.allocatePaymentToInvoice(
                PaymentInvoiceCreateDto.builder()
                        .paymentId(paymentB.id())
                        .invoiceId(invoiceX.id())
                        .allocatedAmount(BigDecimal.valueOf(75.00))
                        .build()
        ).block();

        // invoiceY: pA -> iY only
        paymentInvoiceService.allocatePaymentToInvoice(
                PaymentInvoiceCreateDto.builder()
                        .paymentId(paymentA.id())
                        .invoiceId(invoiceY.id())
                        .allocatedAmount(BigDecimal.valueOf(100.00))
                        .build()
        ).block();

        // Check invoiceX => 2 allocations
        Flux<PaymentInvoiceReadDto> invoiceXFlux = paymentInvoiceService.findAllByInvoiceId(invoiceX.id());
        StepVerifier.create(invoiceXFlux)
                .recordWith(ArrayList::new)
                .thenConsumeWhile(dto -> true)
                .consumeRecordedWith(list -> {
                    // Should have exactly 2
                    Assertions.assertEquals(2, list.size(), "Expect 2 allocations for invoiceX");
                    list.forEach(allocation ->
                            Assertions.assertEquals(invoiceX.id(), allocation.invoiceId(),
                                    "allocation references the wrong invoiceId!")
                    );
                })
                .verifyComplete();

        // Check invoiceY => 1 allocation
        Flux<PaymentInvoiceReadDto> invoiceYFlux = paymentInvoiceService.findAllByInvoiceId(invoiceY.id());
        StepVerifier.create(invoiceYFlux)
                .recordWith(ArrayList::new)
                .thenConsumeWhile(dto -> true)
                .consumeRecordedWith(list -> {
                    // Should have exactly 1
                    Assertions.assertEquals(1, list.size(), "Expect 1 allocation for invoiceY");
                    Assertions.assertEquals(invoiceY.id(), list.stream().toList().get(0).invoiceId(),
                            "allocation references the wrong invoiceId!");
                })
                .verifyComplete();

        /*
         * -----------------------------------------
         * SCENARIO 2: Multiple clients, each with own invoice(s) & payment(s)
         * -----------------------------------------
         */
        Integer client2 = singleClientId + 1;

        PaymentReadDto paymentClient2 = paymentService.create(
                new PaymentCreateUpdateDto(
                        client2,
                        LocalDate.now(),
                        CURRENCY_USD,
                        BigDecimal.valueOf(150.00),
                        BigDecimal.ZERO,
                        PAYMENT_TYPE_ID,
                        null
                )
        ).block();

        InvoiceReadDto invoiceClient2 = invoiceService.create(
                new InvoiceCreateUpdateDto(
                        client2,
                        SERVICE_TYPE_ID,
                        BigDecimal.valueOf(500.00),
                        LocalDate.now(),
                        LocalDate.now().plusDays(30),
                        null,
                        CURRENCY_USD,
                        null,
                        INVOICE_STATUS_ID
                )
        ).block();

        // Allocate: paymentClient2 -> invoiceClient2
        paymentInvoiceService.allocatePaymentToInvoice(
                PaymentInvoiceCreateDto.builder()
                        .paymentId(paymentClient2.id())
                        .invoiceId(invoiceClient2.id())
                        .allocatedAmount(BigDecimal.valueOf(100.00))
                        .build()
        ).block();

        // Now findAllByInvoiceId for invoiceClient2 => 1 allocation
        Flux<PaymentInvoiceReadDto> invoiceClient2Flux = paymentInvoiceService.findAllByInvoiceId(invoiceClient2.id());
        StepVerifier.create(invoiceClient2Flux)
                .recordWith(ArrayList::new)
                .thenConsumeWhile(dto -> true)
                .consumeRecordedWith(list -> {
                    // Should have exactly 1
                    Assertions.assertEquals(1, list.size(),
                            "Expect 1 allocation for invoiceClient2's invoice");
                    Assertions.assertEquals(invoiceClient2.id(), list.stream().toList().get(0).invoiceId(),
                            "allocation references the wrong invoiceId!");
                })
                .verifyComplete();
    }


    @Test
    public void testPaymentFullyUsed_AndSingleAllocationConstraint() {
        /*
         * -----------------------------------------------
         * SCENARIO A: Payment is fully used by a single allocation
         * -----------------------------------------------
         */
        // 1) Create Payment(200.00) & Invoice(200.00)
        PaymentReadDto paymentFullUse = paymentService.create(
                new PaymentCreateUpdateDto(
                        CLIENT_ID,
                        LocalDate.now(),
                        CURRENCY_USD,
                        BigDecimal.valueOf(200.00),
                        BigDecimal.ZERO,
                        PAYMENT_TYPE_ID,
                        null
                )
        ).block();

        InvoiceReadDto invoiceFullUse = invoiceService.create(
                new InvoiceCreateUpdateDto(
                        CLIENT_ID,
                        SERVICE_TYPE_ID,
                        BigDecimal.valueOf(200.00),
                        LocalDate.now(),
                        LocalDate.now().plusDays(30),
                        null,
                        CURRENCY_USD,
                        null,
                        INVOICE_STATUS_ID
                )
        ).block();

        // 2) Allocate the FULL 200.00 => Payment is exactly used
        PaymentInvoiceCreateDto fullAllocationDto = PaymentInvoiceCreateDto.builder()
                .paymentId(paymentFullUse.id())
                .invoiceId(invoiceFullUse.id())
                .allocatedAmount(BigDecimal.valueOf(200.00))
                .build();

        Mono<PaymentInvoiceReadDto> successAllocation = paymentInvoiceService.allocatePaymentToInvoice(fullAllocationDto);

        StepVerifier.create(successAllocation)
                .assertNext(allocation -> {
                    Assertions.assertEquals(paymentFullUse.id(), allocation.paymentId(),
                            "Wrong paymentId in returned allocation!");
                    Assertions.assertEquals(invoiceFullUse.id(), allocation.invoiceId(),
                            "Wrong invoiceId in returned allocation!");
                    // Check allocated is exactly 200.00
                    Assertions.assertEquals(0, BigDecimal.valueOf(200.00)
                                    .compareTo(allocation.allocatedAmount()),
                            "Allocated amount mismatch!");
                })
                .verifyComplete();

        // 3) Any further allocation from that Payment => "allocatedAmountExceedsUnallocated"
        PaymentInvoiceCreateDto secondAllocDto = PaymentInvoiceCreateDto.builder()
                .paymentId(paymentFullUse.id())
                .invoiceId(invoiceFullUse.id()) // same or different invoice => unallocated=0 anyway
                .allocatedAmount(BigDecimal.valueOf(10.00))
                .build();

        Mono<PaymentInvoiceReadDto> failAllocation = paymentInvoiceService.allocatePaymentToInvoice(secondAllocDto);

        StepVerifier.create(failAllocation)
                .expectErrorMatches(throwable ->
                        throwable instanceof ValidationException
                                && throwable.getMessage().contains("allocatedAmountExceedsUnallocated"))
                .verify();
    }

    @Test
    public void testPaymentFullyUsed_CheckUpdatedDtoValues() {
        /*
         * Scenario: Payment(200.00) & Invoice(200.00).
         * Allocate full 200 => Payment fully used, Invoice fully paid.
         * Then we check PaymentReadDto.unallocatedAmount == 0,
         * InvoiceReadDto.outstandingBalance == 0, etc.
         */

        // 1) Create Payment(200), Invoice(200)
        PaymentReadDto paymentFull = paymentService.create(
                new PaymentCreateUpdateDto(
                        CLIENT_ID,
                        LocalDate.now(),
                        CURRENCY_USD,
                        BigDecimal.valueOf(200.00),
                        BigDecimal.ZERO,
                        PAYMENT_TYPE_ID,
                        null
                )
        ).block();

        InvoiceReadDto invoiceFull = invoiceService.create(
                new InvoiceCreateUpdateDto(
                        CLIENT_ID,
                        SERVICE_TYPE_ID,
                        BigDecimal.valueOf(200.00),
                        LocalDate.now(),
                        LocalDate.now().plusDays(30),
                        null,
                        CURRENCY_USD,
                        null,
                        INVOICE_STATUS_ID
                )
        ).block();

        // 2) Allocate FULL 200 => Payment used up, Invoice fully covered
        PaymentInvoiceCreateDto fullAllocDto = PaymentInvoiceCreateDto.builder()
                .paymentId(paymentFull.id())
                .invoiceId(invoiceFull.id())
                .allocatedAmount(BigDecimal.valueOf(200.00))
                .build();

        PaymentInvoiceReadDto allocation = paymentInvoiceService.allocatePaymentToInvoice(fullAllocDto)
                .block(); // We do a .block() in test for simplicity

        Assertions.assertNotNull(allocation, "Allocation should not be null after a successful allocate.");
        Assertions.assertEquals(0, BigDecimal.valueOf(200).compareTo(allocation.allocatedAmount()),
                "Allocated amount mismatch!");

        // 3) Check Payment & Invoice after the allocation
        // We'll fetch them again from the service to verify updated fields.
        PaymentReadDto updatedPayment = paymentService.findById(paymentFull.id()).block();
        Assertions.assertNotNull(updatedPayment, "Updated payment should exist.");
        // If your domain sets 'unallocatedAmount' => 0, let's verify:
        Assertions.assertEquals(0, BigDecimal.ZERO.compareTo(updatedPayment.unallocatedAmount()),
                "Payment's unallocatedAmount should be 0 after full usage!");

        // Also check Payment isFullyAllocated => true (assuming your domain sets it):
        // If not used, comment out or adapt to your logic
        Assertions.assertTrue(Boolean.TRUE.equals(updatedPayment.isFullyAllocated()),
                "Payment should be marked isFullyAllocated after being fully used!");

        InvoiceReadDto updatedInvoice = invoiceService.findById(invoiceFull.id()).block();
        Assertions.assertNotNull(updatedInvoice, "Updated invoice should exist.");
        // If your domain sets outstandingBalance => 0, let's verify:
        Assertions.assertEquals(0, BigDecimal.ZERO.compareTo(updatedInvoice.outstandingBalance()),
                "Invoice's outstandingBalance should be 0 after being fully paid!");

        // If there's an amountPaid field, check it is 200.00
        Assertions.assertEquals(0, BigDecimal.valueOf(200).compareTo(updatedInvoice.amountPaid()),
                "Invoice's amountPaid should be 200 after full coverage!");
    }

    @Test
    public void testSmallestValidAllocation_Success() {
        /*
         * Scenario:
         *   - Payment with 10.00
         *   - Invoice with 100.00
         *   - We allocate 0.01 from the Payment
         *   => If the domain rule says "must be >= 0.01," this should SUCCEED.
         */

        // 1) Create Payment(10.00) & Invoice(100.00)
        PaymentReadDto payment = paymentService.create(
                new PaymentCreateUpdateDto(
                        CLIENT_ID,
                        LocalDate.now(),
                        CURRENCY_USD,
                        BigDecimal.valueOf(10.00),  // Enough to cover 0.01
                        BigDecimal.ZERO,
                        PAYMENT_TYPE_ID,
                        null
                )
        ).block();

        InvoiceReadDto invoice = invoiceService.create(
                new InvoiceCreateUpdateDto(
                        CLIENT_ID,
                        SERVICE_TYPE_ID,
                        BigDecimal.valueOf(100.00),
                        LocalDate.now(),
                        LocalDate.now().plusDays(30),
                        null,
                        CURRENCY_USD,
                        null,
                        INVOICE_STATUS_ID
                )
        ).block();

        // 2) Allocate exactly 0.01
        PaymentInvoiceCreateDto createDto = PaymentInvoiceCreateDto.builder()
                .paymentId(payment.id())
                .invoiceId(invoice.id())
                .allocatedAmount(BigDecimal.valueOf(0.01))
                .build();

        Mono<PaymentInvoiceReadDto> result = paymentInvoiceService.allocatePaymentToInvoice(createDto);

        // 3) We expect success with allocated=0.01
        StepVerifier.create(result)
                .assertNext(allocation -> {
                    Assertions.assertEquals(payment.id(), allocation.paymentId());
                    Assertions.assertEquals(invoice.id(), allocation.invoiceId());
                    Assertions.assertEquals(
                            0, BigDecimal.valueOf(0.01).compareTo(allocation.allocatedAmount()),
                            "Allocated amount mismatch. We expected 0.01"
                    );
                })
                .verifyComplete();
    }

    @Test
    public void testConcurrentAllocations_ExceedPaymentUnallocated() {
        /*
         * Scenario:
         *  - Payment(100.00)
         *  - Two Invoices: invoiceA & invoiceB
         *  - Each thread tries to allocate 60.00 from the same Payment => total 120.00
         *  => We expect exactly one success, one failure: "allocatedAmountExceedsUnallocated"
         *
         * Implementation detail:
         *  We'll run each allocate call on Schedulers.parallel() to push concurrency.
         *  Then we'll use Mono.zip(...) to collect results.
         *  One call should fail, the other should succeed.
         */

        // 1) Payment(100.00)
        PaymentReadDto payment = paymentService.create(
                new PaymentCreateUpdateDto(
                        CLIENT_ID,
                        LocalDate.now(),
                        CURRENCY_USD,
                        BigDecimal.valueOf(100.00),
                        BigDecimal.ZERO,
                        PAYMENT_TYPE_ID,
                        null
                )
        ).block();

        // Two separate invoices
        InvoiceReadDto invoiceA = invoiceService.create(
                new InvoiceCreateUpdateDto(
                        CLIENT_ID,
                        SERVICE_TYPE_ID,
                        BigDecimal.valueOf(200.00),
                        LocalDate.now(),
                        LocalDate.now().plusDays(30),
                        null,
                        CURRENCY_USD,
                        null,
                        INVOICE_STATUS_ID
                )
        ).block();

        InvoiceReadDto invoiceB = invoiceService.create(
                new InvoiceCreateUpdateDto(
                        CLIENT_ID,
                        SERVICE_TYPE_ID,
                        BigDecimal.valueOf(300.00),
                        LocalDate.now(),
                        LocalDate.now().plusDays(30),
                        null,
                        CURRENCY_USD,
                        null,
                        INVOICE_STATUS_ID
                )
        ).block();

        // 2) Two parallel calls, each tries to allocate 60 => total 120 > Payment(100)
        Mono<PaymentInvoiceReadDto> call1 = paymentInvoiceService.allocatePaymentToInvoice(
                PaymentInvoiceCreateDto.builder()
                        .paymentId(payment.id())
                        .invoiceId(invoiceA.id())
                        .allocatedAmount(BigDecimal.valueOf(60.00))
                        .build()
        ).subscribeOn(Schedulers.parallel());

        Mono<PaymentInvoiceReadDto> call2 = paymentInvoiceService.allocatePaymentToInvoice(
                PaymentInvoiceCreateDto.builder()
                        .paymentId(payment.id())
                        .invoiceId(invoiceB.id())
                        .allocatedAmount(BigDecimal.valueOf(60.00))
                        .build()
        ).subscribeOn(Schedulers.parallel());

        /*
         * We'll combine them with Mono.zip but each can either succeed or fail.
         * We'll materialize() them so that we can capture their signals (success/fail)
         * and verify that EXACTLY ONE is success, EXACTLY ONE is an error.
         */

        Mono<Signal<PaymentInvoiceReadDto>> sig1 = call1.materialize();
        Mono<Signal<PaymentInvoiceReadDto>> sig2 = call2.materialize();

        Mono<Tuple2<Signal<PaymentInvoiceReadDto>, Signal<PaymentInvoiceReadDto>>> combined = Mono.zip(sig1, sig2);

        /*
         * Now we expect:
         *   - 1 success, 1 error with "allocatedAmountExceedsUnallocated"
         * Because the Payment has only 100, total requested is 120 across 2 parallel calls.
         */

        StepVerifier.create(combined)
                .assertNext(tuple -> {
                    Signal<PaymentInvoiceReadDto> firstSig = tuple.getT1();
                    Signal<PaymentInvoiceReadDto> secondSig = tuple.getT2();

                    // Count how many are onNext vs onError
                    int successCount = 0;
                    int errorCount   = 0;

                    // For each signal:
                    if (firstSig.isOnNext()) successCount++;
                    else if (firstSig.isOnError()) errorCount++;

                    if (secondSig.isOnNext()) successCount++;
                    else if (secondSig.isOnError()) errorCount++;

                    // We expect exactly 1 success, 1 fail
                    Assertions.assertEquals(1, successCount,
                            "Exactly one call should succeed!");
                    Assertions.assertEquals(1, errorCount,
                            "Exactly one call should fail!");

                    // Check error message
                    if (firstSig.getThrowable() != null) {
                        Assertions.assertTrue(firstSig.getThrowable() instanceof ValidationException
                                        && firstSig.getThrowable().getMessage().contains("allocatedAmountExceedsUnallocated"),
                                "Error call should be 'allocatedAmountExceedsUnallocated'");
                    }
                    if (secondSig.getThrowable() != null) {
                        Assertions.assertTrue(secondSig.getThrowable() instanceof ValidationException
                                        && secondSig.getThrowable().getMessage().contains("allocatedAmountExceedsUnallocated"),
                                "Error call should be 'allocatedAmountExceedsUnallocated'");
                    }
                })
                .verifyComplete();

        /*
         * Note:
         *  Because concurrency in tests can be non-deterministic, there's a chance
         *  both succeed if they run strictly sequentially. Usually, though,
         *  the domain logic + DB transaction constraints + parallel threads
         *  should cause one to fail. For true concurrency testing, you’d likely
         *  need a bigger harness or DB-level upsert approach.
         */
    }


    /*
     * ---------------
     * HELPER METHODS
     * ---------------
     */

    private Mono<Void> insertExchangeRate(Integer currencyFromId, Integer currencyToId, LocalDate rateDate,
                                          BigDecimal official_rate, BigDecimal standard_rate, BigDecimal premium_client_rate) {
        String sql = "INSERT INTO exchange_rate (currency_from_id, currency_to_id, rate_date, official_rate, standard_rate, premium_client_rate) "
                + "VALUES (:currencyFromId, :currencyToId, :rateDate, :official_rate, :standardRate, :premium_client_rate)";

        return databaseClient.sql(sql)
                .bind("currencyFromId", currencyFromId)
                .bind("currencyToId", currencyToId)
                .bind("rateDate", rateDate)
                .bind("official_rate", official_rate)
                .bind("standardRate", standard_rate)
                .bind("premium_client_rate", premium_client_rate)
                .then();
    }

    private Mono<Void> deleteExchangeRate(Integer currencyFromId, Integer currencyToId, LocalDate rateDate) {
        String sql = "DELETE FROM exchange_rate WHERE currency_from_id = :currencyFromId AND currency_to_id = :currencyToId AND rate_date = :rateDate";

        return databaseClient.sql(sql)
                .bind("currencyFromId", currencyFromId)
                .bind("currencyToId", currencyToId)
                .bind("rateDate", rateDate)
                .then();
    }
}
