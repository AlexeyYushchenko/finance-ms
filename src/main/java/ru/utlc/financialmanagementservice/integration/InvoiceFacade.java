//package ru.utlc.financialmanagementservice.integration;
//
//import lombok.RequiredArgsConstructor;
//import lombok.extern.slf4j.Slf4j;
//import org.springframework.stereotype.Component;
//import reactor.core.publisher.Mono;
//import ru.utlc.financialmanagementservice.dto.clientId.ClientReadDto;
//import ru.utlc.financialmanagementservice.mapper.ClientMapper;
//import ru.utlc.financialmanagementservice.model.Invoice;
//import ru.utlc.financialmanagementservice.repository.CurrencyRepository;
//import ru.utlc.financialmanagementservice.repository.InvoiceStatusRepository;
//import ru.utlc.financialmanagementservice.repository.ServiceTypeRepository;
//
//import java.util.Optional;
//import java.util.concurrent.ExecutionException;
//
//@Slf4j
//@Component
//@RequiredArgsConstructor
//public class InvoiceFacade {
//    private final ClientService clientService;
//    private final CurrencyRepository currencyRepository;
//    private final InvoiceStatusRepository invoiceStatusRepository;
//    private final ServiceTypeRepository serviceTypeRepository;
//    private final ClientMapper clientMapper;
//
//    public Invoice enrichInvoice(Invoice invoice) {
//        // Fetch clientId details asynchronously
//        Mono<ClientReadDto> clientMono = clientService.findClientById(invoice.getClient().getId());
//
//        try {
//
//            clientMono.subscribe(clientId -> invoice.setClient(clientId));
//
//            ClientReadDto clientDto = clientMono.toFuture().get();  // Blocking call for simplicity, use async handling in real applications
//            invoice.setClient(clientMapper.toEntity(clientDto));
//        } catch (InterruptedException | ExecutionException e) {
//            log.error("Error fetching clientId details", e);
//            // Handle error, e.g., set clientId to null or a default value
//        }
//
//        var invoiceStatus = Optional.ofNullable(invoice.getInvoiceStatus().getId())
//                .flatMap(invoiceStatusRepository::findById)
//                .orElse(null);
//        invoice.setInvoiceStatus(invoiceStatus);
//
//        var currency = Optional.ofNullable(invoice.getCurrency().getId())
//                .flatMap(currencyRepository::findById)
//                .orElse(null);
//        invoice.setCurrency(currency);
//
//        var serviceType = Optional.ofNullable(invoice.getServiceType().getId())
//                .flatMap(serviceTypeRepository::findById)
//                .orElse(null);
//        invoice.setServiceType(serviceType);
//
//        return invoice;
//    }
//}
