package com.svc.pokeguessteam.dto.pokemon;

import com.svc.pokeguessteam.model.user.UserPokemonInventoryModel;
import org.springframework.data.domain.Page;

import java.util.List;

public record PcPageResponse(
        List<PcLineDto> content,
        int page,
        int size,
        long totalElements,
        int totalPages,
        boolean first,
        boolean last
) {
    public static PcPageResponse from(Page<UserPokemonInventoryModel> page) {
        List<PcLineDto> items = page.getContent().stream().map(PcLineDto::from).toList();
        return new PcPageResponse(
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
