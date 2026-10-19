package com.acms.controller;
import com.acms.dto.FarmerRegistrationRequest;
import com.acms.dto.FarmerResponse;
import com.acms.dto.FarmerStatusChangeRequest;
import com.acms.dto.FarmerSummaryResponse;
import com.acms.dto.FarmerUpdateRequest;
import com.acms.dto.PageResponse;
import com.acms.entity.FarmerStatus;
import com.acms.service.FarmerService;
import jakarta.validation.Valid;
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
@RestController
@RequestMapping("/api/farmers")
public class FarmerController {
    private final FarmerService farmerService;
    public FarmerController(FarmerService farmerService) {
        this.farmerService = farmerService;
    }
    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public FarmerResponse register(
            @Valid @RequestBody FarmerRegistrationRequest request) {
        return farmerService.register(request);
    }
    @GetMapping
    public PageResponse<FarmerSummaryResponse> search(
            @RequestParam(required = false) String name,
            @RequestParam(required = false) String membershipNumber,
            @RequestParam(required = false) Long cooperativeId,
            @RequestParam(required = false) FarmerStatus status,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size,
            @RequestParam(defaultValue = "id") String sortBy,
            @RequestParam(defaultValue = "asc") String direction) {
        return farmerService.search(
                name,
                membershipNumber,
                cooperativeId,
                status,
                page,
                size,
                sortBy,
                direction);
    }
    @GetMapping("/{id}")
    public FarmerResponse get(@PathVariable Long id) {
        return farmerService.findById(id);
    }
    @PutMapping("/{id}")
    public FarmerResponse update(
            @PathVariable Long id,
            @Valid @RequestBody FarmerUpdateRequest request) {
        return farmerService.update(id, request);
    }
    @PatchMapping("/{id}/status")
    public FarmerResponse changeStatus(
            @PathVariable Long id,
            @Valid @RequestBody FarmerStatusChangeRequest request) {
        return farmerService.changeStatus(id, request);
    }
}
