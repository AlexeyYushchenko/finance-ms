package ru.utlc.financialmanagementservice.integration;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.TestExecutionListeners;
import org.springframework.test.context.junit.jupiter.SpringExtension;
import org.springframework.test.context.support.DependencyInjectionTestExecutionListener;
import org.springframework.transaction.annotation.Transactional;
import org.testcontainers.junit.jupiter.Testcontainers;
import reactor.test.StepVerifier;
import ru.utlc.financialmanagementservice.dto.clientbalance.ClientBalanceReadDto;
import ru.utlc.financialmanagementservice.service.ClientBalanceService;

@TestExecutionListeners(
        listeners = {
                DependencyInjectionTestExecutionListener.class,
                // Add other listeners here if needed
        },
        mergeMode = TestExecutionListeners.MergeMode.REPLACE_DEFAULTS
)
@SpringBootTest
@ExtendWith(SpringExtension.class)
@Testcontainers
@Transactional
public class ClientBalanceServiceIT extends IntegrationTestBase{

    @Autowired
    private ClientBalanceService clientBalanceService;

    @Test
    public void testFindAllBalances() {
        StepVerifier.create(clientBalanceService.findAll())
                .expectNextCount(0)
                .verifyComplete();
    }




}
