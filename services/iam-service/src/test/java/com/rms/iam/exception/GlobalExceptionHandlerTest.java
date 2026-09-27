package com.rms.iam.exception;

import com.rms.common.dto.ApiError;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.mock.web.MockHttpServletRequest;

import static org.assertj.core.api.Assertions.assertThat;

class GlobalExceptionHandlerTest {

    private final GlobalExceptionHandler handler = new GlobalExceptionHandler();

    @Test
    void handleIllegalArgumentReturnsBadRequestWithApiError() {
        MockHttpServletRequest request = new MockHttpServletRequest();
        request.addHeader("X-Correlation-Id", "test-corr-id");

        IllegalArgumentException ex = new IllegalArgumentException("Invalid password parameters");
        ResponseEntity<ApiError> response = handler.handleIllegalArgumentException(ex, request);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().getErrorCode()).isEqualTo("ERR_VALIDATION_FAILED");
        assertThat(response.getBody().getMessage()).isEqualTo("Invalid password parameters");
        assertThat(response.getBody().getTraceId()).isEqualTo("test-corr-id");
    }
}
