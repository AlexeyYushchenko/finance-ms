package ru.utlc.financialmanagementservice.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.ReactiveTransactionManager;
import org.springframework.transaction.reactive.TransactionalOperator;
import org.w3c.dom.Document;
import org.w3c.dom.NodeList;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import ru.utlc.financialmanagementservice.dto.currency.CurrencyReadDto;
import ru.utlc.financialmanagementservice.model.ExchangeRate;
import ru.utlc.financialmanagementservice.model.RateInfo;
import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.net.URL;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.Locale;
import java.util.stream.IntStream;

@Service
@RequiredArgsConstructor
@Slf4j
public class CurrencyRateService {

    private final CurrencyService currencyService;
    private final ExchangeRateService exchangeRateService;
    private final ReactiveTransactionManager transactionManager;
    private final RateUpdateLogService rateUpdateLogService;


    private TransactionalOperator transactionalOperator() {
        return TransactionalOperator.create(transactionManager);
    }

    public Mono<Boolean> checkIfRatesUpdatedForToday() {
        LocalDate today = LocalDate.now();
        return rateUpdateLogService.checkIfRatesUpdatedForToday(today);
    }

    public Mono<Void> fetchAndSaveRates(LocalDate date) {
        System.out.println(date.format(DateTimeFormatter.ofPattern("dd MM yyyy").localizedBy(Locale.forLanguageTag("ru"))));
        String formattedDate = date.format(DateTimeFormatter.ofPattern("dd/MM/yyyy"));
        System.out.println(formattedDate);
        String url = "https://www.cbr.ru/scripts/XML_daily.asp?date_req=" + formattedDate;

        return currencyService.findAll()
                .collectMap(CurrencyReadDto::code)  // Map by charCode for fast lookup
                .flatMapMany(enabledCurrencies -> fetchRatesFromUrl(url)
                        .flatMapMany(this::parseRatesFromXml)
                        .filter(rateInfo -> enabledCurrencies.containsKey(rateInfo.charCode()))  // Filter only enabled currencies
                        .flatMap(rateInfo -> saveExchangeRate(rateInfo, enabledCurrencies.get(rateInfo.charCode()), date))) // Pass currency ID
                .then(exchangeRateService.logSuccessfulRateUpdate(date))
                .as(transactionalOperator()::transactional);
    }

    private Mono<Document> fetchRatesFromUrl(String url) {
        return Mono.fromCallable(() -> {
            DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
            factory.setExpandEntityReferences(false);
            factory.setFeature("http://apache.org/xml/features/disallow-doctype-decl", true);
            factory.setFeature("http://xml.org/sax/features/external-general-entities", false);
            factory.setFeature("http://xml.org/sax/features/external-parameter-entities", false);

            DocumentBuilder builder = factory.newDocumentBuilder();
            return builder.parse(new URL(url).openStream());
        }).doOnError(e -> log.error("Error fetching XML from URL: {}", url, e));
    }

    private Flux<RateInfo> parseRatesFromXml(Document document) {
        NodeList valuteNodes = document.getElementsByTagName("Valute");

        return Flux.fromStream(IntStream.range(0, valuteNodes.getLength())
                .mapToObj(i -> {
                    String charCode = document.getElementsByTagName("CharCode").item(i).getTextContent();
                    String value = document.getElementsByTagName("Value").item(i).getTextContent().replace(",", ".");
                    String nominal = document.getElementsByTagName("Nominal").item(i).getTextContent();

                    return new RateInfo(charCode, new BigDecimal(value), new BigDecimal(nominal));
                }));
    }

    private Mono<ExchangeRate> saveExchangeRate(RateInfo rateInfo, CurrencyReadDto currencyDto, LocalDate date) {
        BigDecimal officialRate = rateInfo.value().divide(rateInfo.nominal(), 6, RoundingMode.HALF_UP);

        // Already in DB, just return existingRate
        return exchangeRateService
                .findByCurrencyFromIdAndCurrencyToIdAndRateDate(currencyDto.id(), 1, date)
                .switchIfEmpty(
                        // Insert only if not found
                        Mono.defer(() -> {
                            ExchangeRate exchangeRate = new ExchangeRate();
                            exchangeRate.setCurrencyFromId(currencyDto.id());
                            exchangeRate.setCurrencyToId(1); // 1 for RUB
                            exchangeRate.setOfficialRate(officialRate);
                            exchangeRate.setStandardRate(officialRate.multiply(BigDecimal.valueOf(1.02)));
                            exchangeRate.setPremiumClientRate(officialRate.multiply(BigDecimal.valueOf(1.01)));
                            exchangeRate.setRateDate(date);

                            return exchangeRateService.saveExchangeRate(exchangeRate);
                        })
                );
    }
}
