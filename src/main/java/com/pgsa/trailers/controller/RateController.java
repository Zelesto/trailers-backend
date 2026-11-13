package com.pgsa.trailers.controller;

import com.pgsa.trailers.dto.billing.RateActivationRequest;
import com.pgsa.trailers.dto.billing.RateReferenceData;
import com.pgsa.trailers.dto.billing.RateRequest;
import com.pgsa.trailers.dto.billing.RateResponse;
import com.pgsa.trailers.service.billing.RateManagementService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/rates")
@RequiredArgsConstructor
public class RateController {

    private final RateManagementService rateManagementService;

    @GetMapping
    public List<RateResponse> list(@RequestParam(defaultValue = "false") boolean includeInactive) {
        return rateManagementService.list(includeInactive);
    }

    @GetMapping("/references")
    public RateReferenceData references() {
        return rateManagementService.getReferences();
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public RateResponse create(@Valid @RequestBody RateRequest request) {
        return rateManagementService.create(request);
    }

    @PutMapping("/{id}")
    public RateResponse update(@PathVariable Long id, @Valid @RequestBody RateRequest request) {
        return rateManagementService.update(id, request);
    }

    @PatchMapping("/{id}/active")
    public RateResponse setActive(
            @PathVariable Long id,
            @Valid @RequestBody RateActivationRequest request) {
        return rateManagementService.setActive(id, request.active());
    }
}
