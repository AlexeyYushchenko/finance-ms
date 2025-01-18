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
import org.springframework.test.context.support.DirtiesContextTestExecutionListener;
import org.testcontainers.junit.jupiter.Testcontainers;
import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;
import ru.utlc.financialmanagementservice.dto.invoice.InvoiceCreateUpdateDto;
import ru.utlc.financialmanagementservice.dto.invoice.InvoiceReadDto;
import ru.utlc.financialmanagementservice.exception.InvoiceNotFoundException;
import ru.utlc.financialmanagementservice.service.ClientBalanceService;
import ru.utlc.financialmanagementservice.service.InvoiceService;

import java.math.BigDecimal;
import java.time.LocalDate;

import static org.junit.jupiter.api.Assertions.assertEquals;

/*
 * Integration tests for InvoiceService.
 * Ensures that CRUD operations on invoices correctly adjust client balances.
 */
@TestExecutionListeners(
        listeners = {
                DependencyInjectionTestExecutionListener.class,
                DirtiesContextTestExecutionListener.class
        },
        mergeMode = TestExecutionListeners.MergeMode.REPLACE_DEFAULTS
)
@ExtendWith(SpringExtension.class)
@Testcontainers
@ActiveProfiles("test")
@SpringBootTest
@RequiredArgsConstructor
    class InvoiceServiceIT extends IntegrationTestBase {

    @Autowired
    private DatabaseClient databaseClient;

    @Autowired
    private InvoiceService invoiceService;

    @Autowired
    private ClientBalanceService clientBalanceService;

    // Constants for test data
    private static final Integer CLIENT_ID = 1;
    private static final Integer UPDATED_CLIENT_ID = 2;
    private static final Integer CURRENCY_ID = 1;
    private static final Integer UPDATED_CURRENCY_ID = 2;
    private static final Integer SERVICE_TYPE_ID = 1;
    private static final Integer UPDATED_SERVICE_TYPE_ID = 2;
    private static final Integer STATUS_ID = 1;
    private static final Integer UPDATED_STATUS_ID = 2;
    private static final BigDecimal TOTAL_AMOUNT = new BigDecimal("500.00");
    private static final BigDecimal UPDATED_TOTAL_AMOUNT = new BigDecimal("600.00");
    private static final LocalDate ISSUE_DATE = LocalDate.now();
    private static final LocalDate UPDATED_ISSUE_DATE = ISSUE_DATE.plusDays(1);
    private static final LocalDate DUE_DATE = ISSUE_DATE.plusDays(30);
    private static final LocalDate UPDATED_DUE_DATE = DUE_DATE.plusDays(10);
    private static final String COMMENTARY = "Initial Invoice";
    private static final String UPDATED_COMMENTARY = "Updated Invoice";
    private static final Long SHIPMENT_ID = 100L;
    private static final Long UPDATED_SHIPMENT_ID = 200L;

    @BeforeEach
    void resetDatabase() {
        // Truncate all tables or reset sequences
        // Example using R2DBC DatabaseClient
        databaseClient.sql("TRUNCATE TABLE invoice, client_balance RESTART IDENTITY CASCADE").fetch().rowsUpdated().block();
    }


    /**
     * Test to verify the number of invoices in the database.
     */
    @Test
    public void testSize() {
        // 1) (Optional) Reset or verify the DB is empty
        //    If you truncate in @BeforeEach, it should be empty at this point.

        // 2) Create some Invoices for this test
        InvoiceCreateUpdateDto dto1 = new InvoiceCreateUpdateDto(
                1,  // clientId
                1,  // serviceTypeId
                new BigDecimal("100.00"),
                LocalDate.now(),
                LocalDate.now().plusDays(30),
                "Invoice #1",
                1,   // currencyId
                10L, // shipmentId
                1    // statusId
        );

        InvoiceCreateUpdateDto dto2 = new InvoiceCreateUpdateDto(
                2,  // clientId
                2,  // serviceTypeId
                new BigDecimal("200.00"),
                LocalDate.now(),
                LocalDate.now().plusDays(15),
                "Invoice #2",
                2,   // currencyId
                20L, // shipmentId
                2    // statusId
        );

        invoiceService.create(dto1).block();
        invoiceService.create(dto2).block();

        // 3) Now check the total count
        StepVerifier.create(invoiceService.findAll().collectList())
                .assertNext(invoices -> {
                    // We inserted exactly 2 in this test
                    assertEquals(2, invoices.size());
                })
                .verifyComplete();
    }


    /**
     * Test the 'create' method.
     * Verifies that an invoice is created correctly and the client's balance is adjusted accordingly.
     */
    @Test
    public void testCreateInvoice() {
        InvoiceCreateUpdateDto dto = new InvoiceCreateUpdateDto(
                CLIENT_ID,
                SERVICE_TYPE_ID,
                TOTAL_AMOUNT,
                ISSUE_DATE,
                DUE_DATE,
                COMMENTARY,
                CURRENCY_ID,
                SHIPMENT_ID,
                STATUS_ID
        );

        Mono<InvoiceReadDto> resultMono = invoiceService.create(dto);

        StepVerifier.create(resultMono)
                .assertNext(result -> {
                    assertEquals(CLIENT_ID, result.clientId().intValue());
                    assertEquals(SERVICE_TYPE_ID, result.serviceTypeId().intValue());
                    assertEquals(TOTAL_AMOUNT, result.totalAmount());
                    assertEquals(ISSUE_DATE, result.issueDate());
                    assertEquals(DUE_DATE, result.dueDate());
                    assertEquals(COMMENTARY, result.commentary());
                    assertEquals(CURRENCY_ID, result.currencyId().intValue());
                    assertEquals(SHIPMENT_ID, result.shipmentId());
                    assertEquals(STATUS_ID, result.statusId().intValue());
                })
                .verifyComplete();

        // Verify the client balance is adjusted correctly
        StepVerifier.create(clientBalanceService.findByClientIdAndCurrencyId(CLIENT_ID, CURRENCY_ID))
                .assertNext(balance -> {
                    // Ensure the balance is -500.00
                    System.out.println("balance.balance(): " + balance.balance());
                    System.out.println("TOTAL_AMOUNT.negate(): " + TOTAL_AMOUNT.negate());
                    assertEquals(0, balance.balance().compareTo(TOTAL_AMOUNT.negate()));
                })
                .verifyComplete();
    }

    /**
     * Test the 'update' method with all fields.
     * Verifies that an existing invoice is updated correctly and the client's balance is adjusted accordingly.
     */
    @Test
    public void testUpdateInvoiceWithAllFields() {
        // First, create an invoice to update
        InvoiceCreateUpdateDto createDto = new InvoiceCreateUpdateDto(
                CLIENT_ID,
                SERVICE_TYPE_ID,
                TOTAL_AMOUNT,
                ISSUE_DATE,
                DUE_DATE,
                COMMENTARY,
                CURRENCY_ID,
                SHIPMENT_ID,
                STATUS_ID
        );

        Mono<InvoiceReadDto> createdInvoiceMono = invoiceService.create(createDto);

        InvoiceReadDto createdInvoice = createdInvoiceMono.block();

        assert createdInvoice != null; // Ensure invoice was created

        // Now, update the invoice
        InvoiceCreateUpdateDto updateDto = new InvoiceCreateUpdateDto(
                UPDATED_CLIENT_ID,
                UPDATED_SERVICE_TYPE_ID,
                UPDATED_TOTAL_AMOUNT,
                UPDATED_ISSUE_DATE,
                UPDATED_DUE_DATE,
                UPDATED_COMMENTARY,
                UPDATED_CURRENCY_ID,
                UPDATED_SHIPMENT_ID,
                UPDATED_STATUS_ID
        );

        Mono<InvoiceReadDto> updatedInvoiceMono = invoiceService.update(createdInvoice.id(), updateDto);

        StepVerifier.create(updatedInvoiceMono)
                .assertNext(result -> {
                    assertEquals(UPDATED_CLIENT_ID, result.clientId().intValue());
                    assertEquals(UPDATED_SERVICE_TYPE_ID, result.serviceTypeId().intValue());
                    assertEquals(UPDATED_TOTAL_AMOUNT, result.totalAmount());
                    assertEquals(UPDATED_ISSUE_DATE, result.issueDate());
                    assertEquals(UPDATED_DUE_DATE, result.dueDate());
                    assertEquals(UPDATED_COMMENTARY, result.commentary());
                    assertEquals(UPDATED_CURRENCY_ID, result.currencyId().intValue());
                    assertEquals(UPDATED_SHIPMENT_ID, result.shipmentId());
                    assertEquals(UPDATED_STATUS_ID, result.statusId().intValue());
                })
                .verifyComplete();

        // Verify the client balance adjustments
        // Original invoice adjusted CLIENT_ID's balance by -500
        // Updated invoice moves from CLIENT_ID=1 to CLIENT_ID=2 and adjusts by -600
        // So, CLIENT_ID=1's balance should be +500 (restored to 0)
        // CLIENT_ID=2's balance should be -600
//        StepVerifier.create(clientBalanceService.findByClientIdAndCurrencyId(CLIENT_ID, CURRENCY_ID))
//                .assertNext(balance -> {
//                    // Ensure the balance is 0
//                    System.out.println("balance.balance(): " + balance.balance());
//                    System.out.println("UPDATED_TOTAL_AMOUNT.negate(): " + UPDATED_TOTAL_AMOUNT.negate());
//                    assertEquals(0, balance.balance().compareTo(BigDecimal.ZERO));
//                })
//                .verifyComplete();

        StepVerifier.create(clientBalanceService.findByClientIdAndCurrencyId(UPDATED_CLIENT_ID, UPDATED_CURRENCY_ID))
                .assertNext(balance -> {
                    // Ensure the balance is -600.00
                    System.out.println("balance.balance(): " + balance.balance());
                    System.out.println("UPDATED_TOTAL_AMOUNT.negate(): " + UPDATED_TOTAL_AMOUNT.negate());
                    assertEquals(0, balance.balance().compareTo(UPDATED_TOTAL_AMOUNT.negate()));
                })
                .verifyComplete();
    }

    /**
     * Test the 'delete' method.
     * Verifies that an existing invoice is deleted correctly and the client's balance is adjusted accordingly.
     */
    @Test
    public void testDeleteInvoice() {
        // First, create an invoice to delete
        InvoiceCreateUpdateDto dto = new InvoiceCreateUpdateDto(
                CLIENT_ID,
                SERVICE_TYPE_ID,
                TOTAL_AMOUNT,
                ISSUE_DATE,
                DUE_DATE,
                COMMENTARY,
                CURRENCY_ID,
                SHIPMENT_ID,
                STATUS_ID
        );

        Mono<InvoiceReadDto> createdInvoiceMono = invoiceService.create(dto);

        InvoiceReadDto createdInvoice = createdInvoiceMono.block();

        assert createdInvoice != null; // Ensure invoice was created

        // Now, delete the invoice
        Mono<Boolean> deleteResultMono = invoiceService.delete(createdInvoice.id());

        StepVerifier.create(deleteResultMono)
                .expectNext(true)
                .verifyComplete();

        // Attempt to delete again, should return false
        StepVerifier.create(invoiceService.delete(createdInvoice.id()))
                .expectNext(false)
                .verifyComplete();

        // Verify the client balance is adjusted correctly (adding back the total amount)
        StepVerifier.create(clientBalanceService.findByClientIdAndCurrencyId(CLIENT_ID, CURRENCY_ID))
                .assertNext(balance -> {
                    // Ensure the balance is 0
                    assertEquals(0, balance.balance().compareTo(BigDecimal.ZERO));
                })
                .verifyComplete();
    }

    /**
     * Test deleting a non-existent invoice.
     * Verifies that attempting to delete an invoice that doesn't exist returns false without throwing an exception.
     */
    @Test
    public void testDeleteNonExistentInvoice() {
        Mono<Boolean> result = invoiceService.delete(999L); // Assuming 999L does not exist

        StepVerifier.create(result)
                .expectNext(false)
                .verifyComplete();
    }

    /**
     * Test updating a non-existent invoice.
     * Verifies that attempting to update an invoice that doesn't exist results in an empty Mono.
     */
    @Test
    public void testUpdateNonExistentInvoice() {
        InvoiceCreateUpdateDto updateDto = new InvoiceCreateUpdateDto(
                CLIENT_ID,
                SERVICE_TYPE_ID,
                TOTAL_AMOUNT,
                ISSUE_DATE,
                DUE_DATE,
                COMMENTARY,
                CURRENCY_ID,
                SHIPMENT_ID,
                STATUS_ID
        );

        Mono<InvoiceReadDto> resultMono = invoiceService.update(999L, updateDto); // Assuming 999L does not exist

        StepVerifier.create(resultMono)
                .expectComplete() // Expects the Mono to complete without emitting a value
                .verify();
    }

    @Test
    public void testFindInvoiceById_NonExistent_ThrowsException() {
        Mono<InvoiceReadDto> resultMono = invoiceService.findById(999L); // Non-existing ID

        StepVerifier.create(resultMono)
                .expectErrorMatches(throwable ->
                        throwable instanceof InvoiceNotFoundException
                                && throwable.getMessage().contains("error.invoice.notFound"))
                .verify();
    }

}
