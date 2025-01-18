package ru.utlc.financialmanagementservice.integration;

import jakarta.validation.ConstraintViolationException;
import lombok.RequiredArgsConstructor;
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
import ru.utlc.financialmanagementservice.dto.invoice.InvoiceReadDto;
import ru.utlc.financialmanagementservice.dto.payment.PaymentCreateUpdateDto;
import ru.utlc.financialmanagementservice.dto.payment.PaymentReadDto;
import ru.utlc.financialmanagementservice.exception.InvoiceNotFoundException;
import ru.utlc.financialmanagementservice.exception.PaymentNotFoundException;
import ru.utlc.financialmanagementservice.service.ClientBalanceService;
import ru.utlc.financialmanagementservice.service.PaymentService;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;

@TestExecutionListeners(
        listeners = {
                DependencyInjectionTestExecutionListener.class,
                // Add other listeners here if needed
        },
        mergeMode = TestExecutionListeners.MergeMode.REPLACE_DEFAULTS
)
@ExtendWith(SpringExtension.class)
@Testcontainers
@ActiveProfiles("test")
@SpringBootTest
@RequiredArgsConstructor
class PaymentServiceIT extends IntegrationTestBase {

    @Autowired
    DatabaseClient databaseClient;

    @Autowired
    private PaymentService paymentService;

    @Autowired
    private ClientBalanceService clientBalanceService;

    final static Integer CLIENT_ID = 1;
    final static Integer CURRENCY_ID = 1;
    final static Integer PAYMENT_TYPE_ID = 1;
    final static BigDecimal AMOUNT = new BigDecimal("100.00");
    final static LocalDate PAYMENT_DATE = LocalDate.now();
    final static BigDecimal PROCESSING_FEES = new BigDecimal("2.00");
    final static BigDecimal TOTAL_AMOUNT = AMOUNT.subtract(PROCESSING_FEES);
    final static String COMMENTARY = "Initial Payment";

    @BeforeEach
    void resetDatabase() {
        // Truncate all tables or reset sequences
        // Example using R2DBC DatabaseClient
        databaseClient.sql("TRUNCATE TABLE payment, client_balance RESTART IDENTITY CASCADE").fetch().rowsUpdated().block();
    }

    @Test
    public void testSize() {
        // Assume the database is truncated in @BeforeEach

        // Insert 3 Payments
        PaymentCreateUpdateDto p1 = new PaymentCreateUpdateDto(
                1,  // clientId
                LocalDate.now(),
                1,  // currencyId
                new BigDecimal("10.00"),
                BigDecimal.ZERO,
                1,  // paymentTypeId
                null
        );

        PaymentCreateUpdateDto p2 = new PaymentCreateUpdateDto(
                2,
                LocalDate.now().minusDays(1),
                2,
                new BigDecimal("20.00"),
                BigDecimal.ZERO,
                1,
                null
        );

        PaymentCreateUpdateDto p3 = new PaymentCreateUpdateDto(
                3,
                LocalDate.now().minusDays(2),
                3,
                new BigDecimal("30.00"),
                BigDecimal.ZERO,
                1,
                null
        );

        paymentService.create(p1).block();
        paymentService.create(p2).block();
        paymentService.create(p3).block();

        // Now check that we have 3 total
        paymentService.findAll()
                .collectList()
                .map(list -> list.size())
                .as(StepVerifier::create)
                .expectNext(3)
                .verifyComplete();
    }

    // Test the 'create' method
    @Test
    public void testCreatePayment() {
        PaymentCreateUpdateDto dto = new PaymentCreateUpdateDto(
                CLIENT_ID,
                PAYMENT_DATE,
                CURRENCY_ID,
                AMOUNT,
                PROCESSING_FEES,
                PAYMENT_TYPE_ID,
                COMMENTARY
        );

        Mono<PaymentReadDto> resultMono = paymentService.create(dto);

        StepVerifier.create(resultMono)
                .assertNext(result -> {
                    assertEquals(CLIENT_ID, result.clientId().intValue());
                    assertEquals(CURRENCY_ID, result.currencyId().intValue());
                    assertEquals(PAYMENT_TYPE_ID, result.paymentTypeId().intValue());
                    assertEquals(AMOUNT, result.amount());
                    assertEquals(PAYMENT_DATE, result.paymentDate());
                    assertEquals(PROCESSING_FEES, result.processingFees());
                    assertEquals(TOTAL_AMOUNT, result.totalAmount()); // 100 - 2 = 98
                    assertEquals(COMMENTARY, result.commentary());
                }).verifyComplete();
    }

    @Test
    public void testUpdatePaymentWithAllFields() {
        PaymentCreateUpdateDto createDto = new PaymentCreateUpdateDto(
                CLIENT_ID,
                PAYMENT_DATE,
                CURRENCY_ID,
                AMOUNT,
                PROCESSING_FEES,
                PAYMENT_TYPE_ID,
                COMMENTARY
        );

        var createdPayment = paymentService.create(createDto).block();

        // Now, update the payment
        Integer updatedClientId = 2;
        Integer updatedCurrencyId = 2;
        Integer updatedPaymentTypeId = 2;
        BigDecimal updatedAmount = new BigDecimal("150.00");
        LocalDate updatedPaymentDate = LocalDate.now().plusDays(1);
        BigDecimal updatedProcessingFees = new BigDecimal("5.00");
        BigDecimal updatedTotalAmount = updatedAmount.subtract(updatedProcessingFees);
        String updatedCommentary = "Updated Payment";

        PaymentCreateUpdateDto updateDto = new PaymentCreateUpdateDto(
                updatedClientId,
                updatedPaymentDate,
                updatedCurrencyId,
                updatedAmount,
                updatedProcessingFees,
                updatedPaymentTypeId,
                updatedCommentary
        );

        var resultMono = paymentService.update(1L, updateDto);
        StepVerifier.create(resultMono)
                .assertNext(result -> {
                    // Assert all updated fields using assertEquals
                    assertEquals(updatedClientId, result.clientId().intValue());
                    assertEquals(updatedCurrencyId, result.currencyId().intValue());
                    assertEquals(updatedPaymentTypeId, result.paymentTypeId().intValue());
                    assertEquals(updatedAmount, result.amount());
                    assertEquals(updatedPaymentDate, result.paymentDate());
                    assertEquals(updatedProcessingFees, result.processingFees());
                    assertEquals(updatedTotalAmount, result.totalAmount()); // 150 - 5 = 145
                    assertEquals(updatedCommentary, result.commentary());
                })
                .verifyComplete();
    }

    @Test
    public void testDeletePayment() {
        PaymentCreateUpdateDto dto = new PaymentCreateUpdateDto(
                CLIENT_ID,
                PAYMENT_DATE,
                CURRENCY_ID,
                AMOUNT,
                PROCESSING_FEES,
                PAYMENT_TYPE_ID,
                COMMENTARY
        );
        var createdPayment = paymentService.create(dto).block();

        // Now delete the payment
        Mono<Boolean> result = paymentService.delete(createdPayment.id());

        StepVerifier.create(result)
                .expectNextMatches(deleted -> deleted.equals(true))
                .verifyComplete();

        StepVerifier.create(paymentService.delete(createdPayment.id()))
                .expectNextMatches(deleted -> deleted.equals(false))
                .verifyComplete();
    }

    @Test
    public void testDeleteNonExistentPayment() {
        Mono<Boolean> result = paymentService.delete(999L); // Assuming 999L does not exist

        StepVerifier.create(result)
                .expectNext(false)
                .verifyComplete();
    }

    @Test
    public void testUpdateNonExistentPayment() {
        PaymentCreateUpdateDto updateDto = new PaymentCreateUpdateDto(
                CLIENT_ID,
                PAYMENT_DATE,
                CURRENCY_ID,
                AMOUNT,
                PROCESSING_FEES,
                PAYMENT_TYPE_ID,
                COMMENTARY
        );

        Mono<PaymentReadDto> resultMono = paymentService.update(999L, updateDto); // Assuming 999L does not exist

        StepVerifier.create(resultMono)
                .expectComplete() // Expects the Mono to complete without emitting a value
                .verify();
    }

    @Test
    public void testFindInvoiceById_NonExistent_ThrowsException() {
        Mono<PaymentReadDto> resultMono = paymentService.findById(999L); // Non-existing ID

        StepVerifier.create(resultMono)
                .expectErrorMatches(throwable ->
                        throwable instanceof PaymentNotFoundException
                                && throwable.getMessage().contains("error.payment.notFound"))
                .verify();
    }
}
