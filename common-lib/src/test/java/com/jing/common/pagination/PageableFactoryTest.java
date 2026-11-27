package com.jing.common.pagination;

import com.jing.common.exception.BadRequestException;
import org.junit.jupiter.api.Test;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;

import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class PageableFactoryTest {

    private static final Set<String> SORTABLE = Set.of("id", "name", "createdAt");

    @Test
    void pagesAreOneBased() {
        Pageable pageable = PageableFactory.of(1, 10, "name", "asc", SORTABLE, "id");

        assertThat(pageable.getPageNumber()).isZero();
        assertThat(pageable.getPageSize()).isEqualTo(10);
    }

    @Test
    void addsIdTieBreakerForStablePaging() {
        Sort sort = PageableFactory.of(2, 10, "name", "DESC", SORTABLE, "id").getSort();

        assertThat(sort.getOrderFor("name").getDirection()).isEqualTo(Sort.Direction.DESC);
        assertThat(sort.getOrderFor("id").getDirection()).isEqualTo(Sort.Direction.ASC);
    }

    @Test
    void blankValuesUseTheDefaults() {
        Sort sort = PageableFactory.of(1, 10, " ", null, SORTABLE, "createdAt", Sort.Direction.ASC).getSort();

        assertThat(sort.getOrderFor("createdAt").getDirection()).isEqualTo(Sort.Direction.ASC);
    }

    @Test
    void rejectsInvalidInput() {
        assertThatThrownBy(() -> PageableFactory.of(0, 10, null, null, SORTABLE, "id"))
                .isInstanceOf(BadRequestException.class);
        assertThatThrownBy(() -> PageableFactory.of(1, 101, null, null, SORTABLE, "id"))
                .isInstanceOf(BadRequestException.class);
        assertThatThrownBy(() -> PageableFactory.of(1, 10, "password", null, SORTABLE, "id"))
                .isInstanceOf(BadRequestException.class);
        assertThatThrownBy(() -> PageableFactory.of(1, 10, "name", "sideways", SORTABLE, "id"))
                .isInstanceOf(BadRequestException.class);
    }
}
