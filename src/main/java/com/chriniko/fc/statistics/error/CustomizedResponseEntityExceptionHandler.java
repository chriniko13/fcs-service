package com.chriniko.fc.statistics.error;

import lombok.extern.log4j.Log4j2;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.validation.FieldError;
import org.springframework.validation.ObjectError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.context.request.WebRequest;
import org.springframework.web.servlet.mvc.method.annotation.ResponseEntityExceptionHandler;

import java.lang.reflect.Field;
import java.util.Date;

@Log4j2

@ControllerAdvice
@RestController
public class CustomizedResponseEntityExceptionHandler extends ResponseEntityExceptionHandler {

    @ExceptionHandler(Exception.class)
    public final ResponseEntity<ErrorDetails> handleAllExceptions(Exception ex, WebRequest request) {

        log.error(">> internal unknown error occurred: " + ex.getMessage(), ex);

        ErrorDetails errorDetails = new ErrorDetails(
                new Date(),
                ex.getMessage(),
                request.getDescription(false)
        );

        return new ResponseEntity<>(errorDetails, HttpStatus.INTERNAL_SERVER_ERROR);
    }

    @ExceptionHandler(BusinessValidationException.class)
    public final ResponseEntity<ErrorDetails> handleBusinessValidationException(BusinessValidationException ex,
                                                                                WebRequest request) {

        log.error(">> handleBusinessValidationException error occurred: " + ex.getMessage());

        ErrorDetails errorDetails = new ErrorDetails(
                new Date(),
                ex.getMessage(),
                request.getDescription(false)
        );

        return new ResponseEntity<>(errorDetails, HttpStatus.BAD_REQUEST);
    }

    @ExceptionHandler(BusinessProcessingException.class)
    public final ResponseEntity<ErrorDetails> handleBusinessProcessingException(BusinessProcessingException ex,
                                                                                WebRequest request) {

        log.error(">> handleBusinessProcessingException error occurred: " + ex.getMessage());

        ErrorDetails errorDetails = new ErrorDetails(
                new Date(),
                ex.getMessage(),
                request.getDescription(false)
        );

        return new ResponseEntity<>(errorDetails, HttpStatus.INTERNAL_SERVER_ERROR);
    }

    @Override
    protected ResponseEntity<Object> handleMethodArgumentNotValid(MethodArgumentNotValidException ex,
                                                                  HttpHeaders headers,
                                                                  HttpStatus status,
                                                                  WebRequest request) {

        log.error(">> handleMethodArgumentNotValid error occurred: " + ex.getMessage());

        ErrorDetails errorDetails = new ErrorDetails(
                new Date(),
                "validation failed",
                request.getDescription(false)
        );

        Class<?> parameterType = ex.getParameter().getParameterType();
        String parameterTypeSimpleName = parameterType.getSimpleName();

        for (ObjectError objectError : ex.getBindingResult().getAllErrors()) {

            String objectName = objectError.getObjectName();
            FieldError fieldError = (FieldError) objectError;

            String fieldName = fieldError.getField();

            StringBuilder sb = new StringBuilder();
            if (!objectName.equalsIgnoreCase(parameterTypeSimpleName)) {
                sb.append(objectName).append(".").append(fieldName);
            } else { // Note: we do not want to include root name.
                sb.append(fieldName);
            }

            String defaultMessage = fieldError.getDefaultMessage();

            // Note: extract type of field name.
            try {
                Field declaredField = parameterType.getDeclaredField(fieldName);
                String declaredFieldTypeSimpleName = declaredField.getType().getSimpleName();
                defaultMessage += ", and type is: " + declaredFieldTypeSimpleName;
            } catch (Exception ignored) {
                log.error("could not extract field name type");
            }

            ValidationError validationError = new ValidationError(sb.toString(), defaultMessage);
            errorDetails.getValidationErrors().add(validationError);
        }

        return new ResponseEntity<>(errorDetails, HttpStatus.BAD_REQUEST);
    }

    @Override
    protected ResponseEntity<Object> handleHttpMessageNotReadable(HttpMessageNotReadableException ex,
                                                                  HttpHeaders headers,
                                                                  HttpStatus status,
                                                                  WebRequest request) {

        log.error(">> handleHttpMessageNotReadable error occurred: " + ex.getMessage());

        Throwable error = unwrap(ex.getCause());
        String msg = error.getMessage();

        if (msg.contains("\n")) {
            msg = msg.split("\n")[0];
        }

        ErrorDetails errorDetails = new ErrorDetails(
                new Date(),
                msg,
                request.getDescription(false)
        );

        return new ResponseEntity<>(errorDetails, HttpStatus.BAD_REQUEST);
    }

    // ---- internals ----

    private Throwable unwrap(Throwable input) {
        Throwable walker = input;
        while (walker.getCause() != null) {
            walker = walker.getCause();
        }
        return walker;
    }
}
