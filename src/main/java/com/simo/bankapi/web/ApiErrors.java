package com.simo.bankapi.web;

import com.fasterxml.jackson.databind.JsonMappingException;
import com.fasterxml.jackson.databind.exc.MismatchedInputException;
import com.simo.bankapi.account.AccountNotFoundException;
import com.simo.bankapi.account.InsufficientFundsException;
import java.util.Arrays;
import java.util.Map;
import java.util.stream.Collectors;
import org.springframework.http.HttpStatus;
import org.springframework.http.ProblemDetail;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

@RestControllerAdvice
public class ApiErrors {

    @ExceptionHandler(AccountNotFoundException.class)
    ProblemDetail notFound(AccountNotFoundException e) {
        return ProblemDetail.forStatusAndDetail(HttpStatus.NOT_FOUND, e.getMessage());
    }

    @ExceptionHandler(InsufficientFundsException.class)
    ProblemDetail insufficient(InsufficientFundsException e) {
        return ProblemDetail.forStatusAndDetail(HttpStatus.UNPROCESSABLE_ENTITY, e.getMessage());
    }

    @ExceptionHandler(IllegalArgumentException.class)
    ProblemDetail badRequest(IllegalArgumentException e) {
        return ProblemDetail.forStatusAndDetail(HttpStatus.BAD_REQUEST, e.getMessage());
    }

    /**
     * A body Jackson couldn't read. When the JSON itself is fine but one field holds the wrong
     * kind of value (an unknown account type, text where a number belongs), the client is told
     * which field, in the same {@code fields} shape as other validation errors.
     */
    @ExceptionHandler(HttpMessageNotReadableException.class)
    ProblemDetail unreadable(HttpMessageNotReadableException e) {
        if (e.getCause() instanceof MismatchedInputException m) {
            String field = fieldPath(m);
            if (!field.isEmpty()) {
                return fieldErrors(Map.of(field, describe(m.getTargetType())));
            }
        }
        return ProblemDetail.forStatusAndDetail(HttpStatus.BAD_REQUEST, "The request body isn't valid JSON for this endpoint");
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    ProblemDetail invalid(MethodArgumentNotValidException e) {
        Map<String, String> fields = e.getBindingResult().getFieldErrors().stream()
                .collect(Collectors.toMap(f -> f.getField(), f -> String.valueOf(f.getDefaultMessage()), (a, b) -> a));
        return fieldErrors(fields);
    }

    private static ProblemDetail fieldErrors(Map<String, String> fields) {
        ProblemDetail pd = ProblemDetail.forStatusAndDetail(HttpStatus.BAD_REQUEST, "Some fields are invalid");
        pd.setProperty("fields", fields);
        return pd;
    }

    private static String fieldPath(JsonMappingException e) {
        return e.getPath().stream()
                .map(r -> r.getFieldName() != null ? r.getFieldName() : String.valueOf(r.getIndex()))
                .collect(Collectors.joining("."));
    }

    private static String describe(Class<?> target) {
        if (target != null && target.isEnum()) {
            return "must be one of " + Arrays.stream(target.getEnumConstants())
                    .map(String::valueOf)
                    .collect(Collectors.joining(", "));
        }
        if (target != null && Number.class.isAssignableFrom(target)) {
            return "must be a number";
        }
        return "has the wrong type";
    }
}
