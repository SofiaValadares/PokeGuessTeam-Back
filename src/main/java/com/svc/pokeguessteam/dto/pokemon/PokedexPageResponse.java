package com.svc.pokeguessteam.dto.pokemon;

import com.svc.pokeguessteam.model.pokemon.PokemonModel;
import org.springframework.data.domain.Page;

import java.util.List;

public record PokedexPageResponse(
        List<PokemonDto> content,
        int page,
        int size,
        long totalElements,
        int totalPages,
        boolean first,
        boolean last
) {
    public static PokedexPageResponse from(Page<PokemonModel> page) {
        List<PokemonDto> items = page.getContent().stream().map(PokemonDto::from).toList();
        return new PokedexPageResponse(
                items,
                page.getNumber(),
                page.getSize(),
                page.getTotalElements(),
                page.getTotalPages(),
                page.isFirst(),
                page.isLast()
        );
    }
}
