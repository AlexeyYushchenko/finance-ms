//package ru.utlc.financialmanagementservice.integration;
//
//import lombok.RequiredArgsConstructor;
//import org.junit.jupiter.api.AfterEach;
//import org.junit.jupiter.api.BeforeEach;
//import org.junit.jupiter.api.Test;
//import org.junit.jupiter.api.extension.ExtendWith;
//import org.springframework.beans.factory.annotation.Autowired;
//import org.springframework.boot.test.context.SpringBootTest;
//import org.springframework.cache.CacheManager;
//import org.springframework.test.context.ActiveProfiles;
//import org.springframework.test.context.TestExecutionListeners;
//import org.springframework.test.context.junit.jupiter.SpringExtension;
//import org.springframework.test.context.support.DependencyInjectionTestExecutionListener;
//import org.testcontainers.junit.jupiter.Testcontainers;
//import reactor.test.StepVerifier;
//import ru.utlc.financialmanagementservice.dto.paymentstatus.PaymentStatusCreateUpdateDto;
//import ru.utlc.financialmanagementservice.exception.PaymentStatusNotFoundException;
//import ru.utlc.financialmanagementservice.model.PaymentStatus;
//import ru.utlc.financialmanagementservice.repository.PaymentStatusRepository;
//import ru.utlc.financialmanagementservice.service.PaymentStatusService;
//
//import java.util.List;
//import java.util.concurrent.atomic.AtomicInteger;
//import java.util.stream.Collectors;
//
//import static org.junit.jupiter.api.Assertions.*;
//
///**
// * Adjust the imported cache name or literal string to match your PaymentStatus cache name.
// * For this example, we assume 'paymentStatuses' is used in the PaymentStatusService.
// */
//@TestExecutionListeners(
//    listeners = {
//        DependencyInjectionTestExecutionListener.class
//    },
//    mergeMode = TestExecutionListeners.MergeMode.REPLACE_DEFAULTS
//)
//@ExtendWith(SpringExtension.class)
//@Testcontainers
//@ActiveProfiles("test")
//@SpringBootTest
//@RequiredArgsConstructor
//class PaymentStatusServiceIT extends IntegrationTestBase {
//
//    @Autowired
//    private PaymentStatusService paymentStatusService;
//
//    @Autowired
//    private PaymentStatusRepository paymentStatusRepository;
//
//    @Autowired
//    private CacheManager cacheManager;
//
//    // Adjust this to whatever constant or literal you use for PaymentStatus caching
//    private static final String PAYMENT_STATUSES = "paymentStatuses";
//
//    private List<PaymentStatus> testPaymentStatuses;
//
//    @BeforeEach
//    void setUp() {
//        // Preload test-specific PaymentStatus records
//        testPaymentStatuses = paymentStatusRepository.saveAll(List.of(
//                PaymentStatus.builder().name("Temporary PaymentStatus 1").build(),
//                PaymentStatus.builder().name("Temporary PaymentStatus 2").build()
//        )).collectList().block();
//    }
//
//    @AfterEach
//    void tearDown() {
//        // Clean up only the test-specific PaymentStatus records
//        List<Integer> testIds = testPaymentStatuses.stream()
//                .map(PaymentStatus::getId)
//                .collect(Collectors.toList());
//        paymentStatusRepository.deleteAllById(testIds).block();
//    }
//
//    @Test
//    void findAll_ShouldReturnAllPaymentStatuses() {
//        // Suppose 10 records exist in the DB beforehand
//        long preAddedCount = 3;
//        long testSpecificCount = testPaymentStatuses.size();
//
//        StepVerifier.create(paymentStatusService.findAll())
//                .expectNextCount(preAddedCount + testSpecificCount)
//                .verifyComplete();
//
//        assertNotNull(cacheManager.getCache(PAYMENT_STATUSES).get("all"));
//    }
//
//    @Test
//    void findById_ShouldReturnPaymentStatus() {
//        PaymentStatus testPaymentStatus = testPaymentStatuses.get(0);
//
//        StepVerifier.create(paymentStatusService.findById(testPaymentStatus.getId()))
//                .assertNext(found -> assertEquals("Temporary PaymentStatus 1", found.name()))
//                .verifyComplete();
//
//        // Check individual cache entry
//        assertNotNull(cacheManager.getCache(PAYMENT_STATUSES).get(testPaymentStatus.getId()));
//    }
//
//    @Test
//    void findById_ShouldThrowPaymentStatusNotFoundException() {
//        StepVerifier.create(paymentStatusService.findById(999))
//                .expectErrorMatches(throwable -> throwable instanceof PaymentStatusNotFoundException)
//                .verify();
//    }
//
//    @Test
//    void create_ShouldAddNewPaymentStatus() {
//        PaymentStatusCreateUpdateDto newPaymentStatus = new PaymentStatusCreateUpdateDto("Pending Payment");
//
//        AtomicInteger id = new AtomicInteger();
//        StepVerifier.create(paymentStatusService.create(newPaymentStatus))
//                .assertNext(saved -> {
//                    id.set(saved.id());
//                    assertEquals("Pending Payment", saved.name());
//                    assertNotNull(saved.id());
//                    // Validate individual cache entry
//                    assertNotNull(cacheManager.getCache(PAYMENT_STATUSES).get(saved.id()));
//                })
//                .verifyComplete();
//
//        // "all" cache key is not automatically re-populated by the create method
//        assertNull(cacheManager.getCache(PAYMENT_STATUSES).get("all"));
//    }
//
//    @Test
//    void update_ShouldModifyExistingPaymentStatus() {
//        PaymentStatus testPaymentStatus = testPaymentStatuses.get(0);
//
//        PaymentStatusCreateUpdateDto updateDto = new PaymentStatusCreateUpdateDto("PaymentStatus Updated");
//
//        StepVerifier.create(paymentStatusService.update(testPaymentStatus.getId(), updateDto))
//                .assertNext(updated -> {
//                    assertEquals("PaymentStatus Updated", updated.name());
//                })
//                .verifyComplete();
//    }
//
//    @Test
//    void delete_ShouldRemovePaymentStatus() {
//        PaymentStatus testPaymentStatus = testPaymentStatuses.get(0);
//
//        StepVerifier.create(paymentStatusService.delete(testPaymentStatus.getId()))
//                .expectNext(true)
//                .verifyComplete();
//
//        // Verify it's gone from the database
//        StepVerifier.create(paymentStatusRepository.findById(testPaymentStatus.getId()))
//                .verifyComplete();
//
//        // Verify it's gone from the cache
//        assertNull(cacheManager.getCache(PAYMENT_STATUSES).get(testPaymentStatus.getId()));
//    }
//
//    @Test
//    void deleteNonExistentPaymentStatus_ShouldReturnFalse() {
//        // Attempting to delete a non-existent PaymentStatus
//        StepVerifier.create(paymentStatusService.delete(9999))
//                .expectNext(false)
//                .verifyComplete();
//    }
//
//    @Test
//    void updateNonExistentPaymentStatus_ShouldThrowPaymentStatusNotFoundException() {
//        PaymentStatusCreateUpdateDto updateDto = new PaymentStatusCreateUpdateDto("Non-existent Status");
//
//        StepVerifier.create(paymentStatusService.update(9999, updateDto))
//                .expectErrorMatches(throwable -> throwable instanceof PaymentStatusNotFoundException)
//                .verify();
//    }
//}
