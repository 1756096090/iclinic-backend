package com.iclinic.iclinicbackend.shared.exception;

import com.iclinic.iclinicbackend.modules.crm.exception.ChannelConnectionActiveNotFoundException;
import com.iclinic.iclinicbackend.modules.crm.exception.ChannelConnectionNotFoundException;
import com.iclinic.iclinicbackend.modules.crm.exception.ConversationNotFoundException;
import com.iclinic.iclinicbackend.modules.crm.exception.CrmContactNotFoundException;
import com.iclinic.iclinicbackend.modules.crm.exception.InvalidChannelConfigurationException;
import com.iclinic.iclinicbackend.modules.crm.exception.MessageContentInvalidException;
import com.iclinic.iclinicbackend.modules.crm.exception.TelegramWebhookException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.context.request.WebRequest;
import org.springframework.web.servlet.resource.NoResourceFoundException;

import java.time.LocalDateTime;
import java.util.stream.Collectors;

@ControllerAdvice
@Slf4j
public class GlobalExceptionHandler {

    @ExceptionHandler(CompanyNotFoundException.class)
    public ResponseEntity<ErrorResponse> handleCompanyNotFoundException(
            CompanyNotFoundException ex, WebRequest request) {
        log.warn("Company not found: {}", ex.getMessage());
        return build(HttpStatus.NOT_FOUND, ex.getMessage(), "Company not found", request);
    }

    @ExceptionHandler(BranchNotFoundException.class)
    public ResponseEntity<ErrorResponse> handleBranchNotFoundException(
            BranchNotFoundException ex, WebRequest request) {
        log.warn("Branch not found: {}", ex.getMessage());
        return build(HttpStatus.NOT_FOUND, ex.getMessage(), "Branch not found", request);
    }

    @ExceptionHandler(DuplicateCompanyException.class)
    public ResponseEntity<ErrorResponse> handleDuplicateCompanyException(
            DuplicateCompanyException ex, WebRequest request) {
        log.warn("Duplicate company: {}", ex.getMessage());
        return build(HttpStatus.CONFLICT, ex.getMessage(), "Duplicate tax ID", request);
    }

    @ExceptionHandler({
            ChannelConnectionNotFoundException.class,
            CrmContactNotFoundException.class,
            ConversationNotFoundException.class,
            ChannelConnectionActiveNotFoundException.class
    })
    public ResponseEntity<ErrorResponse> handleCrmNotFound(RuntimeException ex, WebRequest request) {
        log.warn("CRM not found: {}", ex.getMessage());
        return build(HttpStatus.NOT_FOUND, ex.getMessage(), ex.getMessage(), request);
    }

    @ExceptionHandler({InvalidChannelConfigurationException.class, MessageContentInvalidException.class})
    public ResponseEntity<ErrorResponse> handleCrmBadRequest(RuntimeException ex, WebRequest request) {
        log.warn("CRM bad request: {}", ex.getMessage());
        return build(HttpStatus.BAD_REQUEST, ex.getMessage(), ex.getMessage(), request);
    }

    @ExceptionHandler(TelegramWebhookException.class)
    public ResponseEntity<ErrorResponse> handleTelegramWebhookException(
            TelegramWebhookException ex, WebRequest request) {
        log.error("Telegram webhook error: {}", ex.getMessage());
        return build(HttpStatus.BAD_REQUEST, "Error en webhook de Telegram", ex.getMessage(), request);
    }

    @ExceptionHandler(IllegalArgumentException.class)
    public ResponseEntity<ErrorResponse> handleIllegalArgumentException(
            IllegalArgumentException ex, WebRequest request) {
        log.warn("Invalid argument: {}", ex.getMessage());
        return build(HttpStatus.BAD_REQUEST, ex.getMessage(), ex.getMessage(), request);
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ErrorResponse> handleValidationException(
            MethodArgumentNotValidException ex, WebRequest request) {
        log.warn("Validation error: {}", ex.getMessage());
        String details = ex.getBindingResult().getFieldErrors().stream()
                .map(fe -> fe.getField() + ": " + fe.getDefaultMessage())
                .collect(Collectors.joining(", "));
        return build(HttpStatus.BAD_REQUEST, "Validación fallida", details, request);
    }

    @ExceptionHandler(NoResourceFoundException.class)
    public ResponseEntity<ErrorResponse> handleNoResourceFoundException(
            NoResourceFoundException ex, WebRequest request) {
        log.debug("Static resource not found: {}", ex.getMessage());
        return build(HttpStatus.NOT_FOUND, "Recurso no encontrado", ex.getMessage(), request);
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<ErrorResponse> handleException(Exception ex, WebRequest request) {
        log.error("Unhandled exception", ex);
        return build(HttpStatus.INTERNAL_SERVER_ERROR, "Error interno del servidor", ex.getMessage(), request);
    }

    private ResponseEntity<ErrorResponse> build(HttpStatus status, String message, String details, WebRequest request) {
        ErrorResponse error = ErrorResponse.builder()
                .timestamp(LocalDateTime.now())
                .status(status.value())
                .message(message)
                .details(details)
                .path(request.getDescription(false).replace("uri=", ""))
                .build();
        return new ResponseEntity<>(error, status);
    }
}
