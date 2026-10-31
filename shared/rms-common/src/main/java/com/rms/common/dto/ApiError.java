package com.rms.common.dto;

import java.time.Instant;
import java.util.List;

public class ApiError {
    private String errorCode;
    private String message;
    private List<Detail> details;
    private Instant timestamp;
    private String traceId;

    public ApiError() {
        this.timestamp = Instant.now();
    }

    public ApiError(String errorCode, String message, List<Detail> details, String traceId) {
        this.errorCode = errorCode;
        this.message = message;
        this.details = details;
        this.timestamp = Instant.now();
        this.traceId = traceId;
    }

    public String getErrorCode() { return errorCode; }
    public void setErrorCode(String errorCode) { this.errorCode = errorCode; }

    public String getMessage() { return message; }
    public void setMessage(String message) { this.message = message; }

    public List<Detail> getDetails() { return details; }
    public void setDetails(List<Detail> details) { this.details = details; }

    public Instant getTimestamp() { return timestamp; }
    public void setTimestamp(Instant timestamp) { this.timestamp = timestamp; }

    public String getTraceId() { return traceId; }
    public void setTraceId(String traceId) { this.traceId = traceId; }

    public static class Detail {
        private String field;
        private String issue;

        public Detail() {}

        public Detail(String field, String issue) {
            this.field = field;
            this.issue = issue;
        }

        public String getField() { return field; }
        public void setField(String field) { this.field = field; }

        public String getIssue() { return issue; }
        public void setIssue(String issue) { this.issue = issue; }
    }
}
