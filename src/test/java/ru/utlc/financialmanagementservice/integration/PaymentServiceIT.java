package ru.utlc.financialmanagementservice.integration;

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
import ru.utlc.financialmanagementservice.dto.payment.PaymentCreateUpdateDto;
import ru.utlc.financialmanagementservice.dto.payment.PaymentReadDto;
import ru.utlc.financialmanagementservice.exception.PaymentNotFoundException;
import ru.utlc.financialmanagementservice.exception.PaymentUpdateException;
import ru.utlc.financialmanagementservice.service.PaymentService;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Integration tests for PaymentService.
 */
@TestExecutionListeners(
        listeners = DependencyInjectionTestExecutionListener.class,
        mergeMode = TestExecutionListeners.MergeMode.REPLACE_DEFAULTS
)
@ExtendWith(SpringExtension.class)
@Testcontainers
@ActiveProfiles("test")
@SpringBootTest
@RequiredArgsConstructor(onConstructor_ = @Autowired)
class PaymentServiceIT extends IntegrationTestBase {

    private static final int RUB = 1;
    private static final int USD = 2;
    private static final int EUR = 3;
    private static final int CNY = 4;

    private static final Long PARTNER_ID_1 = 1L;
    private static final Long PARTNER_ID_2 = 2L;
    private static final Long PARTNER_ID_3 = 3L;

    private static final Integer PAYMENT_TYPE_ID = 1;
    private static final BigDecimal AMOUNT = new BigDecimal("100.00");
    private static final BigDecimal PROCESSING_FEES = new BigDecimal("2.00");
    private static final LocalDate PAYMENT_DATE = LocalDate.now();
    private static final String COMMENTARY = "Initial Payment";

    private static final Integer PAYMENT_STATUS_COMPLETED = 1;
    private static final Integer PAYMENT_STATUS_CANCELLED = 2;
    private static final Integer PAYMENT_STATUS_REFUNDED = 3;

    @Autowired
    private DatabaseClient databaseClient;

    @Autowired
    private PaymentService paymentService;

    @BeforeEach
    void resetDatabase() {
        // Only truncate payment table; partner logic moved to a different service
        databaseClient.sql("TRUNCATE TABLE payment RESTART IDENTITY CASCADE")
                .fetch()
                .rowsUpdated()
                .block();
    }

    @Test
    void testSize() {
        var p1 = new PaymentCreateUpdateDto(
                PAYMENT_STATUS_COMPLETED,
                PARTNER_ID_1,
                LocalDate.now(),
                RUB,
                new BigDecimal("10.00"),
                BigDecimal.ZERO,
                PAYMENT_TYPE_ID,
                "Comment1"
        );
        var p2 = new PaymentCreateUpdateDto(
                PAYMENT_STATUS_COMPLETED,
                PARTNER_ID_2,
                LocalDate.now().minusDays(1),
                USD,
                new BigDecimal("20.00"),
                BigDecimal.ZERO,
                PAYMENT_TYPE_ID,
                "Comment2"
        );
        var p3 = new PaymentCreateUpdateDto(
                PAYMENT_STATUS_COMPLETED,
                PARTNER_ID_3,
                LocalDate.now().minusDays(2),
                EUR,
                new BigDecimal("30.00"),
                BigDecimal.ZERO,
                PAYMENT_TYPE_ID,
                "Comment3"
        );

        paymentService.create(p1).block();
        paymentService.create(p2).block();
        paymentService.create(p3).block();

        StepVerifier.create(paymentService.findAll().collectList().map(List::size))
                .expectNext(3)
                .verifyComplete();
    }

    @Test
    void testCreatePayment() {
        var createDto = new PaymentCreateUpdateDto(
                PAYMENT_STATUS_COMPLETED,
                PARTNER_ID_1,
                PAYMENT_DATE,
                RUB,
                AMOUNT,
                PROCESSING_FEES,
                PAYMENT_TYPE_ID,
                COMMENTARY
        );

        StepVerifier.create(paymentService.create(createDto))
                .assertNext(payment -> {
                    assertEquals(PARTNER_ID_1, payment.partnerId());
                    assertEquals(RUB, payment.currencyId());
                    assertEquals(PAYMENT_TYPE_ID, payment.paymentTypeId());
                    assertEquals(AMOUNT, payment.amount());
                    assertEquals(PROCESSING_FEES, payment.processingFees());
                    assertEquals(AMOUNT.subtract(PROCESSING_FEES), payment.totalAmount());
                    assertEquals(PAYMENT_DATE, payment.paymentDate());
                    assertEquals(AMOUNT.subtract(PROCESSING_FEES), payment.unallocatedAmount());
                    assertFalse(payment.isFullyAllocated());
                    assertEquals(COMMENTARY, payment.commentary());
                })
                .verifyComplete();
    }

    @Test
    void testUpdatePayment_AllowedFieldsOnly_Succeeds() {
        var createDto = new PaymentCreateUpdateDto(
                PAYMENT_STATUS_COMPLETED,
                PARTNER_ID_1,
                PAYMENT_DATE,
                RUB,
                AMOUNT,
                PROCESSING_FEES,
                PAYMENT_TYPE_ID,
                COMMENTARY
        );
        PaymentReadDto created = paymentService.create(createDto).block();
        assertNotNull(created);

        var updatedAmount = new BigDecimal("150.00");
        var updatedFees = new BigDecimal("5.00");
        var expectedTotal = updatedAmount.subtract(updatedFees);
        var updatedType = 2;
        var updatedComment = "New commentary - allowed";

        var updateDto = new PaymentCreateUpdateDto(
                PAYMENT_STATUS_COMPLETED,
                created.partnerId(),
                created.paymentDate(),
                created.currencyId(),
                updatedAmount,
                updatedFees,
                updatedType,
                updatedComment
        );

        StepVerifier.create(paymentService.update(created.id(), updateDto))
                .assertNext(updated -> {
                    assertEquals(updatedAmount, updated.amount());
                    assertEquals(updatedFees, updated.processingFees());
                    assertEquals(expectedTotal, updated.totalAmount());
                    assertEquals(updatedType, updated.paymentTypeId());
                    assertEquals(updatedComment, updated.commentary());
                    assertEquals(PAYMENT_STATUS_COMPLETED, updated.paymentStatusId());
                    assertEquals(PARTNER_ID_1, updated.partnerId());
                    assertEquals(RUB, updated.currencyId());
                    assertEquals(PAYMENT_DATE, updated.paymentDate());
                })
                .verifyComplete();
    }

    @Test
    void testDeletePayment() {
        var createDto = new PaymentCreateUpdateDto(
                PAYMENT_STATUS_COMPLETED,
                PARTNER_ID_1,
                PAYMENT_DATE,
                RUB,
                AMOUNT,
                PROCESSING_FEES,
                PAYMENT_TYPE_ID,
                COMMENTARY
        );
        PaymentReadDto created = paymentService.create(createDto).block();
        assertNotNull(created);

        StepVerifier.create(paymentService.delete(created.id()))
                .expectNext(true)
                .verifyComplete();

        StepVerifier.create(paymentService.delete(created.id()))
                .expectNext(false)
                .verifyComplete();
    }

    @Test
    void testDeleteNonExistentPayment() {
        StepVerifier.create(paymentService.delete(999L))
                .expectNext(false)
                .verifyComplete();
    }

    @Test
    void testUpdateNonExistentPayment() {
        var updateDto = new PaymentCreateUpdateDto(
                PAYMENT_STATUS_COMPLETED,
                PARTNER_ID_1,
                PAYMENT_DATE,
                RUB,
                AMOUNT,
                PROCESSING_FEES,
                PAYMENT_TYPE_ID,
                COMMENTARY
        );
        StepVerifier.create(paymentService.update(999L, updateDto))
                .expectErrorMatches(ex -> ex instanceof PaymentNotFoundException
                        && ex.getMessage().contains("error.payment.notFound"))
                .verify();
    }

    @Test
    void testFindPaymentById_NonExistent_ThrowsException() {
        StepVerifier.create(paymentService.findById(999L))
                .expectErrorMatches(ex -> ex instanceof PaymentNotFoundException
                        && ex.getMessage().contains("error.payment.notFound"))
                .verify();
    }

    @Test
    void testUpdatePaymentBlockedFields() {
        var createDto = new PaymentCreateUpdateDto(
                PAYMENT_STATUS_COMPLETED,
                PARTNER_ID_1,
                PAYMENT_DATE,
                RUB,
                BigDecimal.valueOf(100.00),
                BigDecimal.ZERO,
                1,
                "Initial Payment"
        );
        PaymentReadDto created = paymentService.create(createDto).block();
        assertNotNull(created);

        // partner change
        var updateDto1 = new PaymentCreateUpdateDto(
                created.paymentStatusId(),
                PARTNER_ID_2,
                created.paymentDate(),
                created.currencyId(),
                created.amount(),
                created.processingFees(),
                created.paymentTypeId(),
                "Updated commentary"
        );
        StepVerifier.create(paymentService.update(created.id(), updateDto1))
                .expectErrorMatches(ex -> ex instanceof PaymentUpdateException
                        && ex.getMessage().contains("error.payment.update.partnerOrCurrencyOrDateChanged"))
                .verify();

        // currency change
        var updateDto2 = new PaymentCreateUpdateDto(
                created.paymentStatusId(),
                created.partnerId(),
                created.paymentDate(),
                USD,
                created.amount(),
                created.processingFees(),
                created.paymentTypeId(),
                "Updated commentary"
        );
        StepVerifier.create(paymentService.update(created.id(), updateDto2))
                .expectErrorMatches(ex -> ex instanceof PaymentUpdateException
                        && ex.getMessage().contains("error.payment.update.partnerOrCurrencyOrDateChanged"))
                .verify();

        // date change
        var updateDto3 = new PaymentCreateUpdateDto(
                created.paymentStatusId(),
                created.partnerId(),
                created.paymentDate().plusDays(2),
                created.currencyId(),
                created.amount(),
                created.processingFees(),
                created.paymentTypeId(),
                "Updated commentary"
        );
        StepVerifier.create(paymentService.update(created.id(), updateDto3))
                .expectErrorMatches(ex -> ex instanceof PaymentUpdateException
                        && ex.getMessage().contains("error.payment.update.partnerOrCurrencyOrDateChanged"))
                .verify();
    }

    @Test
    void testFindByPartnerId() {
        var pay1 = new PaymentCreateUpdateDto(
                PAYMENT_STATUS_COMPLETED,
                PARTNER_ID_1,
                PAYMENT_DATE,
                RUB,
                BigDecimal.valueOf(50.00),
                BigDecimal.ZERO,
                PAYMENT_TYPE_ID,
                "Payment1 for Partner1"
        );
        var pay2 = new PaymentCreateUpdateDto(
                PAYMENT_STATUS_COMPLETED,
                PARTNER_ID_2,
                PAYMENT_DATE,
                USD,
                BigDecimal.valueOf(60.00),
                BigDecimal.ZERO,
                PAYMENT_TYPE_ID,
                "Payment2 for Partner2"
        );
        var pay3 = new PaymentCreateUpdateDto(
                PAYMENT_STATUS_COMPLETED,
                PARTNER_ID_2,
                PAYMENT_DATE.minusDays(1),
                RUB,
                BigDecimal.valueOf(80.00),
                BigDecimal.ZERO,
                PAYMENT_TYPE_ID,
                "Payment3 for Partner2"
        );
        var created1 = paymentService.create(pay1).block();
        var created2 = paymentService.create(pay2).block();
        var created3 = paymentService.create(pay3).block();
        assertNotNull(created1);
        assertNotNull(created2);
        assertNotNull(created3);

        StepVerifier.create(paymentService.findByPartnerId(PARTNER_ID_2).collectList())
                .assertNext(list -> assertEquals(2, list.size()))
                .verifyComplete();

        StepVerifier.create(paymentService.findByPartnerId(PARTNER_ID_1).collectList())
                .assertNext(list -> assertEquals(1, list.size()))
                .verifyComplete();

        StepVerifier.create(paymentService.findByPartnerId(9999L).collectList())
                .expectErrorMatches(ex -> ex instanceof PaymentNotFoundException
                        && ex.getMessage().contains("error.payment.partner.notFound"))
                .verify();
    }
}
