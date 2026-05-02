package com.svc.pokeguessteam.config.seed;

import com.svc.pokeguessteam.model.enums.EvolutionStage;
import com.svc.pokeguessteam.model.enums.PokedexColor;
import com.svc.pokeguessteam.model.enums.PokemonType;

import java.util.List;

public final class Generation6Seed {

    private Generation6Seed() {
    }

    public static List<PokemonSeedEntry> entries() {
        return List.of(
                PokemonSeedEntry.pokemon(650, "Chespin", PokemonType.GRASS, null, 6, PokedexColor.GREEN, 0.4, 9.0, EvolutionStage.BASE, 16, 352),
                PokemonSeedEntry.pokemon(651, "Quilladin", PokemonType.GRASS, null, 6, PokedexColor.GREEN, 0.7, 29.0, EvolutionStage.FIRST_STAGE, 36, 352),
                PokemonSeedEntry.pokemon(652, "Chesnaught", PokemonType.GRASS, PokemonType.FIGHTING, 6, PokedexColor.GREEN, 1.6, 90.0, EvolutionStage.SECOND_STAGE, null, 352),
                PokemonSeedEntry.pokemon(653, "Fennekin", PokemonType.FIRE, null, 6, PokedexColor.RED, 0.4, 9.4, EvolutionStage.BASE, 16, 353),
                PokemonSeedEntry.pokemon(654, "Braixen", PokemonType.FIRE, null, 6, PokedexColor.RED, 1.0, 14.5, EvolutionStage.FIRST_STAGE, 36, 353),
                PokemonSeedEntry.pokemon(655, "Delphox", PokemonType.FIRE, PokemonType.PSYCHIC, 6, PokedexColor.RED, 1.5, 39.0, EvolutionStage.SECOND_STAGE, null, 353),
                PokemonSeedEntry.pokemon(656, "Froakie", PokemonType.WATER, null, 6, PokedexColor.BLUE, 0.3, 7.0, EvolutionStage.BASE, 16, 354),
                PokemonSeedEntry.pokemon(657, "Frogadier", PokemonType.WATER, null, 6, PokedexColor.BLUE, 0.6, 10.9, EvolutionStage.FIRST_STAGE, 36, 354),
                PokemonSeedEntry.pokemon(658, "Greninja", PokemonType.WATER, PokemonType.DARK, 6, PokedexColor.BLUE, 1.5, 40.0, EvolutionStage.SECOND_STAGE, null, 354),
                PokemonSeedEntry.pokemon(659, "Bunnelby", PokemonType.NORMAL, null, 6, PokedexColor.BROWN, 0.4, 5.0, EvolutionStage.BASE, 20, 355),
                PokemonSeedEntry.pokemon(660, "Diggersby", PokemonType.NORMAL, PokemonType.GROUND, 6, PokedexColor.BROWN, 1.0, 42.4, EvolutionStage.SECOND_STAGE, null, 355),
                PokemonSeedEntry.pokemon(661, "Fletchling", PokemonType.NORMAL, PokemonType.FLYING, 6, PokedexColor.RED, 0.3, 1.7, EvolutionStage.BASE, 17, 356),
                PokemonSeedEntry.pokemon(662, "Fletchinder", PokemonType.FIRE, PokemonType.FLYING, 6, PokedexColor.RED, 0.7, 16.0, EvolutionStage.FIRST_STAGE, 35, 356),
                PokemonSeedEntry.pokemon(663, "Talonflame", PokemonType.FIRE, PokemonType.FLYING, 6, PokedexColor.RED, 1.2, 24.5, EvolutionStage.SECOND_STAGE, null, 356),
                PokemonSeedEntry.pokemon(664, "Scatterbug", PokemonType.BUG, null, 6, PokedexColor.BLACK, 0.3, 2.5, EvolutionStage.BASE, 9, 357),
                PokemonSeedEntry.pokemon(665, "Spewpa", PokemonType.BUG, null, 6, PokedexColor.BLACK, 0.3, 8.4, EvolutionStage.FIRST_STAGE, 12, 357),
                PokemonSeedEntry.pokemon(666, "Vivillon", PokemonType.BUG, PokemonType.FLYING, 6, PokedexColor.WHITE, 1.2, 17.0, EvolutionStage.SECOND_STAGE, null, 357),
                PokemonSeedEntry.pokemon(667, "Litleo", PokemonType.FIRE, PokemonType.NORMAL, 6, PokedexColor.BROWN, 0.6, 13.5, EvolutionStage.BASE, 35, 358),
                PokemonSeedEntry.pokemon(668, "Pyroar", PokemonType.FIRE, PokemonType.NORMAL, 6, PokedexColor.BROWN, 1.5, 81.5, EvolutionStage.SECOND_STAGE, null, 358),
                PokemonSeedEntry.pokemon(669, "Flabébé", PokemonType.FAIRY, null, 6, PokedexColor.WHITE, 0.1, 0.1, EvolutionStage.BASE, 19, 359),
                PokemonSeedEntry.pokemon(670, "Floette", PokemonType.FAIRY, null, 6, PokedexColor.WHITE, 0.2, 0.9, EvolutionStage.FIRST_STAGE, null, 359),
                PokemonSeedEntry.pokemon(671, "Florges", PokemonType.FAIRY, null, 6, PokedexColor.WHITE, 1.1, 10.0, EvolutionStage.SECOND_STAGE, null, 359),
                PokemonSeedEntry.pokemon(672, "Skiddo", PokemonType.GRASS, null, 6, PokedexColor.BROWN, 0.9, 31.0, EvolutionStage.BASE, 32, 360),
                PokemonSeedEntry.pokemon(673, "Gogoat", PokemonType.GRASS, null, 6, PokedexColor.BROWN, 1.7, 91.0, EvolutionStage.SECOND_STAGE, null, 360),
                PokemonSeedEntry.pokemon(674, "Pancham", PokemonType.FIGHTING, null, 6, PokedexColor.WHITE, 0.6, 8.0, EvolutionStage.BASE, 32, 361),
                PokemonSeedEntry.pokemon(675, "Pangoro", PokemonType.FIGHTING, PokemonType.DARK, 6, PokedexColor.WHITE, 2.1, 136.0, EvolutionStage.SECOND_STAGE, null, 361),
                PokemonSeedEntry.pokemon(676, "Furfrou", PokemonType.NORMAL, null, 6, PokedexColor.WHITE, 1.2, 28.0, null, null, 362),
                PokemonSeedEntry.pokemon(677, "Espurr", PokemonType.PSYCHIC, null, 6, PokedexColor.GRAY, 0.3, 3.5, EvolutionStage.BASE, 25, 363),
                PokemonSeedEntry.pokemon(678, "Meowstic", PokemonType.PSYCHIC, null, 6, PokedexColor.BLUE, 0.6, 8.5, EvolutionStage.SECOND_STAGE, null, 363),
                PokemonSeedEntry.pokemon(679, "Honedge", PokemonType.STEEL, PokemonType.GHOST, 6, PokedexColor.BROWN, 0.8, 2.0, EvolutionStage.BASE, 35, 364),
                PokemonSeedEntry.pokemon(680, "Doublade", PokemonType.STEEL, PokemonType.GHOST, 6, PokedexColor.BROWN, 0.8, 4.5, EvolutionStage.FIRST_STAGE, null, 364),
                PokemonSeedEntry.pokemon(681, "Aegislash", PokemonType.STEEL, PokemonType.GHOST, 6, PokedexColor.BROWN, 1.7, 53.0, EvolutionStage.SECOND_STAGE, null, 364),
                PokemonSeedEntry.pokemon(682, "Spritzee", PokemonType.FAIRY, null, 6, PokedexColor.PINK, 0.2, 0.5, EvolutionStage.BASE, null, 365),
                PokemonSeedEntry.pokemon(683, "Aromatisse", PokemonType.FAIRY, null, 6, PokedexColor.PINK, 0.8, 15.5, EvolutionStage.SECOND_STAGE, null, 365),
                PokemonSeedEntry.pokemon(684, "Swirlix", PokemonType.FAIRY, null, 6, PokedexColor.WHITE, 0.4, 3.5, EvolutionStage.BASE, null, 366),
                PokemonSeedEntry.pokemon(685, "Slurpuff", PokemonType.FAIRY, null, 6, PokedexColor.WHITE, 0.8, 5.0, EvolutionStage.SECOND_STAGE, null, 366),
                PokemonSeedEntry.pokemon(686, "Inkay", PokemonType.DARK, PokemonType.PSYCHIC, 6, PokedexColor.BLUE, 0.4, 3.5, EvolutionStage.BASE, 30, 367),
                PokemonSeedEntry.pokemon(687, "Malamar", PokemonType.DARK, PokemonType.PSYCHIC, 6, PokedexColor.BLUE, 1.5, 47.0, EvolutionStage.SECOND_STAGE, null, 367),
                PokemonSeedEntry.pokemon(688, "Binacle", PokemonType.ROCK, PokemonType.WATER, 6, PokedexColor.BROWN, 0.5, 31.0, EvolutionStage.BASE, 39, 368),
                PokemonSeedEntry.pokemon(689, "Barbaracle", PokemonType.ROCK, PokemonType.WATER, 6, PokedexColor.BROWN, 1.3, 96.0, EvolutionStage.SECOND_STAGE, null, 368),
                PokemonSeedEntry.pokemon(690, "Skrelp", PokemonType.POISON, PokemonType.WATER, 6, PokedexColor.BROWN, 0.5, 7.3, EvolutionStage.BASE, 48, 369),
                PokemonSeedEntry.pokemon(691, "Dragalge", PokemonType.POISON, PokemonType.DRAGON, 6, PokedexColor.BROWN, 1.8, 81.5, EvolutionStage.SECOND_STAGE, null, 369),
                PokemonSeedEntry.pokemon(692, "Clauncher", PokemonType.WATER, null, 6, PokedexColor.BLUE, 0.5, 8.3, EvolutionStage.BASE, 37, 370),
                PokemonSeedEntry.pokemon(693, "Clawitzer", PokemonType.WATER, null, 6, PokedexColor.BLUE, 1.3, 35.3, EvolutionStage.SECOND_STAGE, null, 370),
                PokemonSeedEntry.pokemon(694, "Helioptile", PokemonType.ELECTRIC, PokemonType.NORMAL, 6, PokedexColor.YELLOW, 0.5, 6.0, EvolutionStage.BASE, null, 371),
                PokemonSeedEntry.pokemon(695, "Heliolisk", PokemonType.ELECTRIC, PokemonType.NORMAL, 6, PokedexColor.YELLOW, 1.0, 21.0, EvolutionStage.SECOND_STAGE, null, 371),
                PokemonSeedEntry.pokemon(696, "Tyrunt", PokemonType.ROCK, PokemonType.DRAGON, 6, PokedexColor.BROWN, 0.8, 26.0, EvolutionStage.BASE, 39, 372),
                PokemonSeedEntry.pokemon(697, "Tyrantrum", PokemonType.ROCK, PokemonType.DRAGON, 6, PokedexColor.RED, 2.5, 270.0, EvolutionStage.SECOND_STAGE, null, 372),
                PokemonSeedEntry.pokemon(698, "Amaura", PokemonType.ROCK, PokemonType.ICE, 6, PokedexColor.BLUE, 1.3, 25.2, EvolutionStage.BASE, 39, 373),
                PokemonSeedEntry.pokemon(699, "Aurorus", PokemonType.ROCK, PokemonType.ICE, 6, PokedexColor.BLUE, 2.7, 225.0, EvolutionStage.SECOND_STAGE, null, 373),
                PokemonSeedEntry.pokemon(700, "Sylveon", PokemonType.FAIRY, null, 6, PokedexColor.PINK, 1.0, 23.5, EvolutionStage.SECOND_STAGE, null, 70),
                PokemonSeedEntry.pokemon(701, "Hawlucha", PokemonType.FIGHTING, PokemonType.FLYING, 6, PokedexColor.GREEN, 0.8, 21.5, null, null, 374),
                PokemonSeedEntry.pokemon(702, "Dedenne", PokemonType.ELECTRIC, PokemonType.FAIRY, 6, PokedexColor.YELLOW, 0.2, 2.2, null, null, 375),
                PokemonSeedEntry.pokemon(703, "Carbink", PokemonType.ROCK, PokemonType.FAIRY, 6, PokedexColor.GRAY, 0.3, 5.7, null, null, 376),
                PokemonSeedEntry.pokemon(704, "Goomy", PokemonType.DRAGON, null, 6, PokedexColor.PURPLE, 0.3, 2.8, EvolutionStage.BASE, 40, 377),
                PokemonSeedEntry.pokemon(705, "Sliggoo", PokemonType.DRAGON, null, 6, PokedexColor.PURPLE, 0.8, 17.5, EvolutionStage.FIRST_STAGE, 50, 377),
                PokemonSeedEntry.pokemon(706, "Goodra", PokemonType.DRAGON, null, 6, PokedexColor.PURPLE, 2.0, 150.5, EvolutionStage.SECOND_STAGE, null, 377),
                PokemonSeedEntry.pokemon(707, "Klefki", PokemonType.STEEL, PokemonType.FAIRY, 6, PokedexColor.GRAY, 0.2, 3.0, null, null, 378),
                PokemonSeedEntry.pokemon(708, "Phantump", PokemonType.GHOST, PokemonType.GRASS, 6, PokedexColor.BROWN, 0.4, 7.0, EvolutionStage.BASE, null, 379),
                PokemonSeedEntry.pokemon(709, "Trevenant", PokemonType.GHOST, PokemonType.GRASS, 6, PokedexColor.BROWN, 1.5, 71.0, EvolutionStage.SECOND_STAGE, null, 379),
                PokemonSeedEntry.pokemon(710, "Pumpkaboo", PokemonType.GHOST, PokemonType.GRASS, 6, PokedexColor.BROWN, 0.4, 5.0, EvolutionStage.BASE, null, 380),
                PokemonSeedEntry.pokemon(711, "Gourgeist", PokemonType.GHOST, PokemonType.GRASS, 6, PokedexColor.BROWN, 0.9, 12.5, EvolutionStage.SECOND_STAGE, null, 380),
                PokemonSeedEntry.pokemon(712, "Bergmite", PokemonType.ICE, null, 6, PokedexColor.BLUE, 1.0, 99.5, EvolutionStage.BASE, 37, 381),
                PokemonSeedEntry.pokemon(713, "Avalugg", PokemonType.ICE, null, 6, PokedexColor.BLUE, 2.0, 505.0, EvolutionStage.SECOND_STAGE, null, 381),
                PokemonSeedEntry.pokemon(714, "Noibat", PokemonType.FLYING, PokemonType.DRAGON, 6, PokedexColor.PURPLE, 0.5, 8.0, EvolutionStage.BASE, 48, 382),
                PokemonSeedEntry.pokemon(715, "Noivern", PokemonType.FLYING, PokemonType.DRAGON, 6, PokedexColor.PURPLE, 1.5, 85.0, EvolutionStage.SECOND_STAGE, null, 382),
                PokemonSeedEntry.pokemon(716, "Xerneas", PokemonType.FAIRY, null, 6, PokedexColor.BLUE, 3.0, 215.0, null, null, 383),
                PokemonSeedEntry.pokemon(717, "Yveltal", PokemonType.DARK, PokemonType.FLYING, 6, PokedexColor.RED, 5.8, 203.0, null, null, 384),
                PokemonSeedEntry.pokemon(718, "Zygarde", PokemonType.DRAGON, PokemonType.GROUND, 6, PokedexColor.GREEN, 5.0, 305.0, null, null, 385),
                PokemonSeedEntry.pokemon(719, "Diancie", PokemonType.ROCK, PokemonType.FAIRY, 6, PokedexColor.PINK, 0.7, 8.8, null, null, 386),
                PokemonSeedEntry.pokemon(720, "Hoopa", PokemonType.PSYCHIC, PokemonType.GHOST, 6, PokedexColor.PURPLE, 0.5, 9.0, null, null, 387),
                PokemonSeedEntry.pokemon(721, "Volcanion", PokemonType.FIRE, PokemonType.WATER, 6, PokedexColor.BROWN, 1.7, 195.0, null, null, 388)
        );
    }
}
