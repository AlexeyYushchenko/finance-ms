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
import reactor.core.scheduler.Schedulers;
import ru.utlc.financialmanagementservice.dto.currency.CurrencyReadDto;
import ru.utlc.financialmanagementservice.exception.ExchangeRateRetrievalFailedException;
import ru.utlc.financialmanagementservice.exception.ValidationException;
import ru.utlc.financialmanagementservice.model.ExchangeRate;
import ru.utlc.financialmanagementservice.model.RateInfo;
import ru.utlc.financialmanagementservice.repository.ExchangeRateRepository;
import ru.utlc.financialmanagementservice.repository.RateUpdateLogRepository;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.net.URL;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.time.temporal.ChronoUnit;
import java.util.Locale;
import java.util.stream.IntStream;

@Slf4j
@Service
@RequiredArgsConstructor
public class ExchangeRateService {

    private final ExchangeRateRepository exchangeRateRepository;
    private final RateUpdateLogRepository rateUpdateLogRepository;
    private final CurrencyService currencyService;
    private final RateUpdateLogService rateUpdateLogService;
    private final ReactiveTransactionManager transactionManager;

    private static final Integer RUB_CURRENCY_ID = 1;


    private TransactionalOperator transactionalOperator() {
        return TransactionalOperator.create(transactionManager);
    }

//    public Mono<ExchangeRate> findByCurrencyFromIdAndCurrencyToIdAndRateDate(Integer currencyFromId, Integer currencyToId, LocalDate exchangeRateDate) {
//        return exchangeRateRepository.findByCurrencyFromIdAndCurrencyToIdAndRateDate(currencyFromId, currencyToId, exchangeRateDate)
//                .switchIfEmpty(Mono.empty());
//    }

    public Mono<ExchangeRate> findByCurrencyFromIdAndRateDate(Integer currencyFromId, LocalDate exchangeRateDate) {
        return exchangeRateRepository.findExchangeRateByCurrencyFromIdAndRateDate(currencyFromId, exchangeRateDate)
                .switchIfEmpty(Mono.empty());
    }


//    public Mono<BigDecimal> getExchangeRate(Integer currencyFromId, Integer currencyToId, LocalDate exchangeRateDate) {
//        return exchangeRateRepository.findByCurrencyFromIdAndCurrencyToIdAndRateDate(currencyFromId, currencyToId, exchangeRateDate)
//                .map(ExchangeRate::getStandardRate); // Or choose the rate type based on client type (standard, premium, etc.)
//    }

    public Mono<BigDecimal> getForeignToRubExchangeRate(Integer foreignCurrencyFromId, LocalDate exchangeRateDate) {
        return exchangeRateRepository.findExchangeRateByCurrencyFromIdAndRateDate(foreignCurrencyFromId, exchangeRateDate)
                .map(ExchangeRate::getStandardRate); // Or choose the rate type based on client type (official, standard, premium, etc.)
    }

    public Mono<Void> saveExchangeRate(ExchangeRate exchangeRate) {
        return exchangeRateRepository.upsertExchangeRate(exchangeRate);
    }

    public Mono<Void> logSuccessfulRateUpdate(LocalDate date) {
        return rateUpdateLogRepository.saveLogForToday(date)
                .then(Mono.empty());
    }

    // New method: calculates the exchange rate between any two currencies.
    public Mono<BigDecimal> getExchangeRate(Integer currencyFromId, Integer currencyToId, LocalDate rateDate) {
        if (currencyFromId.equals(currencyToId)) {
            return Mono.just(BigDecimal.ONE);
        }
        boolean fromRUB = currencyFromId.equals(RUB_CURRENCY_ID);
        boolean toRUB = currencyToId.equals(RUB_CURRENCY_ID);

        if (fromRUB) {
            return fetchForeignToRubRate(currencyToId, rateDate)
                    .map(rate -> BigDecimal.ONE.divide(rate, 6, RoundingMode.HALF_UP));
        } else if (toRUB) {
            return fetchForeignToRubRate(currencyFromId, rateDate);
        } else {
            return Mono.zip(
                    fetchForeignToRubRate(currencyFromId, rateDate),
                    fetchForeignToRubRate(currencyToId, rateDate)
            ).map(tuple -> {
                BigDecimal fromRub = tuple.getT1();
                BigDecimal toRub = tuple.getT2();
                return fromRub.divide(toRub, 6, RoundingMode.HALF_UP);
            });
        }
    }

    // New helper method: fetches a foreign-to-RUB rate, triggering a rate fetch/save if needed.
    private Mono<BigDecimal> fetchForeignToRubRate(Integer foreignCurrencyId, LocalDate rateDate) {
        return getForeignToRubExchangeRate(foreignCurrencyId, rateDate)
                .switchIfEmpty(
                        fetchAndSaveRates(rateDate)
                                .then(getForeignToRubExchangeRate(foreignCurrencyId, rateDate))
                                .switchIfEmpty(Mono.error(new ExchangeRateRetrievalFailedException(
                                        "error.exchangeRate.retrievalFailed", foreignCurrencyId, rateDate)))
                );
    }

    // New method: converts an amount from one currency to another.
    public Mono<BigDecimal> convertAmount(Integer currencyFromId, Integer currencyToId, BigDecimal amount, LocalDate rateDate) {
        return getExchangeRate(currencyFromId, currencyToId, rateDate)
                .map(rate -> amount.multiply(rate).setScale(2, RoundingMode.HALF_UP))
                .flatMap(converted -> {
                    if (converted.compareTo(BigDecimal.valueOf(0.01)) < 0) {
                        return Mono.error(new ValidationException("validation.paymentAllocation.convertedAmount.min"));
                    }
                    return Mono.just(converted);
                });
    }

    public Mono<Boolean> checkIfRatesUpdatedForToday() {
        LocalDate today = LocalDate.now();
        return rateUpdateLogService.checkIfRatesUpdatedForToday(today);
    }

    public Mono<Void> fetchAndSaveRates(LocalDate date) {
        String formattedDate = date.format(DateTimeFormatter.ofPattern("dd/MM/yyyy"));
        String url = "https://www.cbr.ru/scripts/XML_daily.asp?date_req=" + formattedDate;

        return currencyService.findAll()
                .collectMap(CurrencyReadDto::code)  // Map by charCode for fast lookup
                .flatMapMany(enabledCurrencies -> fetchRatesFromUrl(url)
                        .flatMapMany(this::parseRatesFromXml)
                        .filter(rateInfo -> enabledCurrencies.containsKey(rateInfo.charCode()))  // Filter only enabled currencies
                        .flatMap(rateInfo -> saveExchangeRate(rateInfo, enabledCurrencies.get(rateInfo.charCode()), date))) // Pass currency ID
                .then(logSuccessfulRateUpdate(date))
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

    private Mono<Void> saveExchangeRate(RateInfo rateInfo, CurrencyReadDto currencyDto, LocalDate date) {
        BigDecimal officialRate = rateInfo.value().divide(rateInfo.nominal(), 6, RoundingMode.HALF_UP);

        ExchangeRate exchangeRate = new ExchangeRate();
        exchangeRate.setCurrencyFromId(currencyDto.id());
        exchangeRate.setCurrencyToId(RUB_CURRENCY_ID);
        exchangeRate.setOfficialRate(officialRate);
        exchangeRate.setStandardRate(officialRate);
        exchangeRate.setPremiumClientRate(officialRate);
        exchangeRate.setRateDate(date);
        return saveExchangeRate(exchangeRate);
    }

    /** Convenience wrapper that defaults to 8 parallel fetches. */
    public Mono<Void> fetchAndSaveRatesForPastYear() {
        return fetchAndSaveRatesForPastYear(8);
    }

    /**
     * Loads missing rates from (today-1 year) to today in **parallel**.
     *
     * @param concurrency how many different days to fetch simultaneously
     */
    public Mono<Void> fetchAndSaveRatesForPastYear(int concurrency) {
        LocalDate today      = LocalDate.now();
        LocalDate oneYearAgo = today.minusYears(1);
        long daysBetween     = ChronoUnit.DAYS.between(oneYearAgo, today);

        return Flux.range(0, (int) daysBetween + 1)
                .map(oneYearAgo::plusDays)
                .flatMap(date ->
                                exchangeRateRepository.findAnyRateByDate(date)
                                        .hasElements()
                                        .flatMap(exists -> exists
                                                ? Mono.fromRunnable(() ->
                                                log.info("Rates already exist for {}. Skipping.", date))
                                                : fetchAndSaveRates(date)
                                                .doFirst(() ->
                                                        log.info("Rates missing for {} – fetching…", date))
                                        ),
                        concurrency                                   // ⬅️ NEW: cap parallelism
                )
                .subscribeOn(Schedulers.boundedElastic())           // each branch may block
                .then()
                .doOnSubscribe(s -> log.info(
                        "Loading currency rates for the past year ({} parallel threads)…",
                        concurrency))
                .doOnTerminate(() -> log.info("Finished loading historical rates!"));
    }


}