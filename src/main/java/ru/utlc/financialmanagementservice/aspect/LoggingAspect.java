package ru.utlc.financialmanagementservice.aspect;

import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Mono;
import reactor.core.publisher.Flux;

@Aspect
@Component
public class LoggingAspect {

    public static final String RESULT_FROM = "Result from {}: {}";
    private final Logger log = LoggerFactory.getLogger(this.getClass());

    @Around("within(ru.utlc.financialmanagementservice.controller..*) || within(ru.utlc.financialmanagementservice.service..*)")
    public Object logAround(ProceedingJoinPoint joinPoint) throws Throwable {
        var joinPointSignatureShortString = joinPoint.getSignature().toShortString();
        log.info("Request to {} with arguments: {}", joinPointSignatureShortString, joinPoint.getArgs());

        Object result = joinPoint.proceed();

        if (result instanceof Mono) {
            return ((Mono<?>) result)
                    .doOnSuccess(res -> log.info(RESULT_FROM, joinPointSignatureShortString, res))
                    .doOnError(e -> log.error("Exception in {}: {}", joinPointSignatureShortString, e.getMessage()));
        } else if (result instanceof Flux) {
            return ((Flux<?>) result)
                    .doOnNext(res -> log.info(RESULT_FROM, joinPointSignatureShortString, res))
                    .doOnError(e -> log.error("Exception in {}: {}", joinPointSignatureShortString, e.getMessage()));
        } else {
            log.info(RESULT_FROM, joinPointSignatureShortString, result);
            return result;
        }
    }
}
