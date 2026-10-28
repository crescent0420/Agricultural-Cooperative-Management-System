package com.acms.controller;

import com.acms.common.ApiResponse;
import com.acms.common.BaseController;
import com.acms.common.PagedResponse;
import com.acms.dto.EnrolmentWindowResponse;
import com.acms.dto.SeasonRequest;
import com.acms.dto.SeasonResponse;
import com.acms.service.SeasonService;
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
@RestController
@RequestMapping("/api/seasons")
@RequiredArgsConstructor
public class SeasonController extends BaseController {

    private final SeasonService seasonService;

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public ApiResponse<SeasonResponse> create(@Valid @RequestBody SeasonRequest request, Authentication authentication) {
        return created(seasonService.create(request, currentUserId(authentication)));
    }

    @GetMapping
    public ApiResponse<PagedResponse<SeasonResponse>> list(@RequestParam(defaultValue = "0") int page,
                                             @RequestParam(defaultValue = "20") int size,
                                             @RequestParam(defaultValue = "startsOn") String sortBy,
                                             @RequestParam(defaultValue = "desc") String direction) {
        return ok(seasonService.findAll(page, size, sortBy, direction));
    }

    @GetMapping("/{id}")
    public ApiResponse<SeasonResponse> get(@PathVariable Long id) {
        return ok(seasonService.findById(id));
    }

    @PutMapping("/{id}")
    public ApiResponse<SeasonResponse> update(@PathVariable Long id, @Valid @RequestBody SeasonRequest request,
                                 Authentication authentication) {
        return ok(seasonService.update(id, request, currentUserId(authentication)));
    }

    @PostMapping("/{id}/windows/open")
    @ResponseStatus(HttpStatus.CREATED)
    public ApiResponse<EnrolmentWindowResponse> openWindow(@PathVariable Long id, Authentication authentication) {
        return created(seasonService.openWindow(id, currentUserId(authentication)));
    }

    @PostMapping("/{id}/windows/close")
    public ApiResponse<EnrolmentWindowResponse> closeWindow(@PathVariable Long id, Authentication authentication) {
        return ok(seasonService.closeWindow(id, currentUserId(authentication)));
    }

    @GetMapping("/{id}/windows")
    public ApiResponse<PagedResponse<EnrolmentWindowResponse>> windows(@PathVariable Long id,
                                                         @RequestParam(defaultValue = "0") int page,
                                                         @RequestParam(defaultValue = "20") int size,
                                                         @RequestParam(defaultValue = "openedAt") String sortBy,
                                                         @RequestParam(defaultValue = "desc") String direction) {
        return ok(seasonService.windows(id, page, size, sortBy, direction));
    }

}
