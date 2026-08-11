package com.recruitment.ats.api;

import java.util.UUID;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.recruitment.ats.api.ApplicationDtos.ApplicationResponse;
import com.recruitment.ats.service.ApplicationService;

@RestController
@RequestMapping("/internal/applications")
public class InternalApplicationController {

    private final ApplicationService service;

    public InternalApplicationController(ApplicationService service) {
        this.service = service;
    }

    @GetMapping("/{id}")
    ApplicationResponse get(@PathVariable UUID id) {
        return service.getInternal(id);
    }
}
