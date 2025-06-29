//package ru.utlc.financialmanagementservice.integration;
//
//import lombok.RequiredArgsConstructor;
//import org.junit.jupiter.api.BeforeEach;
//import org.junit.jupiter.api.Test;
//import org.junit.jupiter.api.extension.ExtendWith;
//import org.springframework.beans.factory.annotation.Autowired;
//import org.springframework.boot.test.context.SpringBootTest;
//import org.springframework.r2dbc.core.DatabaseClient;
//import org.springframework.test.context.ActiveProfiles;
//import org.springframework.test.context.TestExecutionListeners;
//import org.springframework.test.context.junit.jupiter.SpringExtension;
//import org.springframework.test.context.support.DependencyInjectionTestExecutionListener;
//import org.testcontainers.junit.jupiter.Testcontainers;
//import reactor.core.publisher.Mono;
//import reactor.test.StepVerifier;
//import ru.utlc.financialmanagementservice.dto.partner.PartnerCreateUpdateDto;
//import ru.utlc.financialmanagementservice.dto.partner.PartnerReadDto;
//import ru.utlc.financialmanagementservice.exception.PartnerNotFoundException;
//import ru.utlc.financialmanagementservice.service.PartnerService;
//
//import java.util.List;
//
//import static org.junit.jupiter.api.Assertions.*;
//
//@TestExecutionListeners(
//        listeners = {
//                DependencyInjectionTestExecutionListener.class
//        },
//        mergeMode = TestExecutionListeners.MergeMode.REPLACE_DEFAULTS
//)
//@ExtendWith(SpringExtension.class)
//@Testcontainers
//@ActiveProfiles("test")
//@SpringBootTest
//@RequiredArgsConstructor
//class PartnerServiceIT extends IntegrationTestBase {
//
//    @Autowired
//    DatabaseClient databaseClient;
//
//    @Autowired
//    private PartnerService partnerService;
//
//    // We'll always use partner_type_id=1 in these tests
//    static final Integer PARTNER_TYPE_ID = 1;
//    static final Long EXTERNAL_ID = 5000L;
//
//    @BeforeEach
//    void resetDatabase() {
//        // Truncate partner table to ensure a fresh start
//        databaseClient.sql("TRUNCATE TABLE partner RESTART IDENTITY CASCADE")
//                .fetch()
//                .rowsUpdated()
//                .block();
//
//        // We assume partner_type with id=1 is already in the DB
//        // (pre-seeded) and references "Clients".
//    }
//
//    /**
//     * Verify partner table is initially empty, then create multiple partners and check size.
//     */
//    @Test
//    void testFindAll_size() {
//        // Initially empty
//        partnerService.findAll()
//                .collectList()
//                .as(StepVerifier::create)
//                .assertNext(list -> assertEquals(0, list.size()))
//                .verifyComplete();
//
//        // Create 2 partners
//        PartnerCreateUpdateDto p1 = new PartnerCreateUpdateDto(
//                PARTNER_TYPE_ID, // partnerTypeId = 1
//                EXTERNAL_ID      // externalId
//        );
//        PartnerCreateUpdateDto p2 = new PartnerCreateUpdateDto(
//                PARTNER_TYPE_ID,
//                EXTERNAL_ID + 1
//        );
//
//        partnerService.create(p1).block();
//        partnerService.create(p2).block();
//
//        // Now check size == 2
//        partnerService.findAll()
//                .collectList()
//                .as(StepVerifier::create)
//                .assertNext(list -> assertEquals(2, list.size()))
//                .verifyComplete();
//    }
//
//    /**
//     * Test creating a Partner. Ensure fields are correct.
//     */
//    @Test
//    void testCreatePartner() {
//        PartnerCreateUpdateDto dto = new PartnerCreateUpdateDto(
//                PARTNER_TYPE_ID,
//                EXTERNAL_ID
//        );
//
//        Mono<PartnerReadDto> resultMono = partnerService.create(dto);
//
//        StepVerifier.create(resultMono)
//                .assertNext(partner -> {
//                    // Check fields
//                    assertNotNull(partner.id());
//                    // partner_type_id should be 1
//                    assertEquals(PARTNER_TYPE_ID, partner.partnerTypeId());
//                    // external_id should be 5000
//                    assertEquals(EXTERNAL_ID, partner.externalId());
//                })
//                .verifyComplete();
//    }
//
//    /**
//     * Test findById with an existing partner.
//     */
//    @Test
//    void testFindById() {
//        // 1) Create a partner
//        var created = partnerService.create(
//                new PartnerCreateUpdateDto(PARTNER_TYPE_ID, EXTERNAL_ID)
//        ).block();
//        assertNotNull(created);
//
//        // 2) Query by ID
//        partnerService.findById(created.id())
//                .as(StepVerifier::create)
//                .assertNext(found -> {
//                    assertEquals(created.id(), found.id());
//                    assertEquals(created.externalId(), found.externalId());
//                    assertEquals(created.partnerTypeId(), found.partnerTypeId());
//                })
//                .verifyComplete();
//    }
//
//    /**
//     * Test findById for a non-existent ID => throws PartnerNotFoundException.
//     */
//    @Test
//    void testFindById_nonExistent_throwsException() {
//        partnerService.findById(9999L)
//                .as(StepVerifier::create)
//                .expectErrorMatches(ex -> ex instanceof PartnerNotFoundException
//                        && ex.getMessage().contains("error.partner.notFound"))
//                .verify();
//    }
//
//    /**
//     * Test updating an existing Partner.
//     */
//    @Test
//    void testUpdatePartner() {
//        // 1) Create a partner
//        var created = partnerService.create(
//                new PartnerCreateUpdateDto(PARTNER_TYPE_ID, EXTERNAL_ID)
//        ).block();
//        assertNotNull(created);
//
//        // 2) Prepare update
//        // e.g. change partnerTypeId to 1 again, but different externalId
//        Integer updatedType = 1;
//        Long updatedExternalId = 9999L;
//        PartnerCreateUpdateDto updateDto = new PartnerCreateUpdateDto(updatedType, updatedExternalId);
//
//        // 3) Execute update
//        partnerService.update(created.id(), updateDto)
//                .as(StepVerifier::create)
//                .assertNext(updated -> {
//                    assertEquals(created.id(), updated.id());
//                    // partner_type_id still 1
//                    assertEquals(updatedType, updated.partnerTypeId());
//                    // external_id updated to 9999
//                    assertEquals(updatedExternalId, updated.externalId());
//                })
//                .verifyComplete();
//    }
//
//    /**
//     * Updating a non-existent Partner => throws PartnerNotFoundException.
//     */
//    @Test
//    void testUpdateNonExistentPartner() {
//        PartnerCreateUpdateDto dto = new PartnerCreateUpdateDto(1, 2222L);
//
//        partnerService.update(9999L, dto)
//                .as(StepVerifier::create)
//                .expectErrorMatches(ex -> ex instanceof PartnerNotFoundException
//                        && ex.getMessage().contains("error.partner.notFound"))
//                .verify();
//    }
//
//    /**
//     * Test delete an existing Partner => returns true,
//     * then calling delete again => returns false (not found).
//     */
//    @Test
//    void testDeletePartner() {
//        // 1) Create a partner
//        var created = partnerService.create(
//                new PartnerCreateUpdateDto(PARTNER_TYPE_ID, EXTERNAL_ID)
//        ).block();
//        assertNotNull(created);
//
//        // 2) Delete => expect true
//        partnerService.delete(created.id())
//                .as(StepVerifier::create)
//                .expectNext(true)
//                .verifyComplete();
//
//        // 3) Delete again => not found => false
//        partnerService.delete(created.id())
//                .as(StepVerifier::create)
//                .expectNext(false)
//                .verifyComplete();
//    }
//
//    /**
//     * Deleting a partner that doesn't exist => returns false.
//     */
//    @Test
//    void testDeleteNonExistentPartner() {
//        partnerService.delete(9999L)
//                .as(StepVerifier::create)
//                .expectNext(false)
//                .verifyComplete();
//    }
//}
