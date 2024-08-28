/*******************************************************************************
 * COPYRIGHT Ericsson 2021
 *
 *
 *
 * The copyright to the computer program(s) herein is the property of
 *
 * Ericsson Inc. The programs may be used and/or copied only with written
 *
 * permission from Ericsson Inc. or in accordance with the terms and
 *
 * conditions stipulated in the agreement/contract under which the
 *
 * program(s) have been supplied.
 ******************************************************************************/

package com.ericsson.oss.air.exception;

import com.ericsson.oss.air.api.generated.model.EricOssAssuranceIndexerProblem;
import com.fasterxml.jackson.databind.exc.MismatchedInputException;
import com.fasterxml.jackson.databind.exc.ValueInstantiationException;
import org.springframework.web.servlet.config.annotation.EnableWebMvc;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.http.ResponseEntity;
import org.springframework.http.HttpStatus;
import jakarta.validation.ConstraintViolationException;
import lombok.extern.slf4j.Slf4j;
import java.math.BigDecimal;

@RestControllerAdvice
@EnableWebMvc
@Slf4j
public class IndexerRestExceptionHandler {

  public static final String ERROR_HEADER_MESSAGE = "HTTP ERROR - 400 BAD REQUEST: %s";
  public static final String PARSING_ERROR_MESSAGE = "%s. Found when parsing %s";
  public static final String NOT_FOUND_ERROR_HEADER_MESSAGE = "HTTP ERROR - 404 NOT FOUND: %s";

  @ExceptionHandler({ConstraintViolationException.class})
  protected ResponseEntity<EricOssAssuranceIndexerProblem> handleConstraintViolationException(ConstraintViolationException e) {
    log.info(String.format(ERROR_HEADER_MESSAGE, e.getMessage()));
    return createResponse(e.getMessage(), HttpStatus.BAD_REQUEST);
  }

  @ExceptionHandler({MismatchedInputException.class})
  protected ResponseEntity<EricOssAssuranceIndexerProblem> handleMismatchedInputException(MismatchedInputException e) {
    String errorResponse = String.format(PARSING_ERROR_MESSAGE, e.getOriginalMessage(), e.getPathReference());
    log.info(String.format(ERROR_HEADER_MESSAGE, errorResponse));
    return createResponse(errorResponse, HttpStatus.BAD_REQUEST);
  }

  @ExceptionHandler({ValueInstantiationException.class})
  protected ResponseEntity<EricOssAssuranceIndexerProblem> handleValueInstantiationException(ValueInstantiationException e) {
    String errorResponse = String.format(PARSING_ERROR_MESSAGE, e.getOriginalMessage(), e.getPathReference());
    log.info(String.format(ERROR_HEADER_MESSAGE, errorResponse));
    return createResponse(errorResponse, HttpStatus.BAD_REQUEST);
  }

@ExceptionHandler({HttpNotFoundException.class})
protected ResponseEntity<EricOssAssuranceIndexerProblem> handleHttpNotFoundException(HttpNotFoundException e) {
  log.info(String.format(NOT_FOUND_ERROR_HEADER_MESSAGE, e.getMessage()));
  return createResponse(e.getMessage(), HttpStatus.NOT_FOUND);
}

  private ResponseEntity<EricOssAssuranceIndexerProblem> createResponse(String errorMessage, HttpStatus status) {
    return new ResponseEntity<>(new EricOssAssuranceIndexerProblem()
            .title(status.getReasonPhrase())
            .status(BigDecimal.valueOf(status.value()))
            .detail(errorMessage)
            .type("about:blank"), status);
  }
}
