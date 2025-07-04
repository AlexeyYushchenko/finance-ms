package ru.utlc.integration;

import lombok.RequiredArgsConstructor;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.r2dbc.core.DatabaseClient;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.junit.jupiter.SpringExtension;
import org.testcontainers.junit.jupiter.Testcontainers;
import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;
import ru.utlc.dto.invoice.InvoiceReadDto;
import ru.utlc.dto.invoice.InvoiceCreateUpdateDto;
import ru.utlc.exception.InvoiceNotFoundException;
import ru.utlc.exception.InvoiceUpdateException;
import ru.utlc.model.InvoiceDirection;
import ru.utlc.service.InvoiceService;

import java.math.BigDecimal;
import java.time.LocalDate;

import static org.junit.jupiter.api.Assertions.assertEquals;

@ExtendWith(SpringExtension.class)
@SpringBootTest
@ActiveProfiles("test")
@Testcontainers
@RequiredArgsConstructor(onConstructor_ = @Autowired)
class InvoiceServiceIT {

    private static final Long PARTNER_ID_1 = 1L;
    private static final Long PARTNER_ID_2 = 2L;
    private static final Integer SERVICE_TYPE_ID = 1;
    private static final BigDecimal TOTAL_AMOUNT_1 = new BigDecimal("1000.00");
    private static final BigDecimal TOTAL_AMOUNT_2 = new BigDecimal("2000.00");
    private static final LocalDate ISSUE_DATE_1 = LocalDate.now();
    private static final LocalDate DUE_DATE_1 = ISSUE_DATE_1.plusDays(30);
    private static final LocalDate ISSUE_DATE_2 = ISSUE_DATE_1.plusDays(5);
    private static final LocalDate DUE_DATE_2 = ISSUE_DATE_2.plusDays(30);
    private static final String COMMENT_1 = "Initial Invoice";
    private static final String COMMENT_2 = "Updated Invoice";
    private static final int STATUS_DRAFT = 1;
    private static final int STATUS_ACTIVE = 2;

    private final DatabaseClient databaseClient;
    private final InvoiceService invoiceService;

    @BeforeEach
    void resetDatabase() {
        // Only truncate invoice table; partner is external now
        databaseClient
                .sql("TRUNCATE TABLE invoice RESTART IDENTITY CASCADE")
                .fetch()
                .rowsUpdated()
                .block();
    }

    private Mono<InvoiceReadDto> createInvoiceMono(
            Long partnerId,
            Integer serviceTypeId,
            BigDecimal totalAmount,
            LocalDate issueDate,
            LocalDate dueDate,
            String commentary,
            Integer currencyId,
            Long shipmentId,
            Integer statusId
    ) {
        InvoiceCreateUpdateDto dto = new InvoiceCreateUpdateDto(
                InvoiceDirection.RECEIVABLE,
                partnerId,
                serviceTypeId,
                totalAmount,
                issueDate,
                dueDate,
                commentary,
                currencyId,
                shipmentId,
                statusId
        );
        return invoiceService.create(dto);
    }

    @Test
    void testCreateInvoice() {
        InvoiceCreateUpdateDto dto = new InvoiceCreateUpdateDto(
                InvoiceDirection.RECEIVABLE,
                PARTNER_ID_1,
                SERVICE_TYPE_ID,
                TOTAL_AMOUNT_1,
                ISSUE_DATE_1,
                DUE_DATE_1,
                COMMENT_1,
                1,
                100L,
                STATUS_DRAFT
        );

        StepVerifier.create(invoiceService.create(dto))
                .assertNext(inv -> {
                    assertEquals(PARTNER_ID_1, inv.partnerId());
                    assertEquals(SERVICE_TYPE_ID, inv.serviceTypeId());
                    assertEquals(TOTAL_AMOUNT_1, inv.totalAmount());
                    assertEquals(ISSUE_DATE_1, inv.issueDate());
                    assertEquals(DUE_DATE_1, inv.dueDate());
                    assertEquals(COMMENT_1, inv.commentary());
                    assertEquals(1, inv.currencyId());
                    assertEquals(100L, inv.shipmentId());
                    assertEquals(STATUS_DRAFT, inv.statusId());
                })
                .verifyComplete();
    }

    @Test
    void testUpdateInvoice_AllowedChanges() {
        InvoiceReadDto created = createInvoiceMono(
                PARTNER_ID_1, SERVICE_TYPE_ID, TOTAL_AMOUNT_1,
                ISSUE_DATE_1, DUE_DATE_1, COMMENT_1,
                1, 100L, STATUS_DRAFT
        ).block();

        InvoiceCreateUpdateDto updateDto = new InvoiceCreateUpdateDto(
                InvoiceDirection.RECEIVABLE,
                created.partnerId(),
                2,
                new BigDecimal("1500.00"),
                created.issueDate(),
                created.dueDate(),
                COMMENT_2,
                created.currencyId(),
                created.shipmentId(),
                STATUS_ACTIVE
        );

        StepVerifier.create(invoiceService.update(created.id(), updateDto))
                .assertNext(inv -> assertEquals(new BigDecimal("1500.00"), inv.totalAmount()))
                .verifyComplete();
    }

    @Test
    void testUpdateInvoice_ChangePartner_ShouldFail() {
        InvoiceReadDto created = createInvoiceMono(
                PARTNER_ID_1, SERVICE_TYPE_ID, TOTAL_AMOUNT_1,
                ISSUE_DATE_1, DUE_DATE_1, COMMENT_1,
                1, 100L, STATUS_DRAFT
        ).block();

        InvoiceCreateUpdateDto invalid = new InvoiceCreateUpdateDto(
                InvoiceDirection.RECEIVABLE,
                PARTNER_ID_2,
                created.serviceTypeId(),
                created.totalAmount(),
                created.issueDate(),
                created.dueDate(),
                created.commentary(),
                created.currencyId(),
                created.shipmentId(),
                created.statusId()
        );

        StepVerifier.create(invoiceService.update(created.id(), invalid))
                .expectError(InvoiceUpdateException.class)
                .verify();
    }

    @Test
    void testUpdateInvoice_ChangeCurrency_ShouldFail() {
        InvoiceReadDto created = createInvoiceMono(
                PARTNER_ID_1, SERVICE_TYPE_ID, TOTAL_AMOUNT_1,
                ISSUE_DATE_1, DUE_DATE_1, COMMENT_1,
                1, 100L, STATUS_DRAFT
        ).block();

        InvoiceCreateUpdateDto invalid = new InvoiceCreateUpdateDto(
                InvoiceDirection.RECEIVABLE,
                created.partnerId(),
                created.serviceTypeId(),
                created.totalAmount(),
                created.issueDate(),
                created.dueDate(),
                created.commentary(),
                2,
                created.shipmentId(),
                created.statusId()
        );

        StepVerifier.create(invoiceService.update(created.id(), invalid))
                .expectError(InvoiceUpdateException.class)
                .verify();
    }

    @Test
    void testUpdateInvoice_ChangeIssueDate_ShouldFail() {
        InvoiceReadDto created = createInvoiceMono(
                PARTNER_ID_1, SERVICE_TYPE_ID, TOTAL_AMOUNT_1,
                ISSUE_DATE_1, DUE_DATE_1, COMMENT_1,
                1, 100L, STATUS_DRAFT
        ).block();

        InvoiceCreateUpdateDto invalid = new InvoiceCreateUpdateDto(
                InvoiceDirection.RECEIVABLE,
                created.partnerId(),
                created.serviceTypeId(),
                created.totalAmount(),
                created.issueDate().plusDays(1),
                created.dueDate(),
                created.commentary(),
                created.currencyId(),
                created.shipmentId(),
                created.statusId()
        );

        StepVerifier.create(invoiceService.update(created.id(), invalid))
                .expectError(InvoiceUpdateException.class)
                .verify();
    }

    @Test
    void testUpdateInvoice_AlreadyCancelled_ShouldFail() {
        InvoiceReadDto created = createInvoiceMono(
                PARTNER_ID_1, SERVICE_TYPE_ID, TOTAL_AMOUNT_1,
                ISSUE_DATE_1, DUE_DATE_1, COMMENT_1,
                1, 100L, STATUS_DRAFT
        ).block();

        // cancel via delete()
        invoiceService.delete(created.id()).block();

        InvoiceCreateUpdateDto updateDto = new InvoiceCreateUpdateDto(
                InvoiceDirection.RECEIVABLE,
                created.partnerId(),
                created.serviceTypeId(),
                created.totalAmount(),
                created.issueDate(),
                created.dueDate(),
                created.commentary(),
                created.currencyId(),
                created.shipmentId(),
                STATUS_ACTIVE
        );

        StepVerifier.create(invoiceService.update(created.id(), updateDto))
                .expectError(InvoiceUpdateException.class)
                .verify();
    }

    @Test
    void testUpdateInvoice_PartiallyPaid_ShouldFail() {
        InvoiceReadDto created = createInvoiceMono(
                PARTNER_ID_1, SERVICE_TYPE_ID, TOTAL_AMOUNT_1,
                ISSUE_DATE_1, DUE_DATE_1, COMMENT_1,
                1, 100L, STATUS_DRAFT
        ).block();

        // apply partial payment
        invoiceService.addToPaidAmount(created.id(), new BigDecimal("100.00")).block();

        InvoiceCreateUpdateDto updateDto = new InvoiceCreateUpdateDto(
                InvoiceDirection.RECEIVABLE,
                created.partnerId(),
                created.serviceTypeId(),
                created.totalAmount(),
                created.issueDate(),
                created.dueDate(),
                created.commentary(),
                created.currencyId(),
                created.shipmentId(),
                STATUS_ACTIVE
        );

        StepVerifier.create(invoiceService.update(created.id(), updateDto))
                .expectError(InvoiceUpdateException.class)
                .verify();
    }

    @Test
    void testFindByPartnerId() {
        // create invoices for partners 1 & 2
        createInvoiceMono(1L, SERVICE_TYPE_ID, TOTAL_AMOUNT_1, ISSUE_DATE_1, DUE_DATE_1, COMMENT_1, 1, 100L, STATUS_DRAFT).block();
        createInvoiceMono(2L, SERVICE_TYPE_ID, TOTAL_AMOUNT_2, ISSUE_DATE_1, DUE_DATE_2, COMMENT_1, 1, 101L, STATUS_DRAFT).block();
        createInvoiceMono(2L, SERVICE_TYPE_ID, TOTAL_AMOUNT_1, ISSUE_DATE_2, DUE_DATE_2, COMMENT_2, 1, 102L, STATUS_ACTIVE).block();

        // partner 2 should have 2 invoices
        StepVerifier.create(invoiceService.findByPartnerId(2L).collectList())
                .assertNext(list -> assertEquals(2, list.size()))
                .verifyComplete();

        // nonexistent partner => error
        StepVerifier.create(invoiceService.findByPartnerId(9999L))
                .expectErrorMatches(ex -> ex instanceof InvoiceNotFoundException
                        && ex.getMessage().contains("error.invoice.partner.notFound"))
                .verify();
    }
}
