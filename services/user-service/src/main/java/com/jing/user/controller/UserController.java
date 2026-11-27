package com.jing.user.controller;

import com.jing.common.dto.user.UserDto;
import com.jing.common.pagination.PageResponse;
import com.jing.common.pagination.PageableFactory;
import com.jing.user.payload.request.ChangePasswordRequest;
import com.jing.user.payload.request.UpdateRoleRequest;
import com.jing.user.service.UserService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Sort;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.Set;

@RestController
@RequestMapping("/api/v1/users")
@RequiredArgsConstructor
public class UserController {

    private static final Set<String> SORTABLE = Set.of("id", "fullname", "email", "role", "lastLogin");

    private final UserService userService;

    /** The principal is the userId taken from the verified JWT subject. */
    @GetMapping("/me")
    public UserDto me(@AuthenticationPrincipal Long userId) {
        return userService.getById(userId);
    }

    @PutMapping("/me/password")
    public ResponseEntity<Void> changePassword(@AuthenticationPrincipal Long userId,
                                               @Valid @RequestBody ChangePasswordRequest request) {
        userService.changePassword(userId, request);
        return ResponseEntity.noContent().build();
    }

    @GetMapping
    @PreAuthorize("hasRole('SYSTEM_ADMIN')")
    public PageResponse<UserDto> all(@RequestParam(defaultValue = "1") int page,
                                     @RequestParam(defaultValue = "20") int size,
                                     @RequestParam(defaultValue = "id") String sortBy,
                                     @RequestParam(defaultValue = "asc") String sortDirection) {
        // same 1-based, capped paging as every other service
        return userService.getAll(PageableFactory.of(page, size, sortBy, sortDirection,
                SORTABLE, "id", Sort.Direction.ASC));
    }

    @PutMapping("/{id}/role")
    @PreAuthorize("hasRole('SYSTEM_ADMIN')")
    public UserDto updateRole(@AuthenticationPrincipal Long adminId,
                              @PathVariable Long id,
                              @Valid @RequestBody UpdateRoleRequest request) {
        return userService.updateRole(adminId, id, request.role());
    }
}