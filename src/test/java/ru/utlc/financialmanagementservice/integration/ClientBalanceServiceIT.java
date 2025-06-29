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
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.junit.jupiter.SpringExtension;
import org.testcontainers.junit.jupiter.Testcontainers;
import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;
import ru.utlc.financialmanagementservice.dto.clientbalance.ClientBalanceReportDto;
import ru.utlc.financialmanagementservice.dto.invoice.InvoiceCreateUpdateDto;
import ru.utlc.financialmanagementservice.dto.invoice.InvoiceReadDto;
import ru.utlc.financialmanagementservice.dto.payment.PaymentCreateUpdateDto;
import ru.utlc.financialmanagementservice.dto.payment.PaymentReadDto;
import ru.utlc.financialmanagementservice.integration.PartnerService;
import ru.utlc.financialmanagementservice.model.InvoiceDirection;
import ru.utlc.partner.api.dto.PartnerDto;
import ru.utlc.financialmanagementservice.service.ClientBalanceService;
import ru.utlc.financialmanagementservice.service.InvoiceService;
import ru.utlc.financialmanagementservice.service.PaymentService;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.eq;

@Slf4j
@ExtendWith(SpringExtension.class)
@Testcontainers
@ActiveProfiles("test")
@SpringBootTest
@RequiredArgsConstructor
@DirtiesContext
public class ClientBalanceServiceIT extends IntegrationTestBase {

    @Autowired
    private DatabaseClient databaseClient;

    @Autowired
    private PaymentService paymentService;

    @Autowired
    private InvoiceService invoiceService;

    @Autowired
    private ClientBalanceService clientBalanceService;

    @MockBean
    private PartnerService partnerService;

    private static final Long PARTNER_ID_1 = 1L;
    private static final int RUB = 1;  // Base currency
    private static final int USD = 2;
    private static final int SERVICE_TYPE_ID = 1;
    private static final int PAYMENT_TYPE_ID = 1;
    private static final int INVOICE_STATUS_ID = 1;

    @BeforeEach
    void setupData() {
        // Reset tables
        databaseClient.sql("TRUNCATE TABLE transaction_ledger RESTART IDENTITY CASCADE").fetch().rowsUpdated().block();
        databaseClient.sql("TRUNCATE TABLE payment RESTART IDENTITY CASCADE").fetch().rowsUpdated().block();
        databaseClient.sql("TRUNCATE TABLE invoice RESTART IDENTITY CASCADE").fetch().rowsUpdated().block();

        // Stub PartnerService to satisfy client existence
        PartnerDto stubPartner = new PartnerDto(
                PARTNER_ID_1.intValue(),
                "Test Partner",
                "Test Partner Inc.",
                100,
                "Test Address",
                List.of(),
                null,
                null
        );
        Mockito.when(partnerService.findById(eq(PARTNER_ID_1)))
                .thenReturn(Mono.just(stubPartner));
    }

    private PaymentReadDto createPayment(Long partnerId, int currencyId, BigDecimal amount) {
        PaymentCreateUpdateDto dto = new PaymentCreateUpdateDto(
                1, partnerId, LocalDate.now(), currencyId,
                amount, BigDecimal.ZERO, PAYMENT_TYPE_ID, "Test Payment"
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
    void testGetClientBalance_SimpleScenario() {
        // Arrange: create payments
        PaymentReadDto paymentRub = createPayment(PARTNER_ID_1, RUB, BigDecimal.valueOf(200));
        assertNotNull(paymentRub);

        PaymentReadDto paymentUsd = createPayment(PARTNER_ID_1, USD, BigDecimal.valueOf(100));
        assertNotNull(paymentUsd);

        // Arrange: create invoices
        InvoiceReadDto invoiceRub = createInvoice(PARTNER_ID_1, RUB, BigDecimal.valueOf(300));
        assertNotNull(invoiceRub);
        invoiceService.addToPaidAmount(invoiceRub.id(), BigDecimal.valueOf(50)).block();

        InvoiceReadDto invoiceUsd = createInvoice(PARTNER_ID_1, USD, BigDecimal.valueOf(200));
        assertNotNull(invoiceUsd);

        // Act: fetch report
        Mono<ClientBalanceReportDto> reportMono = clientBalanceService.getClientBalance(PARTNER_ID_1, LocalDate.now());

        // Assert
        StepVerifier.create(reportMono)
                .assertNext(report -> {
                    assertNotNull(report.rows());
                    // RUB row
                    var rubRow = report.rows().stream()
                            .filter(r -> r.currencyId() == RUB)
                            .findFirst().orElseThrow();
                    assertEquals(0, BigDecimal.valueOf(200).compareTo(rubRow.leftover()));
                    assertEquals(0, BigDecimal.valueOf(250).compareTo(rubRow.outstanding()));

                    // USD row
                    var usdRow = report.rows().stream()
                            .filter(r -> r.currencyId() == USD)
                            .findFirst().orElseThrow();
                    assertEquals(0, BigDecimal.valueOf(100).compareTo(usdRow.leftover()));
                    assertEquals(0, BigDecimal.valueOf(200).compareTo(usdRow.outstanding()));

                    // Totals in RUB must be >= the raw RUB figures
                    assertTrue(report.totalLeftoverRub().compareTo(BigDecimal.valueOf(200)) >= 0);
                    assertTrue(report.totalOutstandingRub().compareTo(BigDecimal.valueOf(250)) >= 0);
                })
                .verifyComplete();
    }
}
