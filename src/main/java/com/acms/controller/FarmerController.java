package com.acms.controller;
import com.acms.common.ApiResponse;
import com.acms.common.BaseController;
import com.acms.common.PagedResponse;
import com.acms.config.ApiTags;
import com.acms.dto.FarmerRegistrationRequest;
import com.acms.dto.FarmerResponse;
import com.acms.dto.FarmerStatusChangeRequest;
import com.acms.dto.FarmerSummaryResponse;
import com.acms.dto.FarmerUpdateRequest;
import com.acms.enums.FarmerStatus;
import com.acms.service.FarmerService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
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
@Tag(name = ApiTags.FARMERS)
@RestController
@RequestMapping("/api/farmers")
public class FarmerController extends BaseController {
    private final FarmerService farmerService;
    public FarmerController(FarmerService farmerService) {
        this.farmerService = farmerService;
    }
    @Operation(
            summary = "Register a farmer",
            description = """
                    Register a farmer; gives the membership number.

                    - **Permission:** `FARMER_MANAGE` (Registrar)
                    - **Success:** 201
                    - **Errors** (besides 401/403): 400, 409 duplicate national id
                    """)
    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public ApiResponse<FarmerResponse> register(
            @Valid @RequestBody FarmerRegistrationRequest request) {
        return created(farmerService.register(request));
    }
    @Operation(
            summary = "Search farmers",
            description = """
                    Search farmers by name, membership number, cooperative, status; paginated and sortable.

                    - **Permission:** `FARMER_LIST` (Field Officer, Agronomist, Registrar, Finance, Admin)
                    - **Scope:** results are limited to the caller's own district, offerings or records
                    - **Paged:** `page` (from 0), `size` (1-100, default 20), `sortBy` (one of id, fullName, membershipNumber, status, createdAt; default id), `direction` (asc or desc; default asc)
                    - **Success:** 200
                    - **Errors** (besides 401/403): 400
                    """)
    @GetMapping
    public ApiResponse<PagedResponse<FarmerSummaryResponse>> search(
            @RequestParam(required = false) String name,
            @RequestParam(required = false) String membershipNumber,
            @RequestParam(required = false) Long cooperativeId,
            @RequestParam(required = false) FarmerStatus status,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size,
            @RequestParam(defaultValue = "id") String sortBy,
            @RequestParam(defaultValue = "asc") String direction) {
        return ok(farmerService.search(
                name,
                membershipNumber,
                cooperativeId,
                status,
                page,
                size,
                sortBy,
                direction));
    }
    @Operation(
            summary = "One farmer",
            description = """
                    One farmer (district staff: own district; farmer: themself).

                    - **Permission:** `FARMER_READ` (Farmer, Field Officer, Agronomist, Registrar)
                    - **Scope:** only the caller's own district, offerings or records; any other record gives 403
                    - **Success:** 200
                    - **Errors** (besides 401/403): 404
                    """)
    @GetMapping("/{id}")
    public ApiResponse<FarmerResponse> get(@PathVariable Long id) {
        return ok(farmerService.findById(id));
    }
    @Operation(
            summary = "Update a farmer's name and phone",
            description = """
                    Correct name and phone.

                    - **Permission:** `FARMER_MANAGE` (Registrar)
                    - **Success:** 200
                    - **Errors** (besides 401/403): 400, 404
                    """)
    @PutMapping("/{id}")
    public ApiResponse<FarmerResponse> update(
            @PathVariable Long id,
            @Valid @RequestBody FarmerUpdateRequest request) {
        return ok(farmerService.update(id, request));
    }
    @Operation(
            summary = "Change a farmer's status",
            description = """
                    Change status (Active, Suspended, Withdrawn, Retired).

                    - **Permission:** `FARMER_STATUS_CHANGE` (Registrar)
                    - **Success:** 200
                    - **Errors** (besides 401/403): 400, 404, 422 transition not allowed (e.g. Retired → Active)
                    """)
    @PatchMapping("/{id}/status")
    public ApiResponse<FarmerResponse> changeStatus(
            @PathVariable Long id,
            @Valid @RequestBody FarmerStatusChangeRequest request) {
        return ok(farmerService.changeStatus(id, request));
    }
}
