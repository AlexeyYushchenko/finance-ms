package ru.utlc.financialmanagementservice.integration;

import lombok.RequiredArgsConstructor;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.cache.CacheManager;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.TestExecutionListeners;
import org.springframework.test.context.junit.jupiter.SpringExtension;
import org.springframework.test.context.support.DependencyInjectionTestExecutionListener;
import org.testcontainers.junit.jupiter.Testcontainers;
import reactor.test.StepVerifier;
import ru.utlc.financialmanagementservice.dto.paymenttype.PaymentTypeCreateUpdateDto;
import ru.utlc.financialmanagementservice.dto.paymenttype.PaymentTypeReadDto;
import ru.utlc.financialmanagementservice.exception.PaymentTypeNotFoundException;
import ru.utlc.financialmanagementservice.model.PaymentType;
import ru.utlc.financialmanagementservice.repository.PaymentTypeRepository;
import ru.utlc.financialmanagementservice.service.PaymentTypeService;

import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.Collectors;

import static org.junit.jupiter.api.Assertions.*;
import static ru.utlc.financialmanagementservice.constants.CacheNames.PAYMENT_TYPES;

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
class PaymentTypeServiceIT extends IntegrationTestBase {

    @Autowired
    private PaymentTypeService paymentTypeService;

    @Autowired
    private PaymentTypeRepository paymentTypeRepository;

    @Autowired
    private CacheManager cacheManager;

    private List<PaymentType> testPaymentTypes;

    @BeforeEach
    void setUp() {
        // Preload test-specific payment types
        testPaymentTypes = paymentTypeRepository.saveAll(List.of(
                PaymentType.builder().name("Test Wire Transfer").description("Test Payment via bank transfer").build(),
                PaymentType.builder().name("Test Credit Card").description("Test Payment using credit card").build()
        )).collectList().block();
    }

    @AfterEach
    void tearDown() {
        // Delete only the test-specific payment types
        List<Integer> testIds = testPaymentTypes.stream().map(PaymentType::getId).collect(Collectors.toList());
        paymentTypeRepository.deleteAllById(testIds).block();
    }

    @Test
    void findAll_ShouldReturnTestSpecificPaymentTypes() {
        List<String> testPaymentTypeNames = testPaymentTypes.stream()
                .map(PaymentType::getName)
                .toList();

        StepVerifier.create(paymentTypeService.findAll()
                        .filter(paymentType -> testPaymentTypeNames.contains(paymentType.name())))
                .expectNextCount(testPaymentTypeNames.size())
                .verifyComplete();

        assertNotNull(cacheManager.getCache(PAYMENT_TYPES).get("all"));
    }

    @Test
    void findById_ShouldReturnPaymentType() {
        PaymentType testPaymentType = testPaymentTypes.get(0);

        StepVerifier.create(paymentTypeService.findById(testPaymentType.getId()))
                .assertNext(paymentType -> assertEquals("Test Wire Transfer", paymentType.name()))
                .verifyComplete();

        assertNotNull(cacheManager.getCache(PAYMENT_TYPES).get(testPaymentType.getId()));
    }

    @Test
    void findById_ShouldThrowPaymentTypeNotFoundException() {
        StepVerifier.create(paymentTypeService.findById(999))
                .expectErrorMatches(throwable -> throwable instanceof PaymentTypeNotFoundException)
                .verify();
    }

    @Test
    void create_ShouldAddNewPaymentType() {
        PaymentTypeCreateUpdateDto newPaymentType = new PaymentTypeCreateUpdateDto("PayPal", "Online payment via PayPal");

        AtomicInteger id = new AtomicInteger();
        StepVerifier.create(paymentTypeService.create(newPaymentType))
                .assertNext(paymentType -> {
                    id.set(paymentType.id());
                    assertEquals("PayPal", paymentType.name());
                    assertNotNull(paymentType.id());
                    // Validate individual cache entry
                    assertNotNull(cacheManager.getCache(PAYMENT_TYPES).get(paymentType.id()));
                })
                .verifyComplete();

        // Validate that "all" cache is not populated automatically
        assertNull(cacheManager.getCache(PAYMENT_TYPES).get("all"));
    }

    @Test
    void update_ShouldModifyExistingPaymentType() {
        PaymentType testPaymentType = testPaymentTypes.get(0);

        PaymentTypeCreateUpdateDto updateDto = new PaymentTypeCreateUpdateDto("Test Wire Transfer Updated", "Updated description");

        StepVerifier.create(paymentTypeService.update(testPaymentType.getId(), updateDto))
                .assertNext(updated -> {
                    assertEquals("Test Wire Transfer Updated", updated.name());
                    assertEquals("Updated description", updated.description());
                })
                .verifyComplete();
    }

    @Test
    void delete_ShouldRemovePaymentType() {
        PaymentType testPaymentType = testPaymentTypes.get(0);

        StepVerifier.create(paymentTypeService.delete(testPaymentType.getId()))
                .expectNext(true)
                .verifyComplete();

        StepVerifier.create(paymentTypeRepository.findById(testPaymentType.getId()))
                .verifyComplete();

        assertNull(cacheManager.getCache(PAYMENT_TYPES).get(testPaymentType.getId()));
    }
}
