//package ru.utlc.financialmanagementservice.service;
//
//import org.junit.jupiter.api.Test;
//import org.junit.jupiter.api.extension.ExtendWith;
//import org.mapstruct.factory.Mappers;
//import org.mockito.InjectMocks;
//import org.mockito.Mock;
//import org.mockito.junit.jupiter.MockitoExtension;
//import reactor.core.publisher.Mono;
//import reactor.test.StepVerifier;
//import ru.utlc.financialmanagementservice.dto.payment.PaymentCreateUpdateDto;
//import ru.utlc.financialmanagementservice.dto.payment.PaymentReadDto;
//import ru.utlc.financialmanagementservice.mapper.PaymentMapper;
//import ru.utlc.financialmanagementservice.model.ClientBalance;
//import ru.utlc.financialmanagementservice.model.Payment;
//import ru.utlc.financialmanagementservice.repository.ClientBalanceRepository;
//import ru.utlc.financialmanagementservice.repository.PaymentRepository;
//
//import java.math.BigDecimal;
//import java.time.LocalDate;
//
//import static org.mockito.ArgumentMatchers.any;
//import static org.mockito.Mockito.when;
//
//@ExtendWith(MockitoExtension.class)
//class PaymentServiceTest {
//
//    @Mock
//    private PaymentRepository paymentRepository;
//
//    @Mock
//    private ClientBalanceRepository clientBalanceRepository;
//
//    // Instead of mocking the mapper, use the real mapper instance
//    private final PaymentMapper paymentMapper = Mappers.getMapper(PaymentMapper.class);
//
//    @InjectMocks
//    private PaymentService paymentService;
//
//    @Test
//    public void testUpdatePaymentAndAdjustClientBalance() {
//        Long paymentId = 1L;
//        var clientId_1 = 1;
//        var currencyId_1 = 1;
//        var paymentTypeId_1 = 1;
//        var currentDate = LocalDate.of(2024, 1, 1);
//        var updatedDate = LocalDate.of(2023, 2, 2);
//        var existingAmount = new BigDecimal("100.00");
//        var updatedAmount = new BigDecimal("150.00");
//        var existingProcessingFees = BigDecimal.ZERO;
//        var updatedProcessingFees = BigDecimal.TEN;
//        var existingTotalAmount = existingAmount.subtract(existingProcessingFees);
//
//        Payment existingPayment = new Payment(paymentId, clientId_1, paymentTypeId_1, existingAmount, currencyId_1, currentDate, existingProcessingFees, existingTotalAmount, "Test payment");
//        PaymentCreateUpdateDto dto = new PaymentCreateUpdateDto(clientId_1, updatedAmount, currencyId_1, updatedDate, paymentTypeId_1, updatedProcessingFees, "Updated payment");
//
//        // Mock repository calls
//        when(paymentRepository.findById(paymentId)).thenReturn(Mono.just(existingPayment));
//        when(paymentRepository.save(any(Payment.class))).thenAnswer(invocation -> Mono.just(invocation.getArgument(0))); // Return the updated payment from save
//        when(clientBalanceRepository.findByClientIdAndCurrencyId(any(), any())).thenReturn(Mono.just(new ClientBalance(clientId_1, clientId_1, new BigDecimal("1000.00"))));
//
//        // Act: invoke the method
//        Mono<PaymentReadDto> result = paymentService.update(paymentId, dto);
//
//        // Assert: use StepVerifier to test the reactive flow
//        StepVerifier.create(result)
//                .expectNextMatches(paymentReadDto ->
//                        paymentReadDto.id().equals(paymentId) &&
//                                paymentReadDto.amount().equals(new BigDecimal("150.00")) &&
//                                paymentReadDto.processingFees().equals(BigDecimal.TEN) &&
//                                paymentReadDto.totalAmount().equals(new BigDecimal("140.00")) &&
//                                paymentReadDto.commentary().equals("Updated payment"))
//                .verifyComplete();
//    }
//
//
//    @Test
//    void findAll() {
//    }
//
//    @Test
//    void findById() {
//    }
//
//    @Test
//    void create() {
//    }
//
//    @Test
//    void update() {
//    }
//
//    @Test
//    void delete() {
//    }
//}