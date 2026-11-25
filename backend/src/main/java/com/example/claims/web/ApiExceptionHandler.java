package com.example.claims.web;

import com.example.claims.claim.BusinessRuleException;
import com.example.claims.claim.ForbiddenException;
import com.example.claims.claim.InvalidTransitionException;
import com.example.claims.claim.NotFoundException;
import org.springframework.http.HttpStatus;
import org.springframework.http.HttpStatusCode;
import org.springframework.http.ProblemDetail;
import org.springframework.orm.ObjectOptimisticLockingFailureException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

/** Maps domain errors to HTTP status codes, returned as RFC 9457 problem details. */
@RestControllerAdvice
public class ApiExceptionHandler {

    @ExceptionHandler(NotFoundException.class)
    ProblemDetail notFound(NotFoundException e) {
        return ProblemDetail.forStatusAndDetail(HttpStatus.NOT_FOUND, e.getMessage());
    }

    @ExceptionHandler(ForbiddenException.class)
    ProblemDetail forbidden(ForbiddenException e) {
        return ProblemDetail.forStatusAndDetail(HttpStatus.FORBIDDEN, e.getMessage());
    }

    @ExceptionHandler(InvalidTransitionException.class)
    ProblemDetail invalidTransition(InvalidTransitionException e) {
        return ProblemDetail.forStatusAndDetail(HttpStatus.CONFLICT, e.getMessage());
    }

    @ExceptionHandler(BusinessRuleException.class)
    ProblemDetail businessRule(BusinessRuleException e) {
        return ProblemDetail.forStatusAndDetail(HttpStatusCode.valueOf(422), e.getMessage());
    }

    @ExceptionHandler(ObjectOptimisticLockingFailureException.class)
    ProblemDetail concurrentEdit(ObjectOptimisticLockingFailureException e) {
        return ProblemDetail.forStatusAndDetail(HttpStatus.CONFLICT,
                "This claim was changed by someone else. Reload and try again.");
    }
}
