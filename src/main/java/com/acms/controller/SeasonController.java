package com.acms.controller;

import com.acms.common.ApiResponse;
import com.acms.common.BaseController;
import com.acms.common.PagedResponse;
import com.acms.config.ApiTags;
import com.acms.dto.EnrolmentWindowResponse;
import com.acms.dto.SeasonRequest;
import com.acms.dto.SeasonResponse;
import com.acms.service.SeasonService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

/** Who may call what is defined in SecurityConfig: reading is open to every role, changes are Registrar-only. */
@Tag(name = ApiTags.SEASONS)
@RestController
@RequestMapping("/api/seasons")
@RequiredArgsConstructor
public class SeasonController extends BaseController {

    private final SeasonService seasonService;

    @Operation(
            summary = "Create a season",
            description = """
                    Create a season (dates, land limit, distribution day).

                    - **Permission:** `SEASON_MANAGE` (Registrar)
                    - **Success:** 201
                    - **Errors** (besides 401/403): 400 dates, 409 duplicate name
                    """)
    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public ApiResponse<SeasonResponse> create(@Valid @RequestBody SeasonRequest request, Authentication authentication) {
        return created(seasonService.create(request, currentUserId(authentication)));
    }

    @Operation(
            summary = "List seasons",
            description = """
                    List seasons.

                    - **Permission:** `SEASON_READ` (all roles)
                    - **Paged:** `page` (from 0), `size` (1-100, default 20), `sortBy` (one of id, name, startsOn, endsOn, createdAt; default startsOn), `direction` (asc or desc; default desc)
                    - **Success:** 200
                    """)
    @GetMapping
    public ApiResponse<PagedResponse<SeasonResponse>> list(@RequestParam(defaultValue = "0") int page,
                                             @RequestParam(defaultValue = "20") int size,
                                             @RequestParam(defaultValue = "startsOn") String sortBy,
                                             @RequestParam(defaultValue = "desc") String direction) {
        return ok(seasonService.findAll(page, size, sortBy, direction));
    }

    @Operation(
            summary = "One season with its window status",
            description = """
                    One season with its window status.

                    - **Permission:** `SEASON_READ` (all roles)
                    - **Success:** 200
                    - **Errors** (besides 401/403): 404
                    """)
    @GetMapping("/{id}")
    public ApiResponse<SeasonResponse> get(@PathVariable Long id) {
        return ok(seasonService.findById(id));
    }

    @Operation(
            summary = "Change a season before any window opened",
            description = """
                    Change a season before any window opened.

                    - **Permission:** `SEASON_MANAGE` (Registrar)
                    - **Success:** 200
                    - **Errors** (besides 401/403): 404, 400 dates, 409 a window was already opened / duplicate name
                    """)
    @PutMapping("/{id}")
    public ApiResponse<SeasonResponse> update(@PathVariable Long id, @Valid @RequestBody SeasonRequest request,
                                 Authentication authentication) {
        return ok(seasonService.update(id, request, currentUserId(authentication)));
    }

    @Operation(
            summary = "Open the enrolment window",
            description = """
                    Open the enrolment window.

                    - **Permission:** `SEASON_MANAGE` (Registrar)
                    - **Success:** 201
                    - **Errors** (besides 401/403): 404, 409 already open, 422 season ended
                    """)
    @PostMapping("/{id}/windows/open")
    @ResponseStatus(HttpStatus.CREATED)
    public ApiResponse<EnrolmentWindowResponse> openWindow(@PathVariable Long id, Authentication authentication) {
        return created(seasonService.openWindow(id, currentUserId(authentication)));
    }

    @Operation(
            summary = "Close the enrolment window",
            description = """
                    Close the enrolment window.

                    - **Permission:** `SEASON_MANAGE` (Registrar)
                    - **Success:** 200
                    - **Errors** (besides 401/403): 404, 409 not open
                    """)
    @PostMapping("/{id}/windows/close")
    public ApiResponse<EnrolmentWindowResponse> closeWindow(@PathVariable Long id, Authentication authentication) {
        return ok(seasonService.closeWindow(id, currentUserId(authentication)));
    }

    @Operation(
            summary = "History of the windows of a season",
            description = """
                    History of the windows of a season.

                    - **Permission:** `SEASON_READ` (all roles)
                    - **Paged:** `page` (from 0), `size` (1-100, default 20), `sortBy` (one of id, openedAt, closedAt; default openedAt), `direction` (asc or desc; default desc)
                    - **Success:** 200
                    - **Errors** (besides 401/403): 404
                    """)
    @GetMapping("/{id}/windows")
    public ApiResponse<PagedResponse<EnrolmentWindowResponse>> windows(@PathVariable Long id,
                                                         @RequestParam(defaultValue = "0") int page,
                                                         @RequestParam(defaultValue = "20") int size,
                                                         @RequestParam(defaultValue = "openedAt") String sortBy,
                                                         @RequestParam(defaultValue = "desc") String direction) {
        return ok(seasonService.windows(id, page, size, sortBy, direction));
    }

}
