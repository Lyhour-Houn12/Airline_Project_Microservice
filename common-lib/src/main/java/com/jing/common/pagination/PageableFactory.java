package com.jing.common.pagination;

import com.jing.common.exception.BadRequestException;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;

import java.util.Set;
import java.util.TreeSet;

/**
 * Builds a validated {@link Pageable} from the query parameters every list endpoint accepts.
 *
 * <ul>
 *   <li>{@code page} is 1-based (the first page is 1), as returned in {@link PageResponse#getPageNumber()}.</li>
 *   <li>{@code size} must be between 1 and {@value #MAX_PAGE_SIZE}.</li>
 *   <li>{@code sortBy} must be one of the whitelisted fields; blank means {@code defaultSortBy}.</li>
 *   <li>{@code sortDirection} is "asc" or "desc" (any case); blank means {@code defaultDirection}.</li>
 * </ul>
 * Results are always tie-broken by {@code id} so that paging is stable.
 * Invalid input is rejected with a 400 instead of being silently corrected.
 */
public final class PageableFactory {

    public static final int MAX_PAGE_SIZE = 100;

    private PageableFactory() {}

    public static Pageable of(int page, int size, String sortBy, String sortDirection,
                              Set<String> sortable, String defaultSortBy) {
        return of(page, size, sortBy, sortDirection, sortable, defaultSortBy, Sort.Direction.DESC);
    }

    public static Pageable of(int page, int size, String sortBy, String sortDirection,
                              Set<String> sortable, String defaultSortBy, Sort.Direction defaultDirection) {
        if (page < 1) {
            throw new BadRequestException("page must be 1 or greater");
        }
        if (size < 1 || size > MAX_PAGE_SIZE) {
            throw new BadRequestException("size must be between 1 and " + MAX_PAGE_SIZE);
        }

        String field = (sortBy == null || sortBy.isBlank()) ? defaultSortBy : sortBy.trim();
        if (!sortable.contains(field)) {
            throw new BadRequestException("Cannot sort by '" + field + "'. Allowed: " + new TreeSet<>(sortable));
        }

        Sort.Direction direction = defaultDirection;
        if (sortDirection != null && !sortDirection.isBlank()) {
            direction = Sort.Direction.fromOptionalString(sortDirection.trim())
                    .orElseThrow(() -> new BadRequestException("sortDirection must be 'asc' or 'desc'"));
        }

        Sort sort = Sort.by(direction, field);
        if (!"id".equals(field)) {
            sort = sort.and(Sort.by(Sort.Direction.ASC, "id"));   // tie-breaker: stable paging
        }
        return PageRequest.of(page - 1, size, sort);
    }
}
