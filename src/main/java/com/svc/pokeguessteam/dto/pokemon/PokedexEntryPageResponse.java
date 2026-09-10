package com.svc.pokeguessteam.dto.pokemon;

import org.springframework.data.domain.Page;

import java.util.List;

public record PokedexEntryPageResponse(
        List<PokedexEntryDto> content,
        int page,
        int size,
        long totalElements,
        int totalPages,
        boolean first,
        boolean last
) {
    public static PokedexEntryPageResponse from(Page<PokedexEntryDto> page) {
        return new PokedexEntryPageResponse(
                page.getContent(),
                page.getNumber(),
                page.getSize(),
                page.getTotalElements(),
                page.getTotalPages(),
                page.isFirst(),
                page.isLast()
        );
    }
}
