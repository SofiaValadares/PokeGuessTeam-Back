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
    private final AdminAuditService adminAuditService;
    private final AuditLogService auditLogService;

    public AdminUserService(
            UserRepository userRepository,
            HistoryGamePlayerRepository historyGamePlayerRepository,
            AdminAccessService adminAccessService,
            AdminAuditService adminAuditService,
            AuditLogService auditLogService
    ) {
        this.userRepository = userRepository;
        this.historyGamePlayerRepository = historyGamePlayerRepository;
        this.adminAccessService = adminAccessService;
        this.adminAuditService = adminAuditService;
        this.auditLogService = auditLogService;
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

            long abandoned =
                    historyGamePlayerRepository.countAbandonedByUserId(
                            user.getIdUser()
                    );

            items.add(
                    AdminUserListItemDto.from(user, abandoned)
            );
        }

        if (sortByAbandoned) {
            items.sort(
                    Comparator
                            .comparingLong(
                                    AdminUserListItemDto::abandonedMatchesCount
                            )
                            .reversed()
                            .thenComparing(
                                    AdminUserListItemDto::username,
                                    String.CASE_INSENSITIVE_ORDER
                            )
            );
        } else {
            items.sort(
                    Comparator.comparing(
                            AdminUserListItemDto::username,
                            String.CASE_INSENSITIVE_ORDER
                    )
            );
        }

        int from =
                Math.min(safePage * safeSize, items.size());

        int to =
                Math.min(from + safeSize, items.size());

        Page<AdminUserListItemDto> sliced =
                new PageImpl<>(
                        items.subList(from, to),
                        PageRequest.of(safePage, safeSize),
                        items.size()
                );

        return AdminUserPageResponse.from(sliced);
    }

    private static boolean matchesQuery(
            UserModel user,
            String query
    ) {
        if (query.isEmpty()) {
            return true;
        }

        String username =
                user.getUsername() != null
                        ? user.getUsername().toLowerCase()
                        : "";

        String email =
                user.getEmail() != null
                        ? user.getEmail().toLowerCase()
                        : "";

        return username.contains(query)
                || email.contains(query);
    }

    private static boolean matchesFilter(
            UserModel user,
            String filterKey
    ) {
        return switch (filterKey) {
            case "USER", "COMUNS", "COMMON" ->
                    user.getRole() == UserRole.USER;

            case "ADMIN" ->
                    user.getRole().isAdminOrAbove();

            case "BANNED", "BANIDOS" ->
                    user.isSiteBannedNow()
                            || user.isOnlineBannedNow();

            default -> true;
        };
    }

    @Transactional
    public AdminUserListItemDto ban(
            String adminId,
            String targetUserId,
            BanUserRequest request
    ) {
        UserModel admin =
                adminAccessService.requireAdmin(adminId);

        if (adminId.equals(targetUserId)) {
            throw new ApiBusinessException(
                    HttpStatus.FORBIDDEN,
                    ErrorCodes.ADMIN_FORBIDDEN,
                    MessageKeys.ADMIN_FORBIDDEN
            );
        }

        UserModel target =
                requireTarget(targetUserId);

        assertCanModifyTarget(admin, target);

        if (!request.permanent()
                && (request.durationHours() == null
                || request.durationHours() < 1)) {

            throw new ApiBusinessException(
                    HttpStatus.BAD_REQUEST,
                    ErrorCodes.ADMIN_INVALID_BAN_DURATION,
                    MessageKeys.ADMIN_INVALID_BAN_DURATION
            );
        }

        LocalDateTime until =
                request.permanent()
                        ? null
                        : LocalDateTime.now()
                                .plusHours(request.durationHours());

        if (request.scope() == BanScope.SITE) {

            target.setSiteBannedPermanent(
                    request.permanent()
            );

            target.setSiteBannedUntil(until);

        } else {

            target.setOnlineBannedPermanent(
                    request.permanent()
            );

            target.setOnlineBannedUntil(until);
        }

        target.setBanReason(
                request.reason().trim()
        );

        UserModel saved =
                userRepository.save(target);

        auditLogService.recordAdminAction(
                adminId,
                "USER_BAN",
                "scope=" + request.scope()
                        + " target=" + saved.getIdUser()
                        + " @" + saved.getUsername()
                        + " permanent=" + request.permanent()
                        + " reason=" + request.reason().trim()
        );

        return toListItem(saved);
    }

    @Transactional
    public AdminUserListItemDto unban(
            String adminId,
            String targetUserId,
            BanScope scope
    ) {
        UserModel admin =
                adminAccessService.requireAdmin(adminId);

        UserModel target =
                requireTarget(targetUserId);

        assertCanModifyTarget(admin, target);

        if (scope == BanScope.SITE) {

            target.setSiteBannedPermanent(false);
            target.setSiteBannedUntil(null);

        } else {

            target.setOnlineBannedPermanent(false);
            target.setOnlineBannedUntil(null);
        }

        if (!target.isSiteBannedNow()
                && !target.isOnlineBannedNow()) {

            target.setBanReason(null);
        }

        UserModel saved =
                userRepository.save(target);

        auditLogService.recordAdminAction(
                adminId,
                "USER_UNBAN",
                "scope=" + scope + " target=" + saved.getIdUser() + " @" + saved.getUsername()
        );

        return toListItem(saved);
    }

    /**
     * Altera o papel de um usuário.
     *
     * Apenas MASTER_ADMIN pode realizar alterações de papel.
     *
     * Regras:
     *
     * 1. Um master não pode alterar o próprio papel.
     * 2. USER, ADMIN e MASTER_ADMIN podem ser atribuídos.
     * 3. Promoções administrativas exigem e-mail verificado.
     * 4. Nunca pode existir zero MASTER_ADMIN no sistema.
     * 5. As linhas de MASTER_ADMIN são bloqueadas durante a operação
     *    para evitar condições de corrida.
     * 6. Toda alteração é registrada no histórico de auditoria.
     */
    @Transactional
    public AdminUserListItemDto setRole(
            String masterId,
            String targetUserId,
            SetUserRoleRequest request,
            String ipAddress
    ) {

        /*
         * Bloqueia os MASTER_ADMIN existentes.
         *
         * Isso impede, por exemplo:
         *
         * Master A rebaixa Master B
         * ao mesmo tempo que
         * Master B rebaixa Master A.
         *
         * Sem esse lock, os dois requests poderiam verificar
         * que existem dois masters e ambos prosseguirem,
         * deixando o sistema sem nenhum MASTER_ADMIN.
         */
        List<UserModel> masters =
                userRepository.findAllByRoleForUpdate(
                        UserRole.MASTER_ADMIN
                );

        /*
         * Mesmo que o endpoint já seja protegido,
         * a regra também fica no Service.
         *
         * Assim não dependemos exclusivamente do Controller
         * ou do SecurityConfig.
         */
        UserModel actor =
                adminAccessService.requireMaster(masterId);

        /*
         * O master não pode alterar o próprio papel.
         *
         * Isso evita que ele se rebaixe acidentalmente
         * e perca acesso à área administrativa.
         */
        if (masterId.equals(targetUserId)) {

            throw new ApiBusinessException(
                    HttpStatus.FORBIDDEN,
                    ErrorCodes.ADMIN_CANNOT_CHANGE_OWN_ROLE,
                    MessageKeys.ADMIN_CANNOT_CHANGE_OWN_ROLE
            );
        }

        UserModel target =
                requireTarget(targetUserId);

        UserRole oldRole =
                target.getRole();

        UserRole newRole =
                request.role();

        /*
         * Caso o request solicite exatamente o papel
         * que o usuário já possui, nada precisa ser feito.
         */
        if (oldRole == newRole) {
            return toListItem(target);
        }

        /*
         * Verificamos se a alteração realmente representa
         * uma PROMOÇÃO.
         *
         * Exemplos:
         *
         * USER -> ADMIN          = promoção
         * USER -> MASTER_ADMIN   = promoção
         * ADMIN -> MASTER_ADMIN  = promoção
         *
         * MASTER_ADMIN -> ADMIN  = rebaixamento
         * ADMIN -> USER          = rebaixamento
         */
        boolean promotion =
                roleLevel(newRole)
                        > roleLevel(oldRole);

        /*
         * Somente promoções administrativas exigem
         * e-mail verificado.
         *
         * Isso evita conceder privilégios elevados
         * a uma conta que ainda não comprovou
         * que controla o endereço de e-mail.
         */
        if (promotion
                && newRole.isAdminOrAbove()
                && !Boolean.TRUE.equals(
                        target.getEmailVerify()
                )) {

            throw new ApiBusinessException(
                    HttpStatus.CONFLICT,
                    ErrorCodes.ADMIN_TARGET_NOT_VERIFIED,
                    MessageKeys.ADMIN_TARGET_NOT_VERIFIED
            );
        }

        /*
         * Caso o alvo seja MASTER_ADMIN e esteja sendo
         * rebaixado para ADMIN ou USER, precisa existir
         * pelo menos outro MASTER_ADMIN.
         */
        if (oldRole == UserRole.MASTER_ADMIN
                && newRole != UserRole.MASTER_ADMIN
                && masters.size() <= 1) {

            throw new ApiBusinessException(
                    HttpStatus.CONFLICT,
                    ErrorCodes.ADMIN_LAST_MASTER,
                    MessageKeys.ADMIN_LAST_MASTER
            );
        }

        /*
         * Efetiva a alteração.
         */
        target.setRole(newRole);

        UserModel saved =
                userRepository.save(target);

        /*
         * Registra quem fez a alteração,
         * quem foi alterado,
         * papel anterior,
         * papel novo,
         * IP e data/hora.
         */
        adminAuditService.recordRoleChange(
                actor,
                saved,
                oldRole,
                newRole,
                ipAddress
        );

        auditLogService.recordAdminAction(
                masterId,
                "ROLE_CHANGE",
                "target=" + saved.getIdUser()
                        + " @" + saved.getUsername()
                        + " " + oldRole + " -> " + newRole
        );
        if (roleLevel(newRole) > roleLevel(oldRole)) {
            auditLogService.recordSecurity(
                    masterId,
                    "PRIVILEGE_ELEVATION",
                    "target=" + saved.getIdUser() + " role=" + newRole
            );
        }

        return toListItem(saved);
    }

    /**
     * Define a hierarquia dos papéis.
     *
     * Não usamos ordinal() do enum propositalmente.
     *
     * Assim, caso alguém mude a ordem dos valores
     * dentro de UserRole no futuro, a regra de segurança
     * continua funcionando corretamente.
     */
    private static int roleLevel(UserRole role) {
        return switch (role) {
            case USER -> 0;
            case ADMIN -> 1;
            case MASTER_ADMIN -> 2;
        };
    }

    /**
     * Regra utilizada pelas operações de banimento.
     *
     * ADMIN comum não pode modificar um MASTER_ADMIN.
     */
    private void assertCanModifyTarget(
            UserModel admin,
            UserModel target
    ) {
        if (target.getRole().isMaster()) {

            throw new ApiBusinessException(
                    HttpStatus.FORBIDDEN,
                    ErrorCodes.ADMIN_CANNOT_MODIFY_MASTER,
                    MessageKeys.ADMIN_CANNOT_MODIFY_MASTER
            );
        }
    }

    private UserModel requireTarget(
            String targetUserId
    ) {
        return userRepository
                .findById(targetUserId)
                .orElseThrow(
                        () -> new ApiBusinessException(
                                HttpStatus.NOT_FOUND,
                                ErrorCodes.ADMIN_USER_NOT_FOUND,
                                MessageKeys.ADMIN_USER_NOT_FOUND
                        )
                );
    }

    private AdminUserListItemDto toListItem(
            UserModel user
    ) {
        return AdminUserListItemDto.from(
                user,
                historyGamePlayerRepository
                        .countAbandonedByUserId(
                                user.getIdUser()
                        )
        );
    }
}
