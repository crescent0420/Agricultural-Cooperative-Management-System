package com.acms.service;

import com.acms.exception.BadRequestException;
import java.util.Locale;
import java.util.Set;
import java.util.TreeSet;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;

/**
 * The one place that turns ?page=&size=&sortBy=&direction= into a safe database page request.
 * Every list endpoint uses it, so all lists follow the same rules.
 */
public final class Paging {

    public static final int MAX_SIZE = 100;

    private Paging() {
    }

    public static Pageable of(int page, int size, String sortBy, String direction, Set<String> sortableFields) {
        if (page < 0) {
            throw new BadRequestException("page must be 0 or greater.");
        }
        if (size < 1 || size > MAX_SIZE) {
            throw new BadRequestException("size must be between 1 and " + MAX_SIZE + ".");
        }
        if (sortBy == null || !sortableFields.contains(sortBy)) {
            throw new BadRequestException(
                    "sortBy must be one of: " + String.join(", ", new TreeSet<>(sortableFields)) + ".");
        }
        Sort.Direction sortDirection = switch (direction == null ? "" : direction.toLowerCase(Locale.ROOT)) {
            case "asc" -> Sort.Direction.ASC;
            case "desc" -> Sort.Direction.DESC;
            default -> throw new BadRequestException("direction must be 'asc' or 'desc'.");
        };

        // Tie-breaker: rows with equal sort values are ordered by id, so no row repeats or vanishes between pages.
        Sort sort = Sort.by(sortDirection, sortBy);
        if (!"id".equals(sortBy)) {
            sort = sort.and(Sort.by(sortDirection, "id"));
        }
        return PageRequest.of(page, size, sort);
    }
}