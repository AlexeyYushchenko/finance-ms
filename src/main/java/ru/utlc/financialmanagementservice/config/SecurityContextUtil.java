package ru.utlc.financialmanagementservice.config;

import lombok.experimental.UtilityClass;
import org.springframework.security.core.context.ReactiveSecurityContextHolder;
import reactor.core.publisher.Mono;

@UtilityClass
public class SecurityContextUtil {

    public static Mono<String> getCurrentAuditor() {
        return ReactiveSecurityContextHolder.getContext()
                .map(securityContext -> securityContext.getAuthentication().getName())
                .defaultIfEmpty("System");
    }
}
