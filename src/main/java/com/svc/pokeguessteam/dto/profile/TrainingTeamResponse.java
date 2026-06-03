package com.svc.pokeguessteam.dto.profile;

import com.svc.pokeguessteam.dto.pokemon.PokemonDto;
import com.svc.pokeguessteam.model.pokemon.PokemonModel;
import com.svc.pokeguessteam.model.user.TrainingTeamModel;

import java.util.ArrayList;
import java.util.List;

public record TrainingTeamResponse(List<TrainingTeamSlotDto> slots) {

    public record TrainingTeamSlotDto(int slot, PokemonDto pokemon) {
    }

    public static TrainingTeamResponse from(TrainingTeamModel team) {
        if (team == null) {
            return empty();
        }
        List<TrainingTeamSlotDto> list = new ArrayList<>(TrainingTeamModel.TEAM_SIZE);
        for (int i = 0; i < TrainingTeamModel.TEAM_SIZE; i++) {
            PokemonModel p = team.getSlot(i);
            list.add(new TrainingTeamSlotDto(
                    i + 1,
                    p != null ? PokemonDto.from(p) : null
            ));
        }
        return new TrainingTeamResponse(list);
    }

    private static TrainingTeamResponse empty() {
        List<TrainingTeamSlotDto> list = new ArrayList<>(TrainingTeamModel.TEAM_SIZE);
        for (int i = 0; i < TrainingTeamModel.TEAM_SIZE; i++) {
            list.add(new TrainingTeamSlotDto(i + 1, null));
        }
        return new TrainingTeamResponse(list);
    }
}
