package com.acms.controller;

import org.springframework.web.bind.annotation.RequestParam;
import com.acms.dto.CropProgrammeRenameRequest;
import com.acms.dto.CropProgrammeRequest;
import com.acms.dto.CropProgrammeResponse;
import com.acms.dto.PageResponse;
import com.acms.dto.PrerequisiteRequest;
import com.acms.service.CropProgrammeService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.DeleteMapping;
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
@RequestMapping("/api/programmes")
public class CropProgrammeController {

    private final CropProgrammeService programmeService;

    public CropProgrammeController(CropProgrammeService programmeService) {
        this.programmeService = programmeService;
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public CropProgrammeResponse create(
            @Valid @RequestBody CropProgrammeRequest request) {
        return programmeService.create(request);
    }

    @GetMapping
    public PageResponse<CropProgrammeResponse> list(@RequestParam(required = false) Long districtId,
                                                    @RequestParam(defaultValue = "0") int page,
                                                    @RequestParam(defaultValue = "20") int size,
                                                    @RequestParam(defaultValue = "name") String sortBy,
                                                    @RequestParam(defaultValue = "asc") String direction) {
        return programmeService.findAll(districtId, page, size, sortBy, direction);
    }

    @GetMapping("/{id}")
    public CropProgrammeResponse get(@PathVariable Long id) {
        return programmeService.findById(id);
    }

    @PutMapping("/{id}")
    public CropProgrammeResponse rename(
            @PathVariable Long id,
            @Valid @RequestBody CropProgrammeRenameRequest request) {
        return programmeService.rename(id, request);
    }

    @PostMapping("/{id}/prerequisites")
    @ResponseStatus(HttpStatus.CREATED)
    public CropProgrammeResponse addPrerequisite(
            @PathVariable Long id,
            @Valid @RequestBody PrerequisiteRequest request) {
        return programmeService.addPrerequisite(id, request);
    }

    @DeleteMapping("/{id}/prerequisites/{trainingId}")
    public CropProgrammeResponse removePrerequisite(
            @PathVariable Long id,
            @PathVariable Long trainingId) {
        return programmeService.removePrerequisite(id, trainingId);
    }
}
