package com.svc.pokeguessteam.dto.profile;

import com.svc.pokeguessteam.model.user.TrainingTeamModel;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.util.List;

/**
 * 6 posições do time de treino; {@code null} num índice deixa o slot vazio.
 * Cada valor é {@code evolutionLineKey} de uma linha presente no inventário (PC) do jogador.
 */
public record UpdateTrainingTeamRequest(
        @NotNull
        @Size(min = TrainingTeamModel.TEAM_SIZE, max = TrainingTeamModel.TEAM_SIZE)
        List<Integer> slots
) {
}
