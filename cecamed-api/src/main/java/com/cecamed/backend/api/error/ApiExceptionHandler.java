package com.cecamed.backend.api.error;

import com.cecamed.backend.application.shared.ResourceNotFoundException;
import com.cecamed.backend.domain.record.MedicalRecordNotFoundException;
import com.cecamed.backend.domain.consultation.ConsultationNotFoundException;
import com.cecamed.backend.domain.identity.exception.AccountInactiveException;
import com.cecamed.backend.domain.identity.exception.AccountLockedException;
import com.cecamed.backend.domain.identity.exception.InvalidCredentialsException;
import com.cecamed.backend.domain.identity.exception.InvalidTokenException;
import com.cecamed.backend.domain.identity.exception.PasswordPolicyException;
import com.cecamed.backend.domain.identity.exception.TokenExpiredException;
import com.cecamed.backend.domain.identity.exception.UserNotFoundException;
import com.cecamed.backend.domain.identity.exception.UsernameAlreadyExistsException;
import com.cecamed.backend.domain.shared.BusinessRuleException;
import java.net.URI;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.HttpStatusCode;
import org.springframework.http.ProblemDetail;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.context.request.ServletWebRequest;
import org.springframework.web.context.request.WebRequest;
import org.springframework.web.servlet.mvc.method.annotation.ResponseEntityExceptionHandler;

@RestControllerAdvice
public class ApiExceptionHandler extends ResponseEntityExceptionHandler {

    private static final Logger log = LoggerFactory.getLogger(ApiExceptionHandler.class);

    @ExceptionHandler(InvalidCredentialsException.class)
    ProblemDetail handleInvalidCredentials(InvalidCredentialsException exception, WebRequest request) {
        return problem(HttpStatus.UNAUTHORIZED, "INVALID_CREDENTIALS", exception.getMessage(), request);
    }

    @ExceptionHandler(AccountLockedException.class)
    ProblemDetail handleAccountLocked(AccountLockedException exception, WebRequest request) {
        return problem(HttpStatus.UNAUTHORIZED, "ACCOUNT_LOCKED", exception.getMessage(), request);
    }

    @ExceptionHandler(AccountInactiveException.class)
    ProblemDetail handleAccountInactive(AccountInactiveException exception, WebRequest request) {
        return problem(HttpStatus.UNAUTHORIZED, "ACCOUNT_INACTIVE", exception.getMessage(), request);
    }

    @ExceptionHandler(TokenExpiredException.class)
    ProblemDetail handleTokenExpired(TokenExpiredException exception, WebRequest request) {
        return problem(HttpStatus.UNAUTHORIZED, "TOKEN_EXPIRED", exception.getMessage(), request);
    }

    @ExceptionHandler(InvalidTokenException.class)
    ProblemDetail handleInvalidToken(InvalidTokenException exception, WebRequest request) {
        return problem(HttpStatus.UNAUTHORIZED, "INVALID_TOKEN", exception.getMessage(), request);
    }

    @ExceptionHandler(UsernameAlreadyExistsException.class)
    ProblemDetail handleUsernameAlreadyExists(UsernameAlreadyExistsException exception, WebRequest request) {
        return problem(HttpStatus.CONFLICT, "USERNAME_ALREADY_EXISTS", exception.getMessage(), request);
    }

    @ExceptionHandler(UserNotFoundException.class)
    ProblemDetail handleUserNotFound(UserNotFoundException exception, WebRequest request) {
        return problem(HttpStatus.NOT_FOUND, "USER_NOT_FOUND", exception.getMessage(), request);
    }

    @ExceptionHandler(PasswordPolicyException.class)
    ProblemDetail handlePasswordPolicy(PasswordPolicyException exception, WebRequest request) {
        return problem(HttpStatus.BAD_REQUEST, "PASSWORD_POLICY_VIOLATION", exception.getMessage(), request);
    }

    @ExceptionHandler(AccessDeniedException.class)
    ProblemDetail handleAccessDenied(AccessDeniedException exception, WebRequest request) {
        return problem(HttpStatus.FORBIDDEN, "ACCESS_DENIED", "No tienes permiso para realizar esta operación.", request);
    }

    @ExceptionHandler(ResourceNotFoundException.class)
    ProblemDetail handleNotFound(ResourceNotFoundException exception, WebRequest request) {
        return problem(HttpStatus.NOT_FOUND, "RESOURCE_NOT_FOUND", exception.getMessage(), request);
    }

    @ExceptionHandler(MedicalRecordNotFoundException.class)
    ProblemDetail handleMedicalRecordNotFound(MedicalRecordNotFoundException exception, WebRequest request) {
        return problem(HttpStatus.NOT_FOUND, "MEDICAL_RECORD_NOT_FOUND", exception.getMessage(), request);
    }

    @ExceptionHandler(ConsultationNotFoundException.class)
    ProblemDetail handleConsultationNotFound(ConsultationNotFoundException exception, WebRequest request) {
        return problem(HttpStatus.NOT_FOUND, "CONSULTATION_NOT_FOUND", exception.getMessage(), request);
    }

    @ExceptionHandler(BusinessRuleException.class)
    ProblemDetail handleBusinessRule(BusinessRuleException exception, WebRequest request) {
        return problem(HttpStatus.CONFLICT, exception.getCode(), exception.getMessage(), request);
    }

    @Override
    protected ResponseEntity<Object> handleMethodArgumentNotValid(
            MethodArgumentNotValidException exception, HttpHeaders headers,
            HttpStatusCode status, WebRequest request) {
        var problem = problem(status, "VALIDATION_ERROR", "Revisa los campos enviados.", request);
        problem.setProperty("errors", exception.getBindingResult().getFieldErrors().stream()
                .map(error -> new FieldError(error.getField(), error.getDefaultMessage()))
                .toList());
        return new ResponseEntity<>(problem, headers, status);
    }

    @Override
    protected ResponseEntity<Object> handleExceptionInternal(Exception exception, Object body,
            HttpHeaders headers, HttpStatusCode status, WebRequest request) {
        var problem = problem(status, "HTTP_" + status.value(),
                "No se pudo procesar la solicitud.", request);
        return new ResponseEntity<>(problem, headers, status);
    }

    @ExceptionHandler(Exception.class)
    ProblemDetail handleUnexpected(Exception exception, WebRequest request) {
        log.error("Error inesperado al procesar una solicitud", exception);
        return problem(HttpStatus.INTERNAL_SERVER_ERROR, "INTERNAL_ERROR",
                "Ocurrió un error interno.", request);
    }

    private static ProblemDetail problem(HttpStatusCode status, String code,
            String detail, WebRequest request) {
        var problem = ProblemDetail.forStatusAndDetail(status, detail);
        problem.setProperty("code", code);
        if (request instanceof ServletWebRequest servletRequest) {
            problem.setInstance(URI.create(servletRequest.getRequest().getRequestURI()));
        }
        return problem;
    }

    public record FieldError(String field, String message) {}
}
