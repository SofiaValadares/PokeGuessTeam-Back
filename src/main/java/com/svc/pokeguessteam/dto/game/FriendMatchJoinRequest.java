package com.svc.pokeguessteam.dto.game;

import com.svc.pokeguessteam.messages.MessageKeys;
import com.svc.pokeguessteam.util.GameConstants;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.util.List;

public record FriendMatchJoinRequest(
        @NotBlank(message = "{" + MessageKeys.GAME_JOIN_CODE_REQUIRED + "}")
        @Size(min = GameConstants.FRIEND_JOIN_CODE_LENGTH, max = 10, message = "{" + MessageKeys.GAME_JOIN_CODE_SIZE + "}")
        String joinCode,
        @NotNull
        @Size(min = GameConstants.TEAM_SIZE, max = GameConstants.TEAM_SIZE)
        List<Integer> team
) {
}
