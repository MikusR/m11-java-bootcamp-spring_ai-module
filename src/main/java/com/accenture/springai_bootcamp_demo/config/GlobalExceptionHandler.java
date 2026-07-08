package com.accenture.springai_bootcamp_demo.config;

import com.accenture.springai_bootcamp_demo.client.OllamaException;
import com.accenture.springai_bootcamp_demo.client.OpenRouterException;
import com.accenture.springai_bootcamp_demo.service.ChatNotFoundException;
import com.accenture.springai_bootcamp_demo.service.learning.LearningTopicNotFoundException;
import com.accenture.springai_bootcamp_demo.service.learning.LearningWorkflowException;
import java.util.stream.Collectors;
import org.springframework.http.HttpStatus;
import org.springframework.http.ProblemDetail;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

/**
 * Translates domain and validation failures into RFC 7807 problem responses.
 */
@RestControllerAdvice
public class GlobalExceptionHandler {

    @ExceptionHandler(ChatNotFoundException.class)
    public ProblemDetail handleNotFound(ChatNotFoundException ex) {
        return ProblemDetail.forStatusAndDetail(HttpStatus.NOT_FOUND, ex.getMessage());
    }

    @ExceptionHandler(LearningTopicNotFoundException.class)
    public ProblemDetail handleLearningTopicNotFound(LearningTopicNotFoundException ex) {
        return ProblemDetail.forStatusAndDetail(HttpStatus.BAD_REQUEST, ex.getMessage());
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ProblemDetail handleValidation(MethodArgumentNotValidException ex) {
        String detail = ex.getBindingResult().getFieldErrors().stream()
                .map(error -> error.getField() + ": " + error.getDefaultMessage())
                .collect(Collectors.joining("; "));
        return ProblemDetail.forStatusAndDetail(HttpStatus.BAD_REQUEST, detail);
    }

    @ExceptionHandler(OpenRouterException.class)
    public ProblemDetail handleOpenRouter(OpenRouterException ex) {
        return ProblemDetail.forStatusAndDetail(HttpStatus.BAD_GATEWAY, ex.getMessage());
    }

    @ExceptionHandler(OllamaException.class)
    public ProblemDetail handleOllama(OllamaException ex) {
        return ProblemDetail.forStatusAndDetail(HttpStatus.BAD_GATEWAY, ex.getMessage());
    }

    @ExceptionHandler(LearningWorkflowException.class)
    public ProblemDetail handleLearningWorkflow(LearningWorkflowException ex) {
        return ProblemDetail.forStatusAndDetail(HttpStatus.BAD_GATEWAY, ex.getMessage());
    }
}
