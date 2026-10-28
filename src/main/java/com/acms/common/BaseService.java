package com.acms.common;

import com.acms.exception.ResourceNotFoundException;
import java.util.Optional;
import java.util.Set;
import org.springframework.data.domain.Pageable;

/** Parent of every service: the small helpers they all need. */
public abstract class BaseService {

    /** "Farmer with id 7 was not found." */
    protected static ResourceNotFoundException notFound(String what, Object id) {
        return new ResourceNotFoundException(what + " with id " + id + " was not found.");
    }

    /** The value, or a 404 naming what was missing. */
    protected static <T> T found(Optional<T> value, String what, Object id) {
        return value.orElseThrow(() -> notFound(what, id));
    }

    /** ?page=&size=&sortBy=&direction= turned into a safe Pageable (see Paging). */
    protected static Pageable pageable(int page, int size, String sortBy, String direction, Set<String> sortableFields) {
        return Paging.of(page, size, sortBy, direction, sortableFields);
    }
}
