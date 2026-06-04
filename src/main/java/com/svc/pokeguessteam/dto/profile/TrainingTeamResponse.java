package com.svc.pokeguessteam.dto.profile;

import com.svc.pokeguessteam.dto.pokemon.PcLineDto;
import com.svc.pokeguessteam.model.pokemon.EvolutionLineModel;
import com.svc.pokeguessteam.model.user.TrainingTeamModel;
import com.svc.pokeguessteam.model.user.UserPokemonInventoryModel;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public record TrainingTeamResponse(List<TrainingTeamSlotDto> slots) {

    public record TrainingTeamSlotDto(int slot, Integer evolutionLineKey, PcLineDto line) {
    }

    public static TrainingTeamResponse from(
            TrainingTeamModel team,
            Map<Integer, UserPokemonInventoryModel> inventoryByLineKey
    ) {
        if (team == null) {
            return empty();
        }
        List<TrainingTeamSlotDto> list = new ArrayList<>(TrainingTeamModel.TEAM_SIZE);
        for (int i = 0; i < TrainingTeamModel.TEAM_SIZE; i++) {
            EvolutionLineModel line = team.getSlot(i);
            Integer lineKey = line != null ? line.getLineKey() : null;
            PcLineDto pcLine = null;
            if (lineKey != null && inventoryByLineKey != null) {
                UserPokemonInventoryModel row = inventoryByLineKey.get(lineKey);
                if (row != null) {
                    pcLine = PcLineDto.from(row);
                }
            }
            list.add(new TrainingTeamSlotDto(i + 1, lineKey, pcLine));
        }
        return new TrainingTeamResponse(list);
    }

    private static TrainingTeamResponse empty() {
        List<TrainingTeamSlotDto> list = new ArrayList<>(TrainingTeamModel.TEAM_SIZE);
        for (int i = 0; i < TrainingTeamModel.TEAM_SIZE; i++) {
            list.add(new TrainingTeamSlotDto(i + 1, null, null));
        }
        return new TrainingTeamResponse(list);
    }
}
