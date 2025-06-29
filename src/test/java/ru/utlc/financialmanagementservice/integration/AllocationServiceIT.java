package ru.utlc.financialmanagementservice.integration;

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
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.junit.jupiter.SpringExtension;
import org.testcontainers.junit.jupiter.Testcontainers;
import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;
import ru.utlc.financialmanagementservice.dto.invoice.InvoiceCreateUpdateDto;
import ru.utlc.financialmanagementservice.dto.invoice.InvoiceReadDto;
import ru.utlc.financialmanagementservice.dto.payment.PaymentCreateUpdateDto;
import ru.utlc.financialmanagementservice.dto.payment.PaymentReadDto;
import ru.utlc.financialmanagementservice.exception.PartnerNotFoundException;
import ru.utlc.financialmanagementservice.model.InvoiceDirection;
import ru.utlc.financialmanagementservice.service.AllocationService;
import ru.utlc.financialmanagementservice.service.InvoiceService;
import ru.utlc.financialmanagementservice.service.PaymentService;
import ru.utlc.partner.api.dto.PartnerDto;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.mockito.ArgumentMatchers.eq;

@Slf4j
@ExtendWith(SpringExtension.class)
@Testcontainers
@ActiveProfiles("test")
@SpringBootTest
@RequiredArgsConstructor
public class AllocationServiceIT extends IntegrationTestBase {

    @Autowired
    private DatabaseClient databaseClient;

    @Autowired
    private AllocationService allocationService;

    @Autowired
    private PaymentService paymentService;

    @Autowired
    private InvoiceService invoiceService;

    @MockBean
    private PartnerService partnerService;

    private static final long NON_EXISTENT_ID = 9999L;
    private static final Long PARTNER_ID_1 = 1L;
    private static final Long PARTNER_ID_2 = 2L;
    private static final int USD = 2;

    @BeforeEach
    void setUp() {
        // truncate only payment/invoice
        databaseClient.sql("TRUNCATE TABLE payment RESTART IDENTITY CASCADE").fetch().rowsUpdated().block();
        databaseClient.sql("TRUNCATE TABLE invoice RESTART IDENTITY CASCADE").fetch().rowsUpdated().block();

        // stub partnerService lookups
        PartnerDto p1 = new PartnerDto(
                PARTNER_ID_1.intValue(),
                "Partner One",
                "Partner One LLC",
                100,
                "Addr1",
                List.of(),
                null,
                null
        );
        Mockito.when(partnerService.findById(eq(PARTNER_ID_1))).thenReturn(Mono.just(p1));
        Mockito.when(partnerService.findById(eq(PARTNER_ID_2)))
                .thenReturn(Mono.error(new PartnerNotFoundException("Partner not found: " + PARTNER_ID_2)));
        Mockito.when(partnerService.findById(eq(NON_EXISTENT_ID)))
                .thenReturn(Mono.error(new PartnerNotFoundException("Partner not found: " + NON_EXISTENT_ID)));
    }

    @Test
    void testAllocate_SameCurrency_Success() {
        PaymentReadDto payment = createPayment(PARTNER_ID_1, USD, BigDecimal.valueOf(100));
        assertNotNull(payment);

        InvoiceReadDto invoice = createInvoice(PARTNER_ID_1, USD, BigDecimal.valueOf(200));
        assertNotNull(invoice);

        StepVerifier.create(
                allocationService.allocatePaymentToInvoice(payment.id(), invoice.id(), BigDecimal.valueOf(50))
        ).verifyComplete();
    }

    @Test
    void testAllocate_PartnerServiceError_ShouldFail() {
        // Create valid payment and invoice for PARTNER_ID_1
        PaymentReadDto payment = createPayment(PARTNER_ID_1, USD, BigDecimal.valueOf(100));
        assertNotNull(payment);
        InvoiceReadDto invoice = createInvoice(PARTNER_ID_1, USD, BigDecimal.valueOf(100));
        assertNotNull(invoice);

        // Simulate PartnerService failure for existing partner
        Mockito.when(partnerService.findById(eq(PARTNER_ID_1)))
                .thenReturn(Mono.error(new PartnerNotFoundException("Partner service unavailable for: " + PARTNER_ID_1)));

        // Expect allocation to fail due to partner lookup error
        StepVerifier.create(
                        allocationService.allocatePaymentToInvoice(payment.id(), invoice.id(), BigDecimal.TEN)
                )
                .expectError(PartnerNotFoundException.class)
                .verify();
    }

    // Helper methods
    private PaymentReadDto createPayment(Long partnerId, int currencyId, BigDecimal amount) {
        PaymentCreateUpdateDto dto = new PaymentCreateUpdateDto(
                1,
                partnerId,
                LocalDate.now(),
                currencyId,
                amount,
                BigDecimal.ZERO,
                1,
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
}
