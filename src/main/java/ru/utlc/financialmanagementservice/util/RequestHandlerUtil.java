package ru.utlc.financialmanagementservice.util;

import lombok.experimental.UtilityClass;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.BindingResult;
import org.springframework.web.servlet.support.ServletUriComponentsBuilder;
import reactor.core.publisher.Mono;
import ru.utlc.financialmanagementservice.response.Response;

import java.net.URI;
import java.util.function.Function;

@UtilityClass
public class RequestHandlerUtil {

    public static <T, R> Mono<ResponseEntity<Response>> handleRequest(
            BindingResult bindingResult,
            Function<T, Mono<R>> serviceMethod,
            Function<R, ResponseEntity<Response>> responseMapper,
            T dto) {

        if (bindingResult.hasFieldErrors()) {
            return ValidationErrorUtil.handleValidationErrors(bindingResult);
        }

        return serviceMethod.apply(dto)
                .map(responseMapper);
    }

    public static <R> ResponseEntity<Response> createdResponse(R dto, Object id) {
        URI location = ServletUriComponentsBuilder.fromCurrentRequest()
                .path("/{id}")
                .buildAndExpand(id)
                .toUri();
        return ResponseEntity.created(location).body(new Response(id));
    }

    public static <R> ResponseEntity<Response> updatedResponse(R dto) {
        return new ResponseEntity<>(new Response(dto), HttpStatus.OK);
    }
}
