package com.acms.controller;

import com.acms.common.ApiResponse;
import com.acms.common.BaseController;
import com.acms.dto.InspectionListResponse;
import com.acms.dto.InspectionRequest;
import com.acms.dto.InspectionResponse;
import com.acms.service.InspectionService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequiredArgsConstructor
public class InspectionController extends BaseController {

    private final InspectionService inspectionService;

    @PostMapping("/api/offerings/{offeringId}/inspections")
    @ResponseStatus(HttpStatus.CREATED)
    public ApiResponse<InspectionResponse> create(@PathVariable Long offeringId, @Valid @RequestBody InspectionRequest request) {
        return created(inspectionService.create(offeringId, request));
    }

    @GetMapping("/api/offerings/{offeringId}/inspections")
    public ApiResponse<InspectionListResponse> list(@PathVariable Long offeringId) {
        return ok(inspectionService.list(offeringId));
    }

    @PutMapping("/api/inspections/{id}")
    public ApiResponse<InspectionResponse> update(@PathVariable Long id, @Valid @RequestBody InspectionRequest request) {
        return ok(inspectionService.update(id, request));
    }

    @DeleteMapping("/api/inspections/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void delete(@PathVariable Long id) {
        inspectionService.delete(id);
    }
}
