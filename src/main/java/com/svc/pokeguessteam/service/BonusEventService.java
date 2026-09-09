package com.svc.pokeguessteam.service;

import com.svc.pokeguessteam.dto.admin.ActiveBonusEventDto;
import com.svc.pokeguessteam.dto.admin.BonusEventDto;
import com.svc.pokeguessteam.dto.admin.BonusEventUpsertRequest;
import com.svc.pokeguessteam.exception.ApiBusinessException;
import com.svc.pokeguessteam.exception.ErrorCodes;
import com.svc.pokeguessteam.messages.MessageKeys;
import com.svc.pokeguessteam.model.admin.BonusEventModel;
import com.svc.pokeguessteam.model.enums.BonusEventStatus;
import com.svc.pokeguessteam.model.pokemon.EvolutionLineModel;
import com.svc.pokeguessteam.model.user.ProfileModel;
import com.svc.pokeguessteam.model.user.TrainingTeamModel;
import com.svc.pokeguessteam.model.user.UserModel;
import com.svc.pokeguessteam.repository.admin.BonusEventRepository;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.HashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;

@Service
public class BonusEventService {

    private final BonusEventRepository bonusEventRepository;
    private final AdminAccessService adminAccessService;

    public BonusEventService(BonusEventRepository bonusEventRepository, AdminAccessService adminAccessService) {
        this.bonusEventRepository = bonusEventRepository;
        this.adminAccessService = adminAccessService;
    }

    @Transactional(readOnly = true)
    public List<BonusEventDto> listEvents(String adminId) {
        adminAccessService.requireAdmin(adminId);
        return bonusEventRepository.findAll().stream()
                .map(BonusEventDto::from)
                .toList();
    }

    @Transactional(readOnly = true)
    public BonusEventDto getEvent(String adminId, String eventId) {
        adminAccessService.requireAdmin(adminId);
        return BonusEventDto.from(requireEvent(eventId));
    }

    @Transactional
    public BonusEventDto create(String masterId, BonusEventUpsertRequest request) {
        UserModel master = adminAccessService.requireMaster(masterId);
        BonusEventModel event = new BonusEventModel();
        applyUpsert(event, request);
        event.setStatus(BonusEventStatus.DRAFT);
        event.setCreatedBy(master);
        return BonusEventDto.from(bonusEventRepository.save(event));
    }

    @Transactional
    public BonusEventDto update(String masterId, String eventId, BonusEventUpsertRequest request) {
        adminAccessService.requireMaster(masterId);
        BonusEventModel event = requireEvent(eventId);
        assertNotActive(event);
        applyUpsert(event, request);
        return BonusEventDto.from(bonusEventRepository.save(event));
    }

    @Transactional
    public void delete(String masterId, String eventId) {
        adminAccessService.requireMaster(masterId);
        BonusEventModel event = requireEvent(eventId);
        assertNotActive(event);
        bonusEventRepository.delete(event);
    }

    @Transactional
    public BonusEventDto start(String adminId, String eventId) {
        adminAccessService.requireAdmin(adminId);
        BonusEventModel event = requireEvent(eventId);
        if (event.getStatus() == BonusEventStatus.ACTIVE) {
            throw new ApiBusinessException(
                    HttpStatus.CONFLICT,
                    ErrorCodes.ADMIN_EVENT_ALREADY_ACTIVE,
                    MessageKeys.ADMIN_EVENT_ALREADY_ACTIVE
            );
        }
        if (bonusEventRepository.existsByStatus(BonusEventStatus.ACTIVE)) {
            throw new ApiBusinessException(
                    HttpStatus.CONFLICT,
                    ErrorCodes.ADMIN_EVENT_ALREADY_ACTIVE,
                    MessageKeys.ADMIN_EVENT_ALREADY_ACTIVE
            );
        }
        LocalDateTime now = LocalDateTime.now();
        event.setStatus(BonusEventStatus.ACTIVE);
        event.setStartedAt(now);
        event.setEndsAt(now.plusHours(event.getDurationHours()));
        return BonusEventDto.from(bonusEventRepository.save(event));
    }

    @Transactional
    public BonusEventDto end(String masterId, String eventId) {
        adminAccessService.requireMaster(masterId);
        BonusEventModel event = requireEvent(eventId);
        if (event.getStatus() != BonusEventStatus.ACTIVE) {
            throw new ApiBusinessException(
                    HttpStatus.CONFLICT,
                    ErrorCodes.ADMIN_EVENT_NOT_ACTIVE,
                    MessageKeys.ADMIN_EVENT_NOT_ACTIVE
            );
        }
        event.setStatus(BonusEventStatus.ENDED);
        event.setEndsAt(LocalDateTime.now());
        return BonusEventDto.from(bonusEventRepository.save(event));
    }

    @Transactional(readOnly = true)
    public Optional<ActiveBonusEventDto> findActivePublic() {
        return findActiveEntity().map(ActiveBonusEventDto::from);
    }

    @Transactional(readOnly = true)
    public Optional<BonusEventModel> findActive() {
        return findActiveEntity();
    }

    @Transactional
    public int endExpired() {
        LocalDateTime now = LocalDateTime.now();
        List<BonusEventModel> active = bonusEventRepository.findByStatus(BonusEventStatus.ACTIVE);
        int ended = 0;
        for (BonusEventModel event : active) {
            if (event.getEndsAt() != null && !event.getEndsAt().isAfter(now)) {
                event.setStatus(BonusEventStatus.ENDED);
                bonusEventRepository.save(event);
                ended++;
            }
        }
        return ended;
    }

    @Transactional(readOnly = true)
    public boolean teamQualifiesForActiveBonus(ProfileModel profile) {
        Optional<BonusEventModel> active = findActiveEntity();
        if (active.isEmpty()) {
            return false;
        }
        return teamQualifies(profile, active.get());
    }

    @Transactional(readOnly = true)
    public double resolveXpMultiplier(ProfileModel profile) {
        Optional<BonusEventModel> active = findActiveEntity();
        if (active.isEmpty()) {
            return 1.0;
        }
        BonusEventModel event = active.get();
        if (teamQualifies(profile, event) && event.getXpMultiplier() != null) {
            return event.getXpMultiplier();
        }
        return 1.0;
    }

    /** Multiplicador do evento ativo (sem exigir time de treino) — usado em partidas de evento. */
    @Transactional(readOnly = true)
    public double resolveActiveEventXpMultiplier() {
        return findActiveEntity()
                .map(event -> event.getXpMultiplier() != null ? event.getXpMultiplier() : 1.0)
                .orElse(1.0);
    }

    private boolean teamQualifies(ProfileModel profile, BonusEventModel event) {
        TrainingTeamModel team = profile.getTrainingTeam();
        if (team == null) {
            return false;
        }
        Set<Integer> eventPokedex = new HashSet<>(event.getPokedexNumbers());
        if (eventPokedex.isEmpty()) {
            return false;
        }
        for (int i = 0; i < TrainingTeamModel.TEAM_SIZE; i++) {
            EvolutionLineModel line = team.getSlot(i);
            if (line == null) {
                return false;
            }
            List<Integer> members = line.getMemberPokedexNumbers();
            if (members == null || members.stream().noneMatch(eventPokedex::contains)) {
                return false;
            }
        }
        return true;
    }

    private Optional<BonusEventModel> findActiveEntity() {
        return bonusEventRepository.findFirstByStatus(BonusEventStatus.ACTIVE)
                .filter(e -> e.getEndsAt() == null || e.getEndsAt().isAfter(LocalDateTime.now()));
    }

    private BonusEventModel requireEvent(String eventId) {
        return bonusEventRepository.findById(eventId)
                .orElseThrow(() -> new ApiBusinessException(
                        HttpStatus.NOT_FOUND,
                        ErrorCodes.ADMIN_EVENT_NOT_FOUND,
                        MessageKeys.ADMIN_EVENT_NOT_FOUND
                ));
    }

    private static void assertNotActive(BonusEventModel event) {
        if (event.getStatus() == BonusEventStatus.ACTIVE) {
            throw new ApiBusinessException(
                    HttpStatus.CONFLICT,
                    ErrorCodes.ADMIN_EVENT_ACTIVE,
                    MessageKeys.ADMIN_EVENT_ACTIVE
            );
        }
    }

    private static void applyUpsert(BonusEventModel event, BonusEventUpsertRequest request) {
        event.setName(request.name().trim());
        event.setDescription(request.description().trim());
        event.setDurationHours(request.durationHours());
        event.setXpMultiplier(request.xpMultiplier());
        event.setPokedexNumbers(request.pokedexNumbers());
    }
}
