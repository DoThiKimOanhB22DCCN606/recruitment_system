package com.recruitment.interview.api;

import java.util.List;
import java.util.UUID;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import com.recruitment.interview.api.InterviewDtos.CreateInterviewRequest;
import com.recruitment.interview.api.InterviewDtos.InterviewResponse;
import com.recruitment.interview.api.InterviewDtos.UpdateInterviewRequest;
import com.recruitment.interview.service.InterviewService;

import jakarta.validation.Valid;

@RestController
@RequestMapping("/api/v1")
public class InterviewController {

    private final InterviewService service;

    public InterviewController(InterviewService service) {
        this.service = service;
    }

    @PostMapping("/interviews")
    @ResponseStatus(HttpStatus.CREATED)
    InterviewResponse create(@Valid @RequestBody CreateInterviewRequest request) {
        return service.create(request);
    }

    @GetMapping("/interviews/{id}")
    InterviewResponse get(@PathVariable UUID id) {
        return service.get(id);
    }

    @GetMapping("/applications/{applicationId}/interviews")
    List<InterviewResponse> list(@PathVariable UUID applicationId) {
        return service.listByApplication(applicationId);
    }

    @PatchMapping("/interviews/{id}")
    InterviewResponse update(
            @PathVariable UUID id,
            @Valid @RequestBody UpdateInterviewRequest request) {
        return service.update(id, request);
    }

    @PatchMapping("/interviews/{id}/cancel")
    InterviewResponse cancel(@PathVariable UUID id) {
        return service.cancel(id);
    }
}
