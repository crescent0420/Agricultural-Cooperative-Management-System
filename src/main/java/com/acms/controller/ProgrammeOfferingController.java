package com.acms.controller;

import com.acms.dto.PageResponse;
import com.acms.dto.ProgrammeOfferingRequest;
import com.acms.dto.ProgrammeOfferingResponse;
import com.acms.dto.ProgrammeOfferingUpdateRequest;
import com.acms.service.ProgrammeOfferingService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
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

/** Who may call what is defined in SecurityConfig; which district each user sees is decided in the service. */
@RestController
@RequestMapping("/api/offerings")
@RequiredArgsConstructor
public class ProgrammeOfferingController {

    private final ProgrammeOfferingService offeringService;

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public ProgrammeOfferingResponse create(@Valid @RequestBody ProgrammeOfferingRequest request) {
        return offeringService.create(request);
    }

    @GetMapping
    public PageResponse<ProgrammeOfferingResponse> list(@RequestParam(required = false) Long seasonId,
                                                        @RequestParam(required = false) Long programmeId,
                                                        @RequestParam(required = false) Long districtId,
                                                        @RequestParam(defaultValue = "0") int page,
                                                        @RequestParam(defaultValue = "20") int size,
                                                        @RequestParam(defaultValue = "createdAt") String sortBy,
                                                        @RequestParam(defaultValue = "desc") String direction) {
        return offeringService.findAll(seasonId, programmeId, districtId, page, size, sortBy, direction);
    }

    /** A fixed path wins over /{id}, so "assigned-to-me" is never read as an id. */
    @GetMapping("/assigned-to-me")
    public PageResponse<ProgrammeOfferingResponse> assignedToMe(@RequestParam(required = false) Long seasonId,
                                                                @RequestParam(defaultValue = "0") int page,
                                                                @RequestParam(defaultValue = "20") int size,
                                                                @RequestParam(defaultValue = "createdAt") String sortBy,
                                                                @RequestParam(defaultValue = "desc") String direction) {
        return offeringService.findAssignedToCurrentOfficer(seasonId, page, size, sortBy, direction);
    }

    @GetMapping("/{id}")
    public ProgrammeOfferingResponse get(@PathVariable Long id) {
        return offeringService.findById(id);
    }

    @PutMapping("/{id}")
    public ProgrammeOfferingResponse update(@PathVariable Long id,
                                            @Valid @RequestBody ProgrammeOfferingUpdateRequest request) {
        return offeringService.update(id, request);
    }
}
