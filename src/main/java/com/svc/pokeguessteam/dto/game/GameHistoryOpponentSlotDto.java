package com.svc.pokeguessteam.dto.game;

/**
 * Um slot do time adversário no histórico (equipa completa revelada ao terminar).
 *
 * @param accepted {@code true} se o jogador acertou esse Pokémon durante a partida.
 */
public record GameHistoryOpponentSlotDto(
        int slot,
        int pokedexNumber,
        boolean accepted
) {
}
