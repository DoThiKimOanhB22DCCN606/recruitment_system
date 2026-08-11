package com.recruitment.ats.api;

import java.util.List;
import java.util.UUID;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import com.recruitment.ats.api.ApplicationDtos.ApplicationResponse;
import com.recruitment.ats.api.ApplicationDtos.HistoryResponse;
import com.recruitment.ats.api.ApplicationDtos.MoveStageRequest;
import com.recruitment.ats.api.ApplicationDtos.PageResponse;
import com.recruitment.ats.api.ApplicationDtos.StageColumn;
import com.recruitment.ats.api.ApplicationDtos.SubmitApplicationRequest;
import com.recruitment.ats.api.ApplicationDtos.WithdrawRequest;
import com.recruitment.ats.service.ApplicationService;

import jakarta.validation.Valid;

@RestController
@RequestMapping("/api/v1")
public class ApplicationController {

    private final ApplicationService service;

    public ApplicationController(ApplicationService service) {
        this.service = service;
    }

    @PostMapping("/applications")
    @ResponseStatus(HttpStatus.CREATED)
    ApplicationResponse submit(
            @Valid @RequestBody SubmitApplicationRequest request,
            @RequestHeader(value = "Idempotency-Key", required = false) String idempotencyKey) {
        return service.submit(request, idempotencyKey);
    }

    @GetMapping("/applications/{id}")
    ApplicationResponse get(@PathVariable UUID id) {
        return service.get(id);
    }

    @GetMapping("/employer/jobs/{jobId}/ats")
    List<StageColumn> kanban(@PathVariable UUID jobId) {
        return service.kanban(jobId);
    }

    @GetMapping("/employer/jobs/{jobId}/ats/stages/{stageId}/applications")
    PageResponse<ApplicationResponse> stageApplications(
            @PathVariable UUID jobId,
            @PathVariable UUID stageId,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "50") int size) {
        return service.stageApplications(jobId, stageId, page, size);
    }

    @PatchMapping("/applications/{id}/stage")
    ApplicationResponse move(
            @PathVariable UUID id,
            @Valid @RequestBody MoveStageRequest request,
            @RequestHeader(value = "X-Client-IP", defaultValue = "unknown") String clientIp,
            @RequestHeader(value = "X-Client-Device", defaultValue = "unknown") String clientDevice) {
        return service.moveStage(id, request, clientIp, clientDevice);
    }

    @PostMapping("/applications/{id}/withdraw")
    ApplicationResponse withdraw(
            @PathVariable UUID id,
            @Valid @RequestBody(required = false) WithdrawRequest request,
            @RequestHeader(value = "X-Client-IP", defaultValue = "unknown") String clientIp,
            @RequestHeader(value = "X-Client-Device", defaultValue = "unknown") String clientDevice) {
        return service.withdraw(id, request == null ? null : request.reason(),
                clientIp, clientDevice);
    }

    @GetMapping("/applications/{id}/stage-history")
    List<HistoryResponse> history(@PathVariable UUID id) {
        return service.stageHistory(id);
    }

    @GetMapping("/candidate/applications")
    PageResponse<ApplicationResponse> candidateApplications(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        return service.candidateApplications(page, size);
    }
}
