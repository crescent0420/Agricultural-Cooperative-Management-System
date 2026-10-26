package com.acms.controller;

import com.acms.dto.CooperativeRenameRequest;
import com.acms.dto.CooperativeRequest;
import com.acms.dto.CooperativeResponse;
import com.acms.dto.PageResponse;
import com.acms.service.CooperativeService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/cooperatives")
public class CooperativeController {

    private final CooperativeService cooperativeService;

    public CooperativeController(CooperativeService cooperativeService) {
        this.cooperativeService = cooperativeService;
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public CooperativeResponse create(
            @Valid @RequestBody CooperativeRequest request) {
        return cooperativeService.create(request);
    }

    @GetMapping
    public PageResponse<CooperativeResponse> list(
            @RequestParam(required = false) Long districtId,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size,
            @RequestParam(defaultValue = "name") String sortBy,
            @RequestParam(defaultValue = "asc") String direction) {

        return cooperativeService.findAll(
                districtId, page, size, sortBy, direction);
    }

    @GetMapping("/{id}")
    public CooperativeResponse get(@PathVariable Long id) {
        return cooperativeService.findById(id);
    }

    @PutMapping("/{id}")
    public CooperativeResponse rename(
            @PathVariable Long id,
            @Valid @RequestBody CooperativeRenameRequest request) {
        return cooperativeService.rename(id, request);
    }
}
