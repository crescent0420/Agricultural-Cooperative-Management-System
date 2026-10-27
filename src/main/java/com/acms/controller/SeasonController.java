package com.acms.controller;

import com.acms.dto.EnrolmentWindowResponse;
import com.acms.dto.PageResponse;
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
public class SeasonController {

    private final SeasonService seasonService;

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public SeasonResponse create(@Valid @RequestBody SeasonRequest request, Authentication authentication) {
        return seasonService.create(request, userId(authentication));
    }

    @GetMapping
    public PageResponse<SeasonResponse> list(@RequestParam(defaultValue = "0") int page,
                                             @RequestParam(defaultValue = "20") int size,
                                             @RequestParam(defaultValue = "startsOn") String sortBy,
                                             @RequestParam(defaultValue = "desc") String direction) {
        return seasonService.findAll(page, size, sortBy, direction);
    }

    @GetMapping("/{id}")
    public SeasonResponse get(@PathVariable Long id) {
        return seasonService.findById(id);
    }

    @PutMapping("/{id}")
    public SeasonResponse update(@PathVariable Long id, @Valid @RequestBody SeasonRequest request,
                                 Authentication authentication) {
        return seasonService.update(id, request, userId(authentication));
    }

    @PostMapping("/{id}/windows/open")
    @ResponseStatus(HttpStatus.CREATED)
    public EnrolmentWindowResponse openWindow(@PathVariable Long id, Authentication authentication) {
        return seasonService.openWindow(id, userId(authentication));
    }

    @PostMapping("/{id}/windows/close")
    public EnrolmentWindowResponse closeWindow(@PathVariable Long id, Authentication authentication) {
        return seasonService.closeWindow(id, userId(authentication));
    }

    @GetMapping("/{id}/windows")
    public PageResponse<EnrolmentWindowResponse> windows(@PathVariable Long id,
                                                         @RequestParam(defaultValue = "0") int page,
                                                         @RequestParam(defaultValue = "20") int size,
                                                         @RequestParam(defaultValue = "openedAt") String sortBy,
                                                         @RequestParam(defaultValue = "desc") String direction) {
        return seasonService.windows(id, page, size, sortBy, direction);
    }

    private static Long userId(Authentication authentication) {
        return Long.valueOf(authentication.getName());
    }
}
