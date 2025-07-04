package ru.utlc.util;

import lombok.experimental.UtilityClass;
import org.springframework.context.support.DefaultMessageSourceResolvable;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.BindingResult;
import reactor.core.publisher.Mono;
import ru.utlc.response.Response;

import java.util.List;

@UtilityClass
public class ValidationErrorUtil {

    public static Mono<ResponseEntity<Response>> handleValidationErrors(BindingResult bindingResult) {
        List<String> errorMessages = bindingResult.getFieldErrors().stream()
                .map(DefaultMessageSourceResolvable::getDefaultMessage)
                .toList();

        return Mono.just(ResponseEntity.status(HttpStatus.BAD_REQUEST).body(new Response(errorMessages, null)));
    }
}
