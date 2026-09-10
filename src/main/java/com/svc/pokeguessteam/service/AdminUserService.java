package com.svc.pokeguessteam.service;

import com.svc.pokeguessteam.dto.admin.AdminUserListItemDto;
import com.svc.pokeguessteam.dto.admin.AdminUserPageResponse;
import com.svc.pokeguessteam.dto.admin.BanUserRequest;
import com.svc.pokeguessteam.dto.admin.SetUserRoleRequest;
import com.svc.pokeguessteam.exception.ApiBusinessException;
import com.svc.pokeguessteam.exception.ErrorCodes;
import com.svc.pokeguessteam.messages.MessageKeys;
import com.svc.pokeguessteam.model.enums.BanScope;
import com.svc.pokeguessteam.model.enums.UserRole;
import com.svc.pokeguessteam.model.user.UserModel;
import com.svc.pokeguessteam.repository.game.HistoryGamePlayerRepository;
import com.svc.pokeguessteam.repository.user.UserRepository;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

@Service
public class AdminUserService {

    private final UserRepository userRepository;
    private final HistoryGamePlayerRepository historyGamePlayerRepository;
    private final AdminAccessService adminAccessService;

    public AdminUserService(
            UserRepository userRepository,
            HistoryGamePlayerRepository historyGamePlayerRepository,
            AdminAccessService adminAccessService
    ) {
        this.userRepository = userRepository;
        this.historyGamePlayerRepository = historyGamePlayerRepository;
        this.adminAccessService = adminAccessService;
    }

    @Transactional(readOnly = true)
    public AdminUserPageResponse listUsers(
            String adminId,
            int page,
            int size,
            boolean sortByAbandoned,
            String q,
            String filter
    ) {
        adminAccessService.requireAdmin(adminId);
        int safePage = Math.max(page, 0);
        int safeSize = Math.min(Math.max(size, 1), 100);
        String query = q == null ? "" : q.trim().toLowerCase();
        String filterKey = filter == null ? "ALL" : filter.trim().toUpperCase();

        List<UserModel> all = userRepository.findAll();
        List<AdminUserListItemDto> items = new ArrayList<>();
        for (UserModel user : all) {
            if (!matchesQuery(user, query)) {
                continue;
            }
            if (!matchesFilter(user, filterKey)) {
                continue;
            }
            long abandoned = historyGamePlayerRepository.countAbandonedByUserId(user.getIdUser());
            items.add(AdminUserListItemDto.from(user, abandoned));
        }

        if (sortByAbandoned) {
            items.sort(Comparator
                    .comparingLong(AdminUserListItemDto::abandonedMatchesCount).reversed()
                    .thenComparing(AdminUserListItemDto::username, String.CASE_INSENSITIVE_ORDER));
        } else {
            items.sort(Comparator.comparing(AdminUserListItemDto::username, String.CASE_INSENSITIVE_ORDER));
        }

        int from = Math.min(safePage * safeSize, items.size());
        int to = Math.min(from + safeSize, items.size());
        Page<AdminUserListItemDto> sliced = new PageImpl<>(
                items.subList(from, to),
                PageRequest.of(safePage, safeSize),
                items.size()
        );
        return AdminUserPageResponse.from(sliced);
    }

    private static boolean matchesQuery(UserModel user, String query) {
        if (query.isEmpty()) {
            return true;
        }
        String username = user.getUsername() != null ? user.getUsername().toLowerCase() : "";
        String email = user.getEmail() != null ? user.getEmail().toLowerCase() : "";
        return username.contains(query) || email.contains(query);
    }

    private static boolean matchesFilter(UserModel user, String filterKey) {
        return switch (filterKey) {
            case "USER", "COMUNS", "COMMON" -> user.getRole() == UserRole.USER;
            case "ADMIN" -> user.getRole().isAdminOrAbove();
            case "BANNED", "BANIDOS" -> user.isSiteBannedNow() || user.isOnlineBannedNow();
            default -> true;
        };
    }

    @Transactional
    public AdminUserListItemDto ban(String adminId, String targetUserId, BanUserRequest request) {
        UserModel admin = adminAccessService.requireAdmin(adminId);
        if (adminId.equals(targetUserId)) {
            throw new ApiBusinessException(
                    HttpStatus.FORBIDDEN,
                    ErrorCodes.ADMIN_FORBIDDEN,
                    MessageKeys.ADMIN_FORBIDDEN
            );
        }
        UserModel target = requireTarget(targetUserId);
        assertCanModifyTarget(admin, target);

        if (!request.permanent() && (request.durationHours() == null || request.durationHours() < 1)) {
            throw new ApiBusinessException(
                    HttpStatus.BAD_REQUEST,
                    ErrorCodes.ADMIN_INVALID_BAN_DURATION,
                    MessageKeys.ADMIN_INVALID_BAN_DURATION
            );
        }

        LocalDateTime until = request.permanent()
                ? null
                : LocalDateTime.now().plusHours(request.durationHours());

        if (request.scope() == BanScope.SITE) {
            target.setSiteBannedPermanent(request.permanent());
            target.setSiteBannedUntil(until);
        } else {
            target.setOnlineBannedPermanent(request.permanent());
            target.setOnlineBannedUntil(until);
        }
        target.setBanReason(request.reason().trim());
        UserModel saved = userRepository.save(target);
        return toListItem(saved);
    }

    @Transactional
    public AdminUserListItemDto unban(String adminId, String targetUserId, BanScope scope) {
        UserModel admin = adminAccessService.requireAdmin(adminId);
        UserModel target = requireTarget(targetUserId);
        assertCanModifyTarget(admin, target);

        if (scope == BanScope.SITE) {
            target.setSiteBannedPermanent(false);
            target.setSiteBannedUntil(null);
        } else {
            target.setOnlineBannedPermanent(false);
            target.setOnlineBannedUntil(null);
        }
        if (!target.isSiteBannedNow() && !target.isOnlineBannedNow()) {
            target.setBanReason(null);
        }
        UserModel saved = userRepository.save(target);
        return toListItem(saved);
    }

    @Transactional
    public AdminUserListItemDto setRole(String masterId, String targetUserId, SetUserRoleRequest request) {
        adminAccessService.requireMaster(masterId);
        UserModel target = requireTarget(targetUserId);

        if (target.getRole().isMaster()) {
            throw new ApiBusinessException(
                    HttpStatus.FORBIDDEN,
                    ErrorCodes.ADMIN_CANNOT_MODIFY_MASTER,
                    MessageKeys.ADMIN_CANNOT_MODIFY_MASTER
            );
        }

        UserRole newRole = request.role();
        if (newRole != UserRole.USER && newRole != UserRole.ADMIN) {
            throw new ApiBusinessException(
                    HttpStatus.FORBIDDEN,
                    ErrorCodes.ADMIN_FORBIDDEN,
                    MessageKeys.ADMIN_FORBIDDEN
            );
        }

        target.setRole(newRole);
        UserModel saved = userRepository.save(target);
        return toListItem(saved);
    }

    private void assertCanModifyTarget(UserModel admin, UserModel target) {
        if (target.getRole().isMaster()) {
            throw new ApiBusinessException(
                    HttpStatus.FORBIDDEN,
                    ErrorCodes.ADMIN_CANNOT_MODIFY_MASTER,
                    MessageKeys.ADMIN_CANNOT_MODIFY_MASTER
            );
        }
    }

    private UserModel requireTarget(String targetUserId) {
        return userRepository.findById(targetUserId)
                .orElseThrow(() -> new ApiBusinessException(
                        HttpStatus.NOT_FOUND,
                        ErrorCodes.ADMIN_USER_NOT_FOUND,
                        MessageKeys.ADMIN_USER_NOT_FOUND
                ));
    }

    private AdminUserListItemDto toListItem(UserModel user) {
        return AdminUserListItemDto.from(
                user,
                historyGamePlayerRepository.countAbandonedByUserId(user.getIdUser())
        );
    }
}
