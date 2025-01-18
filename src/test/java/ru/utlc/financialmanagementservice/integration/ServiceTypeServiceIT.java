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
import ru.utlc.financialmanagementservice.dto.servicetype.ServiceTypeCreateUpdateDto;
import ru.utlc.financialmanagementservice.dto.servicetype.ServiceTypeReadDto;
import ru.utlc.financialmanagementservice.exception.ServiceTypeNotFoundException;
import ru.utlc.financialmanagementservice.model.ServiceType;
import ru.utlc.financialmanagementservice.repository.ServiceTypeRepository;
import ru.utlc.financialmanagementservice.service.ServiceTypeService;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.Collectors;

import static org.junit.jupiter.api.Assertions.*;
import static ru.utlc.financialmanagementservice.constants.CacheNames.*;

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
class ServiceTypeServiceIT extends IntegrationTestBase {

    @Autowired
    private ServiceTypeService serviceTypeService;

    @Autowired
    private ServiceTypeRepository serviceTypeRepository;

    @Autowired
    private CacheManager cacheManager;

    private List<ServiceType> testServiceTypes;

    @BeforeEach
    void setUp() {
        // Preload test-specific service types
        testServiceTypes = serviceTypeRepository.saveAll(List.of(
                ServiceType.builder().name("Temporary Logistics").description("Temporary Logistics management").build(),
                ServiceType.builder().name("Temporary Warehousing").description("Temporary Warehouse operations").build()
        )).collectList().block();
    }

    @AfterEach
    void tearDown() {
        // Delete only the test-specific service types
        List<Integer> testIds = testServiceTypes.stream().map(ServiceType::getId).collect(Collectors.toList());
        serviceTypeRepository.deleteAllById(testIds).block();
    }

    @Test
    void findAll_ShouldReturnAllServiceTypes() {
        long preAddedCount = 10; // Number of records pre-added to the database
        long testSpecificCount = testServiceTypes.size();

        StepVerifier.create(serviceTypeService.findAll())
                .expectNextCount(preAddedCount + testSpecificCount)
                .verifyComplete();

        assertNotNull(cacheManager.getCache(SERVICE_TYPES).get("all"));
    }

    @Test
    void findById_ShouldReturnServiceType() {
        ServiceType testServiceType = testServiceTypes.get(0);

        StepVerifier.create(serviceTypeService.findById(testServiceType.getId()))
                .assertNext(serviceType -> assertEquals("Temporary Logistics", serviceType.name()))
                .verifyComplete();

        assertNotNull(cacheManager.getCache(SERVICE_TYPES).get(testServiceType.getId()));
    }

    @Test
    void findById_ShouldThrowServiceTypeNotFoundException() {
        StepVerifier.create(serviceTypeService.findById(999))
                .expectErrorMatches(throwable -> throwable instanceof ServiceTypeNotFoundException)
                .verify();
    }

    @Test
    void create_ShouldAddNewServiceType() {
        ServiceTypeCreateUpdateDto newServiceType = new ServiceTypeCreateUpdateDto("Transportation", "Transport services");

        AtomicInteger id = new AtomicInteger();
        StepVerifier.create(serviceTypeService.create(newServiceType))
                .assertNext(serviceType -> {
                    id.set(serviceType.id());
                    assertEquals("Transportation", serviceType.name());
                    assertNotNull(serviceType.id());
                    // Validate individual cache entry
                    assertNotNull(cacheManager.getCache(SERVICE_TYPES).get(serviceType.id()));
                })
                .verifyComplete();

        // Validate that "all" cache is not populated automatically
        assertNull(cacheManager.getCache(SERVICE_TYPES).get("all"));
    }


    @Test
    void update_ShouldModifyExistingServiceType() {
        ServiceType testServiceType = testServiceTypes.get(0);

        ServiceTypeCreateUpdateDto updateDto = new ServiceTypeCreateUpdateDto("Logistics Updated", "Updated description");

        StepVerifier.create(serviceTypeService.update(testServiceType.getId(), updateDto))
                .assertNext(updated -> {
                    assertEquals("Logistics Updated", updated.name());
                    assertEquals("Updated description", updated.description());
                })
                .verifyComplete();
    }

    @Test
    void delete_ShouldRemoveServiceType() {
        ServiceType testServiceType = testServiceTypes.get(0);

        StepVerifier.create(serviceTypeService.delete(testServiceType.getId()))
                .expectNext(true)
                .verifyComplete();

        StepVerifier.create(serviceTypeRepository.findById(testServiceType.getId()))
                .verifyComplete();

        assertNull(cacheManager.getCache(SERVICE_TYPES).get(testServiceType.getId()));
    }
}
