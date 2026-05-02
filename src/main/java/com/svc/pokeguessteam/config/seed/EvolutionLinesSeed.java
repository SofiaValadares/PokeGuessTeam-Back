package com.svc.pokeguessteam.config.seed;

import com.svc.pokeguessteam.model.enums.PokemonRarity;

import java.util.List;

/**
 * Linhas evolutivas por caminho na API PokeAPI.
 * Membros: {@code pokedexNumber} (número nacional), alinhado a {@link com.svc.pokeguessteam.model.PokemonModel#getPokedexNumber()}.
 */
public final class EvolutionLinesSeed {

    private EvolutionLinesSeed() {
    }

    public static List<EvolutionLineSeedEntry> entries() {
        return List.of(
                EvolutionLineSeedEntry.line(1, PokemonRarity.RARE, List.of(1,2,3)), // Bulbasaur → Ivysaur → Venusaur
                EvolutionLineSeedEntry.line(2, PokemonRarity.RARE, List.of(4,5,6)), // Charmander → Charmeleon → Charizard
                EvolutionLineSeedEntry.line(3, PokemonRarity.RARE, List.of(7,8,9)), // Squirtle → Wartortle → Blastoise
                EvolutionLineSeedEntry.line(4, PokemonRarity.COMMON, List.of(10,11,12)), // Caterpie → Metapod → Butterfree
                EvolutionLineSeedEntry.line(5, PokemonRarity.COMMON, List.of(13,14,15)), // Weedle → Kakuna → Beedrill
                EvolutionLineSeedEntry.line(6, PokemonRarity.COMMON, List.of(16,17,18)), // Pidgey → Pidgeotto → Pidgeot
                EvolutionLineSeedEntry.line(7, PokemonRarity.COMMON, List.of(19,20)), // Rattata → Raticate
                EvolutionLineSeedEntry.line(8, PokemonRarity.COMMON, List.of(21,22)), // Spearow → Fearow
                EvolutionLineSeedEntry.line(9, PokemonRarity.COMMON, List.of(23,24)), // Ekans → Arbok
                EvolutionLineSeedEntry.line(10, PokemonRarity.COMMON, List.of(27,28)), // Sandshrew → Sandslash
                EvolutionLineSeedEntry.line(11, PokemonRarity.RARE, List.of(29,30,31)), // Nidoran F → Nidorina → Nidoqueen
                EvolutionLineSeedEntry.line(12, PokemonRarity.RARE, List.of(32,33,34)), // Nidoran M → Nidorino → Nidoking
                EvolutionLineSeedEntry.line(13, PokemonRarity.RARE, List.of(37,38)), // Vulpix → Ninetales
                EvolutionLineSeedEntry.line(14, PokemonRarity.COMMON, List.of(41,42,169)), // Zubat → Golbat → Crobat
                EvolutionLineSeedEntry.line(15, PokemonRarity.COMMON, List.of(43,44,45)), // Oddish → Gloom → Vileplume
                EvolutionLineSeedEntry.line(16, PokemonRarity.COMMON, List.of(43,44,182)), // Oddish → Gloom → Bellossom
                EvolutionLineSeedEntry.line(17, PokemonRarity.COMMON, List.of(46,47)), // Paras → Parasect
                EvolutionLineSeedEntry.line(18, PokemonRarity.COMMON, List.of(48,49)), // Venonat → Venomoth
                EvolutionLineSeedEntry.line(19, PokemonRarity.COMMON, List.of(50,51)), // Diglett → Dugtrio
                EvolutionLineSeedEntry.line(20, PokemonRarity.RARE, List.of(52,53)), // Meowth → Persian
                EvolutionLineSeedEntry.line(21, PokemonRarity.COMMON, List.of(52,863)), // Meowth → Perrserker
                EvolutionLineSeedEntry.line(22, PokemonRarity.RARE, List.of(54,55)), // Psyduck → Golduck
                EvolutionLineSeedEntry.line(23, PokemonRarity.RARE, List.of(56,57,979)), // Mankey → Primeape → Annihilape
                EvolutionLineSeedEntry.line(24, PokemonRarity.RARE, List.of(58,59)), // Growlithe → Arcanine
                EvolutionLineSeedEntry.line(25, PokemonRarity.COMMON, List.of(60,61,62)), // Poliwag → Poliwhirl → Poliwrath
                EvolutionLineSeedEntry.line(26, PokemonRarity.COMMON, List.of(60,61,186)), // Poliwag → Poliwhirl → Politoed
                EvolutionLineSeedEntry.line(27, PokemonRarity.RARE, List.of(63,64,65)), // Abra → Kadabra → Alakazam
                EvolutionLineSeedEntry.line(28, PokemonRarity.RARE, List.of(66,67,68)), // Machop → Machoke → Machamp
                EvolutionLineSeedEntry.line(29, PokemonRarity.COMMON, List.of(69,70,71)), // Bellsprout → Weepinbell → Victreebel
                EvolutionLineSeedEntry.line(30, PokemonRarity.COMMON, List.of(72,73)), // Tentacool → Tentacruel
                EvolutionLineSeedEntry.line(31, PokemonRarity.RARE, List.of(74,75,76)), // Geodude → Graveler → Golem
                EvolutionLineSeedEntry.line(32, PokemonRarity.RARE, List.of(77,78)), // Ponyta → Rapidash
                EvolutionLineSeedEntry.line(33, PokemonRarity.COMMON, List.of(79,80)), // Slowpoke → Slowbro
                EvolutionLineSeedEntry.line(34, PokemonRarity.COMMON, List.of(79,199)), // Slowpoke → Slowking
                EvolutionLineSeedEntry.line(35, PokemonRarity.RARE, List.of(81,82,462)), // Magnemite → Magneton → Magnezone
                EvolutionLineSeedEntry.line(36, PokemonRarity.RARE, List.of(83,865)), // Farfetch'd → Sirfetch'd
                EvolutionLineSeedEntry.line(37, PokemonRarity.COMMON, List.of(84,85)), // Doduo → Dodrio
                EvolutionLineSeedEntry.line(38, PokemonRarity.COMMON, List.of(86,87)), // Seel → Dewgong
                EvolutionLineSeedEntry.line(39, PokemonRarity.COMMON, List.of(88,89)), // Grimer → Muk
                EvolutionLineSeedEntry.line(40, PokemonRarity.COMMON, List.of(90,91)), // Shellder → Cloyster
                EvolutionLineSeedEntry.line(41, PokemonRarity.RARE, List.of(92,93,94)), // Gastly → Haunter → Gengar
                EvolutionLineSeedEntry.line(42, PokemonRarity.RARE, List.of(95,208)), // Onix → Steelix
                EvolutionLineSeedEntry.line(43, PokemonRarity.COMMON, List.of(96,97)), // Drowzee → Hypno
                EvolutionLineSeedEntry.line(44, PokemonRarity.COMMON, List.of(98,99)), // Krabby → Kingler
                EvolutionLineSeedEntry.line(45, PokemonRarity.COMMON, List.of(100,101)), // Voltorb → Electrode
                EvolutionLineSeedEntry.line(46, PokemonRarity.RARE, List.of(102,103)), // Exeggcute → Exeggutor
                EvolutionLineSeedEntry.line(47, PokemonRarity.RARE, List.of(104,105)), // Cubone → Marowak
                EvolutionLineSeedEntry.line(48, PokemonRarity.RARE, List.of(108,463)), // Lickitung → Lickilicky
                EvolutionLineSeedEntry.line(49, PokemonRarity.COMMON, List.of(109,110)), // Koffing → Weezing
                EvolutionLineSeedEntry.line(50, PokemonRarity.COMMON, List.of(111,112,464)), // Rhyhorn → Rhydon → Rhyperior
                EvolutionLineSeedEntry.line(51, PokemonRarity.COMMON, List.of(114,465)), // Tangela → Tangrowth
                EvolutionLineSeedEntry.line(52, PokemonRarity.RARE, List.of(115)), // Kangaskhan
                EvolutionLineSeedEntry.line(53, PokemonRarity.RARE, List.of(116,117,230)), // Horsea → Seadra → Kingdra
                EvolutionLineSeedEntry.line(54, PokemonRarity.COMMON, List.of(118,119)), // Goldeen → Seaking
                EvolutionLineSeedEntry.line(55, PokemonRarity.COMMON, List.of(120,121)), // Staryu → Starmie
                EvolutionLineSeedEntry.line(56, PokemonRarity.RARE, List.of(123,212)), // Scyther → Scizor
                EvolutionLineSeedEntry.line(57, PokemonRarity.RARE, List.of(123,900)), // Scyther → Kleavor
                EvolutionLineSeedEntry.line(58, PokemonRarity.RARE, List.of(127)), // Pinsir
                EvolutionLineSeedEntry.line(59, PokemonRarity.RARE, List.of(128)), // Tauros
                EvolutionLineSeedEntry.line(60, PokemonRarity.RARE, List.of(129,130)), // Magikarp → Gyarados
                EvolutionLineSeedEntry.line(61, PokemonRarity.RARE, List.of(131)), // Lapras
                EvolutionLineSeedEntry.line(62, PokemonRarity.RARE, List.of(132)), // Ditto
                EvolutionLineSeedEntry.line(63, PokemonRarity.RARE, List.of(133,134)), // Eevee → Vaporeon
                EvolutionLineSeedEntry.line(64, PokemonRarity.RARE, List.of(133,135)), // Eevee → Jolteon
                EvolutionLineSeedEntry.line(65, PokemonRarity.RARE, List.of(133,136)), // Eevee → Flareon
                EvolutionLineSeedEntry.line(66, PokemonRarity.RARE, List.of(133,196)), // Eevee → Espeon
                EvolutionLineSeedEntry.line(67, PokemonRarity.RARE, List.of(133,197)), // Eevee → Umbreon
                EvolutionLineSeedEntry.line(68, PokemonRarity.RARE, List.of(133,470)), // Eevee → Leafeon
                EvolutionLineSeedEntry.line(69, PokemonRarity.RARE, List.of(133,471)), // Eevee → Glaceon
                EvolutionLineSeedEntry.line(70, PokemonRarity.RARE, List.of(133,700)), // Eevee → Sylveon
                EvolutionLineSeedEntry.line(71, PokemonRarity.RARE, List.of(137,233,474)), // Porygon → Porygon2 → Porygon-Z
                EvolutionLineSeedEntry.line(72, PokemonRarity.RARE, List.of(138,139)), // Omanyte → Omastar
                EvolutionLineSeedEntry.line(73, PokemonRarity.RARE, List.of(140,141)), // Kabuto → Kabutops
                EvolutionLineSeedEntry.line(74, PokemonRarity.RARE, List.of(142)), // Aerodactyl
                EvolutionLineSeedEntry.line(75, PokemonRarity.LEGENDARY, List.of(144)), // Articuno
                EvolutionLineSeedEntry.line(76, PokemonRarity.LEGENDARY, List.of(145)), // Zapdos
                EvolutionLineSeedEntry.line(77, PokemonRarity.LEGENDARY, List.of(146)), // Moltres
                EvolutionLineSeedEntry.line(78, PokemonRarity.RARE, List.of(147,148,149)), // Dratini → Dragonair → Dragonite
                EvolutionLineSeedEntry.line(79, PokemonRarity.LEGENDARY, List.of(150)), // Mewtwo
                EvolutionLineSeedEntry.line(80, PokemonRarity.MYTHICAL, List.of(151)), // Mew
                EvolutionLineSeedEntry.line(81, PokemonRarity.RARE, List.of(152,153,154)), // Chikorita → Bayleef → Meganium
                EvolutionLineSeedEntry.line(82, PokemonRarity.RARE, List.of(155,156,157)), // Cyndaquil → Quilava → Typhlosion
                EvolutionLineSeedEntry.line(83, PokemonRarity.RARE, List.of(158,159,160)), // Totodile → Croconaw → Feraligatr
                EvolutionLineSeedEntry.line(84, PokemonRarity.COMMON, List.of(161,162)), // Sentret → Furret
                EvolutionLineSeedEntry.line(85, PokemonRarity.COMMON, List.of(163,164)), // Hoothoot → Noctowl
                EvolutionLineSeedEntry.line(86, PokemonRarity.COMMON, List.of(165,166)), // Ledyba → Ledian
                EvolutionLineSeedEntry.line(87, PokemonRarity.COMMON, List.of(167,168)), // Spinarak → Ariados
                EvolutionLineSeedEntry.line(88, PokemonRarity.COMMON, List.of(170,171)), // Chinchou → Lanturn
                EvolutionLineSeedEntry.line(89, PokemonRarity.RARE, List.of(172, 25,26)), // Pichu → Pikachu → Raichu
                EvolutionLineSeedEntry.line(90, PokemonRarity.RARE, List.of(35,36,173)), // Clefairy → Clefable → Cleffa
                EvolutionLineSeedEntry.line(91, PokemonRarity.COMMON, List.of(39,40,174)), // Jigglypuff → Wigglytuff → Igglybuff
                EvolutionLineSeedEntry.line(92, PokemonRarity.RARE, List.of(175,176,468)), // Togepi → Togetic → Togekiss
                EvolutionLineSeedEntry.line(93, PokemonRarity.COMMON, List.of(177,178)), // Natu → Xatu
                EvolutionLineSeedEntry.line(94, PokemonRarity.RARE, List.of(179,180,181)), // Mareep → Flaaffy → Ampharos
                EvolutionLineSeedEntry.line(95, PokemonRarity.RARE, List.of(187,188,189)), // Hoppip → Skiploom → Jumpluff
                EvolutionLineSeedEntry.line(96, PokemonRarity.RARE, List.of(190,424)), // Aipom → Ambipom
                EvolutionLineSeedEntry.line(97, PokemonRarity.COMMON, List.of(191,192)), // Sunkern → Sunflora
                EvolutionLineSeedEntry.line(98, PokemonRarity.RARE, List.of(193,469)), // Yanma → Yanmega
                EvolutionLineSeedEntry.line(99, PokemonRarity.RARE, List.of(194,195)), // Wooper → Quagsire
                EvolutionLineSeedEntry.line(100, PokemonRarity.RARE, List.of(194,980)), // Wooper → Clodsire
                EvolutionLineSeedEntry.line(101, PokemonRarity.RARE, List.of(198,430)), // Murkrow → Honchkrow
                EvolutionLineSeedEntry.line(102, PokemonRarity.RARE, List.of(200,429)), // Misdreavus → Mismagius
                EvolutionLineSeedEntry.line(103, PokemonRarity.RARE, List.of(201)), // Unown
                EvolutionLineSeedEntry.line(104, PokemonRarity.RARE, List.of(203,981)), // Girafarig → Farigiraf
                EvolutionLineSeedEntry.line(105, PokemonRarity.COMMON, List.of(204,205)), // Pineco → Forretress
                EvolutionLineSeedEntry.line(106, PokemonRarity.COMMON, List.of(206,982)), // Dunsparce → Dudunsparce
                EvolutionLineSeedEntry.line(107, PokemonRarity.RARE, List.of(207,472)), // Gligar → Gliscor
                EvolutionLineSeedEntry.line(108, PokemonRarity.COMMON, List.of(209,210)), // Snubbull → Granbull
                EvolutionLineSeedEntry.line(109, PokemonRarity.COMMON, List.of(211,904)), // Qwilfish → Overqwil
                EvolutionLineSeedEntry.line(110, PokemonRarity.COMMON, List.of(213)), // Shuckle
                EvolutionLineSeedEntry.line(111, PokemonRarity.RARE, List.of(214)), // Heracross
                EvolutionLineSeedEntry.line(112, PokemonRarity.RARE, List.of(215,461)), // Sneasel → Weavile
                EvolutionLineSeedEntry.line(113, PokemonRarity.RARE, List.of(215,903)), // Sneasel → Sneasler
                EvolutionLineSeedEntry.line(114, PokemonRarity.COMMON, List.of(216,217,901)), // Teddiursa → Ursaring → Ursaluna
                EvolutionLineSeedEntry.line(115, PokemonRarity.COMMON, List.of(218,219)), // Slugma → Magcargo
                EvolutionLineSeedEntry.line(116, PokemonRarity.COMMON, List.of(220,221,473)), // Swinub → Piloswine → Mamoswine
                EvolutionLineSeedEntry.line(117, PokemonRarity.RARE, List.of(222,864)), // Corsola → Cursola
                EvolutionLineSeedEntry.line(118, PokemonRarity.COMMON, List.of(223,224)), // Remoraid → Octillery
                EvolutionLineSeedEntry.line(119, PokemonRarity.RARE, List.of(225)), // Delibird
                EvolutionLineSeedEntry.line(120, PokemonRarity.RARE, List.of(227)), // Skarmory
                EvolutionLineSeedEntry.line(121, PokemonRarity.RARE, List.of(228,229)), // Houndour → Houndoom
                EvolutionLineSeedEntry.line(122, PokemonRarity.COMMON, List.of(231,232)), // Phanpy → Donphan
                EvolutionLineSeedEntry.line(123, PokemonRarity.RARE, List.of(234,899)), // Stantler → Wyrdeer
                EvolutionLineSeedEntry.line(124, PokemonRarity.RARE, List.of(235)), // Smeargle
                EvolutionLineSeedEntry.line(125, PokemonRarity.RARE, List.of(106,236)), // Hitmonlee → Tyrogue
                EvolutionLineSeedEntry.line(126, PokemonRarity.RARE, List.of(107,236)), // Hitmonchan → Tyrogue
                EvolutionLineSeedEntry.line(127, PokemonRarity.RARE, List.of(236,237)), // Tyrogue → Hitmontop
                EvolutionLineSeedEntry.line(128, PokemonRarity.RARE, List.of(124,238)), // Jynx → Smoochum
                EvolutionLineSeedEntry.line(129, PokemonRarity.RARE, List.of(125,239,466)), // Electabuzz → Elekid → Electivire
                EvolutionLineSeedEntry.line(130, PokemonRarity.RARE, List.of(126,240,467)), // Magmar → Magby → Magmortar
                EvolutionLineSeedEntry.line(131, PokemonRarity.RARE, List.of(241)), // Miltank
                EvolutionLineSeedEntry.line(132, PokemonRarity.LEGENDARY, List.of(243)), // Raikou
                EvolutionLineSeedEntry.line(133, PokemonRarity.LEGENDARY, List.of(244)), // Entei
                EvolutionLineSeedEntry.line(134, PokemonRarity.LEGENDARY, List.of(245)), // Suicune
                EvolutionLineSeedEntry.line(135, PokemonRarity.RARE, List.of(246,247,248)), // Larvitar → Pupitar → Tyranitar
                EvolutionLineSeedEntry.line(136, PokemonRarity.LEGENDARY, List.of(249)), // Lugia
                EvolutionLineSeedEntry.line(137, PokemonRarity.LEGENDARY, List.of(250)), // Ho-Oh
                EvolutionLineSeedEntry.line(138, PokemonRarity.MYTHICAL, List.of(251)), // Celebi
                EvolutionLineSeedEntry.line(139, PokemonRarity.RARE, List.of(252,253,254)), // Treecko → Grovyle → Sceptile
                EvolutionLineSeedEntry.line(140, PokemonRarity.RARE, List.of(255,256,257)), // Torchic → Combusken → Blaziken
                EvolutionLineSeedEntry.line(141, PokemonRarity.RARE, List.of(258,259,260)), // Mudkip → Marshtomp → Swampert
                EvolutionLineSeedEntry.line(142, PokemonRarity.COMMON, List.of(261,262)), // Poochyena → Mightyena
                EvolutionLineSeedEntry.line(143, PokemonRarity.COMMON, List.of(263,264,862)), // Zigzagoon → Linoone → Obstagoon
                EvolutionLineSeedEntry.line(144, PokemonRarity.COMMON, List.of(265,266,267)), // Wurmple → Silcoon → Beautifly
                EvolutionLineSeedEntry.line(145, PokemonRarity.COMMON, List.of(265,268,269)), // Wurmple → Cascoon → Dustox
                EvolutionLineSeedEntry.line(146, PokemonRarity.COMMON, List.of(270,271,272)), // Lotad → Lombre → Ludicolo
                EvolutionLineSeedEntry.line(147, PokemonRarity.COMMON, List.of(273,274,275)), // Seedot → Nuzleaf → Shiftry
                EvolutionLineSeedEntry.line(148, PokemonRarity.RARE, List.of(276,277)), // Taillow → Swellow
                EvolutionLineSeedEntry.line(149, PokemonRarity.RARE, List.of(278,279)), // Wingull → Pelipper
                EvolutionLineSeedEntry.line(150, PokemonRarity.RARE, List.of(280,281,282)), // Ralts → Kirlia → Gardevoir
                EvolutionLineSeedEntry.line(151, PokemonRarity.RARE, List.of(280,281,475)), // Ralts → Kirlia → Gallade
                EvolutionLineSeedEntry.line(152, PokemonRarity.COMMON, List.of(283,284)), // Surskit → Masquerain
                EvolutionLineSeedEntry.line(153, PokemonRarity.COMMON, List.of(285,286)), // Shroomish → Breloom
                EvolutionLineSeedEntry.line(154, PokemonRarity.RARE, List.of(287,288,289)), // Slakoth → Vigoroth → Slaking
                EvolutionLineSeedEntry.line(155, PokemonRarity.COMMON, List.of(290,291)), // Nincada → Ninjask
                EvolutionLineSeedEntry.line(156, PokemonRarity.RARE, List.of(290,292)), // Nincada → Shedinja
                EvolutionLineSeedEntry.line(157, PokemonRarity.RARE, List.of(293,294,295)), // Whismur → Loudred → Exploud
                EvolutionLineSeedEntry.line(158, PokemonRarity.COMMON, List.of(296,297)), // Makuhita → Hariyama
                EvolutionLineSeedEntry.line(159, PokemonRarity.COMMON, List.of(183,184,298)), // Marill → Azumarill → Azurill
                EvolutionLineSeedEntry.line(160, PokemonRarity.COMMON, List.of(299,476)), // Nosepass → Probopass
                EvolutionLineSeedEntry.line(161, PokemonRarity.COMMON, List.of(300,301)), // Skitty → Delcatty
                EvolutionLineSeedEntry.line(162, PokemonRarity.RARE, List.of(302)), // Sableye
                EvolutionLineSeedEntry.line(163, PokemonRarity.RARE, List.of(303)), // Mawile
                EvolutionLineSeedEntry.line(164, PokemonRarity.RARE, List.of(304,305,306)), // Aron → Lairon → Aggron
                EvolutionLineSeedEntry.line(165, PokemonRarity.COMMON, List.of(307,308)), // Meditite → Medicham
                EvolutionLineSeedEntry.line(166, PokemonRarity.RARE, List.of(309,310)), // Electrike → Manectric
                EvolutionLineSeedEntry.line(167, PokemonRarity.COMMON, List.of(311)), // Plusle
                EvolutionLineSeedEntry.line(168, PokemonRarity.COMMON, List.of(312)), // Minun
                EvolutionLineSeedEntry.line(169, PokemonRarity.COMMON, List.of(313)), // Volbeat
                EvolutionLineSeedEntry.line(170, PokemonRarity.COMMON, List.of(314)), // Illumise
                EvolutionLineSeedEntry.line(171, PokemonRarity.COMMON, List.of(316,317)), // Gulpin → Swalot
                EvolutionLineSeedEntry.line(172, PokemonRarity.COMMON, List.of(318,319)), // Carvanha → Sharpedo
                EvolutionLineSeedEntry.line(173, PokemonRarity.COMMON, List.of(320,321)), // Wailmer → Wailord
                EvolutionLineSeedEntry.line(174, PokemonRarity.COMMON, List.of(322,323)), // Numel → Camerupt
                EvolutionLineSeedEntry.line(175, PokemonRarity.COMMON, List.of(324)), // Torkoal
                EvolutionLineSeedEntry.line(176, PokemonRarity.COMMON, List.of(325,326)), // Spoink → Grumpig
                EvolutionLineSeedEntry.line(177, PokemonRarity.COMMON, List.of(327)), // Spinda
                EvolutionLineSeedEntry.line(178, PokemonRarity.RARE, List.of(328,329,330)), // Trapinch → Vibrava → Flygon
                EvolutionLineSeedEntry.line(179, PokemonRarity.COMMON, List.of(331,332)), // Cacnea → Cacturne
                EvolutionLineSeedEntry.line(180, PokemonRarity.RARE, List.of(333,334)), // Swablu → Altaria
                EvolutionLineSeedEntry.line(181, PokemonRarity.COMMON, List.of(335)), // Zangoose
                EvolutionLineSeedEntry.line(182, PokemonRarity.COMMON, List.of(336)), // Seviper
                EvolutionLineSeedEntry.line(183, PokemonRarity.RARE, List.of(337)), // Lunatone
                EvolutionLineSeedEntry.line(184, PokemonRarity.RARE, List.of(338)), // Solrock
                EvolutionLineSeedEntry.line(185, PokemonRarity.COMMON, List.of(339,340)), // Barboach → Whiscash
                EvolutionLineSeedEntry.line(186, PokemonRarity.COMMON, List.of(341,342)), // Corphish → Crawdaunt
                EvolutionLineSeedEntry.line(187, PokemonRarity.COMMON, List.of(343,344)), // Baltoy → Claydol
                EvolutionLineSeedEntry.line(188, PokemonRarity.RARE, List.of(345,346)), // Lileep → Cradily
                EvolutionLineSeedEntry.line(189, PokemonRarity.RARE, List.of(347,348)), // Anorith → Armaldo
                EvolutionLineSeedEntry.line(190, PokemonRarity.RARE, List.of(349,350)), // Feebas → Milotic
                EvolutionLineSeedEntry.line(191, PokemonRarity.COMMON, List.of(351)), // Castform
                EvolutionLineSeedEntry.line(192, PokemonRarity.COMMON, List.of(352)), // Kecleon
                EvolutionLineSeedEntry.line(193, PokemonRarity.RARE, List.of(353,354)), // Shuppet → Banette
                EvolutionLineSeedEntry.line(194, PokemonRarity.RARE, List.of(355,356,477)), // Duskull → Dusclops → Dusknoir
                EvolutionLineSeedEntry.line(195, PokemonRarity.COMMON, List.of(357)), // Tropius
                EvolutionLineSeedEntry.line(196, PokemonRarity.RARE, List.of(359)), // Absol
                EvolutionLineSeedEntry.line(197, PokemonRarity.RARE, List.of(202,360)), // Wobbuffet → Wynaut
                EvolutionLineSeedEntry.line(198, PokemonRarity.COMMON, List.of(361,362)), // Snorunt → Glalie
                EvolutionLineSeedEntry.line(199, PokemonRarity.COMMON, List.of(361,478)), // Snorunt → Froslass
                EvolutionLineSeedEntry.line(200, PokemonRarity.RARE, List.of(363,364,365)), // Spheal → Sealeo → Walrein
                EvolutionLineSeedEntry.line(201, PokemonRarity.COMMON, List.of(366,367)), // Clamperl → Huntail
                EvolutionLineSeedEntry.line(202, PokemonRarity.COMMON, List.of(366,368)), // Clamperl → Gorebyss
                EvolutionLineSeedEntry.line(203, PokemonRarity.RARE, List.of(369)), // Relicanth
                EvolutionLineSeedEntry.line(204, PokemonRarity.COMMON, List.of(370)), // Luvdisc
                EvolutionLineSeedEntry.line(205, PokemonRarity.RARE, List.of(371,372,373)), // Bagon → Shelgon → Salamence
                EvolutionLineSeedEntry.line(206, PokemonRarity.RARE, List.of(374,375,376)), // Beldum → Metang → Metagross
                EvolutionLineSeedEntry.line(207, PokemonRarity.LEGENDARY, List.of(377)), // Regirock
                EvolutionLineSeedEntry.line(208, PokemonRarity.LEGENDARY, List.of(378)), // Regice
                EvolutionLineSeedEntry.line(209, PokemonRarity.LEGENDARY, List.of(379)), // Registeel
                EvolutionLineSeedEntry.line(210, PokemonRarity.LEGENDARY, List.of(380)), // Latias
                EvolutionLineSeedEntry.line(211, PokemonRarity.LEGENDARY, List.of(381)), // Latios
                EvolutionLineSeedEntry.line(212, PokemonRarity.LEGENDARY, List.of(382)), // Kyogre
                EvolutionLineSeedEntry.line(213, PokemonRarity.LEGENDARY, List.of(383)), // Groudon
                EvolutionLineSeedEntry.line(214, PokemonRarity.LEGENDARY, List.of(384)), // Rayquaza
                EvolutionLineSeedEntry.line(215, PokemonRarity.MYTHICAL, List.of(385)), // Jirachi
                EvolutionLineSeedEntry.line(216, PokemonRarity.MYTHICAL, List.of(386)), // Deoxys
                EvolutionLineSeedEntry.line(217, PokemonRarity.RARE, List.of(387,388,389)), // Turtwig → Grotle → Torterra
                EvolutionLineSeedEntry.line(218, PokemonRarity.RARE, List.of(390,391,392)), // Chimchar → Monferno → Infernape
                EvolutionLineSeedEntry.line(219, PokemonRarity.RARE, List.of(393,394,395)), // Piplup → Prinplup → Empoleon
                EvolutionLineSeedEntry.line(220, PokemonRarity.RARE, List.of(396,397,398)), // Starly → Staravia → Staraptor
                EvolutionLineSeedEntry.line(221, PokemonRarity.COMMON, List.of(399,400)), // Bidoof → Bibarel
                EvolutionLineSeedEntry.line(222, PokemonRarity.COMMON, List.of(401,402)), // Kricketot → Kricketune
                EvolutionLineSeedEntry.line(223, PokemonRarity.RARE, List.of(403,404,405)), // Shinx → Luxio → Luxray
                EvolutionLineSeedEntry.line(224, PokemonRarity.COMMON, List.of(315,406,407)), // Roselia → Budew → Roserade
                EvolutionLineSeedEntry.line(225, PokemonRarity.COMMON, List.of(408,409)), // Cranidos → Rampardos
                EvolutionLineSeedEntry.line(226, PokemonRarity.COMMON, List.of(410,411)), // Shieldon → Bastiodon
                EvolutionLineSeedEntry.line(227, PokemonRarity.COMMON, List.of(412,413)), // Burmy → Wormadam
                EvolutionLineSeedEntry.line(228, PokemonRarity.COMMON, List.of(412,414)), // Burmy → Mothim
                EvolutionLineSeedEntry.line(229, PokemonRarity.COMMON, List.of(415,416)), // Combee → Vespiquen
                EvolutionLineSeedEntry.line(230, PokemonRarity.COMMON, List.of(417)), // Pachirisu
                EvolutionLineSeedEntry.line(231, PokemonRarity.COMMON, List.of(418,419)), // Buizel → Floatzel
                EvolutionLineSeedEntry.line(232, PokemonRarity.COMMON, List.of(420,421)), // Cherubi → Cherrim
                EvolutionLineSeedEntry.line(233, PokemonRarity.COMMON, List.of(422,423)), // Shellos → Gastrodon
                EvolutionLineSeedEntry.line(234, PokemonRarity.COMMON, List.of(425,426)), // Drifloon → Drifblim
                EvolutionLineSeedEntry.line(235, PokemonRarity.COMMON, List.of(427,428)), // Buneary → Lopunny
                EvolutionLineSeedEntry.line(236, PokemonRarity.COMMON, List.of(431,432)), // Glameow → Purugly
                EvolutionLineSeedEntry.line(237, PokemonRarity.COMMON, List.of(358,433)), // Chimecho → Chingling
                EvolutionLineSeedEntry.line(238, PokemonRarity.COMMON, List.of(434,435)), // Stunky → Skuntank
                EvolutionLineSeedEntry.line(239, PokemonRarity.COMMON, List.of(436,437)), // Bronzor → Bronzong
                EvolutionLineSeedEntry.line(240, PokemonRarity.RARE, List.of(185,438)), // Sudowoodo → Bonsly
                EvolutionLineSeedEntry.line(241, PokemonRarity.RARE, List.of(439,122,866)), // Mr. Mime → Mime Jr. → Mr. Rime
                EvolutionLineSeedEntry.line(242, PokemonRarity.RARE, List.of(440,113,242)), // Chansey → Blissey → Happiny
                EvolutionLineSeedEntry.line(243, PokemonRarity.RARE, List.of(441)), // Chatot
                EvolutionLineSeedEntry.line(244, PokemonRarity.COMMON, List.of(442)), // Spiritomb
                EvolutionLineSeedEntry.line(245, PokemonRarity.RARE, List.of(443,444,445)), // Gible → Gabite → Garchomp
                EvolutionLineSeedEntry.line(246, PokemonRarity.RARE, List.of(143,446)), // Snorlax → Munchlax
                EvolutionLineSeedEntry.line(247, PokemonRarity.RARE, List.of(447,448)), // Riolu → Lucario
                EvolutionLineSeedEntry.line(248, PokemonRarity.COMMON, List.of(449,450)), // Hippopotas → Hippowdon
                EvolutionLineSeedEntry.line(249, PokemonRarity.RARE, List.of(451,452)), // Skorupi → Drapion
                EvolutionLineSeedEntry.line(250, PokemonRarity.COMMON, List.of(453,454)), // Croagunk → Toxicroak
                EvolutionLineSeedEntry.line(251, PokemonRarity.COMMON, List.of(455)), // Carnivine
                EvolutionLineSeedEntry.line(252, PokemonRarity.COMMON, List.of(456,457)), // Finneon → Lumineon
                EvolutionLineSeedEntry.line(253, PokemonRarity.RARE, List.of(226,458)), // Mantine → Mantyke
                EvolutionLineSeedEntry.line(254, PokemonRarity.COMMON, List.of(459,460)), // Snover → Abomasnow
                EvolutionLineSeedEntry.line(255, PokemonRarity.RARE, List.of(479)), // Rotom
                EvolutionLineSeedEntry.line(256, PokemonRarity.LEGENDARY, List.of(480)), // Uxie
                EvolutionLineSeedEntry.line(257, PokemonRarity.LEGENDARY, List.of(481)), // Mesprit
                EvolutionLineSeedEntry.line(258, PokemonRarity.LEGENDARY, List.of(482)), // Azelf
                EvolutionLineSeedEntry.line(259, PokemonRarity.LEGENDARY, List.of(483)), // Dialga
                EvolutionLineSeedEntry.line(260, PokemonRarity.LEGENDARY, List.of(484)), // Palkia
                EvolutionLineSeedEntry.line(261, PokemonRarity.LEGENDARY, List.of(485)), // Heatran
                EvolutionLineSeedEntry.line(262, PokemonRarity.LEGENDARY, List.of(486)), // Regigigas
                EvolutionLineSeedEntry.line(263, PokemonRarity.LEGENDARY, List.of(487)), // Giratina
                EvolutionLineSeedEntry.line(264, PokemonRarity.LEGENDARY, List.of(488)), // Cresselia
                EvolutionLineSeedEntry.line(265, PokemonRarity.MYTHICAL, List.of(489,490)), // Phione → Manaphy
                EvolutionLineSeedEntry.line(266, PokemonRarity.MYTHICAL, List.of(491)), // Darkrai
                EvolutionLineSeedEntry.line(267, PokemonRarity.MYTHICAL, List.of(492)), // Shaymin
                EvolutionLineSeedEntry.line(268, PokemonRarity.MYTHICAL, List.of(493)), // Arceus
                EvolutionLineSeedEntry.line(269, PokemonRarity.MYTHICAL, List.of(494)), // Victini
                EvolutionLineSeedEntry.line(270, PokemonRarity.RARE, List.of(495,496,497)), // Snivy → Servine → Serperior
                EvolutionLineSeedEntry.line(271, PokemonRarity.RARE, List.of(498,499,500)), // Tepig → Pignite → Emboar
                EvolutionLineSeedEntry.line(272, PokemonRarity.RARE, List.of(501,502,503)), // Oshawott → Dewott → Samurott
                EvolutionLineSeedEntry.line(273, PokemonRarity.COMMON, List.of(504,505)), // Patrat → Watchog
                EvolutionLineSeedEntry.line(274, PokemonRarity.RARE, List.of(506,507,508)), // Lillipup → Herdier → Stoutland
                EvolutionLineSeedEntry.line(275, PokemonRarity.COMMON, List.of(509,510)), // Purrloin → Liepard
                EvolutionLineSeedEntry.line(276, PokemonRarity.COMMON, List.of(511,512)), // Pansage → Simisage
                EvolutionLineSeedEntry.line(277, PokemonRarity.COMMON, List.of(513,514)), // Pansear → Simisear
                EvolutionLineSeedEntry.line(278, PokemonRarity.COMMON, List.of(515,516)), // Panpour → Simipour
                EvolutionLineSeedEntry.line(279, PokemonRarity.COMMON, List.of(517,518)), // Munna → Musharna
                EvolutionLineSeedEntry.line(280, PokemonRarity.RARE, List.of(519,520,521)), // Pidove → Tranquill → Unfezant
                EvolutionLineSeedEntry.line(281, PokemonRarity.COMMON, List.of(522,523)), // Blitzle → Zebstrika
                EvolutionLineSeedEntry.line(282, PokemonRarity.RARE, List.of(524,525,526)), // Roggenrola → Boldore → Gigalith
                EvolutionLineSeedEntry.line(283, PokemonRarity.RARE, List.of(527,528)), // Woobat → Swoobat
                EvolutionLineSeedEntry.line(284, PokemonRarity.COMMON, List.of(529,530)), // Drilbur → Excadrill
                EvolutionLineSeedEntry.line(285, PokemonRarity.COMMON, List.of(531)), // Audino
                EvolutionLineSeedEntry.line(286, PokemonRarity.RARE, List.of(532,533,534)), // Timburr → Gurdurr → Conkeldurr
                EvolutionLineSeedEntry.line(287, PokemonRarity.RARE, List.of(535,536,537)), // Tympole → Palpitoad → Seismitoad
                EvolutionLineSeedEntry.line(288, PokemonRarity.RARE, List.of(538)), // Throh
                EvolutionLineSeedEntry.line(289, PokemonRarity.RARE, List.of(539)), // Sawk
                EvolutionLineSeedEntry.line(290, PokemonRarity.RARE, List.of(540,541,542)), // Sewaddle → Swadloon → Leavanny
                EvolutionLineSeedEntry.line(291, PokemonRarity.RARE, List.of(543,544,545)), // Venipede → Whirlipede → Scolipede
                EvolutionLineSeedEntry.line(292, PokemonRarity.COMMON, List.of(546,547)), // Cottonee → Whimsicott
                EvolutionLineSeedEntry.line(293, PokemonRarity.COMMON, List.of(548,549)), // Petilil → Lilligant
                EvolutionLineSeedEntry.line(294, PokemonRarity.RARE, List.of(550,902)), // Basculin → Basculegion
                EvolutionLineSeedEntry.line(295, PokemonRarity.RARE, List.of(551,552,553)), // Sandile → Krokorok → Krookodile
                EvolutionLineSeedEntry.line(296, PokemonRarity.COMMON, List.of(554,555)), // Darumaka → Darmanitan
                EvolutionLineSeedEntry.line(297, PokemonRarity.COMMON, List.of(556)), // Maractus
                EvolutionLineSeedEntry.line(298, PokemonRarity.COMMON, List.of(557,558)), // Dwebble → Crustle
                EvolutionLineSeedEntry.line(299, PokemonRarity.COMMON, List.of(559,560)), // Scraggy → Scrafty
                EvolutionLineSeedEntry.line(300, PokemonRarity.RARE, List.of(561)), // Sigilyph
                EvolutionLineSeedEntry.line(301, PokemonRarity.COMMON, List.of(562,563)), // Yamask → Cofagrigus
                EvolutionLineSeedEntry.line(302, PokemonRarity.COMMON, List.of(562,867)), // Yamask → Runerigus
                EvolutionLineSeedEntry.line(303, PokemonRarity.RARE, List.of(564,565)), // Tirtouga → Carracosta
                EvolutionLineSeedEntry.line(304, PokemonRarity.RARE, List.of(566,567)), // Archen → Archeops
                EvolutionLineSeedEntry.line(305, PokemonRarity.COMMON, List.of(568,569)), // Trubbish → Garbodor
                EvolutionLineSeedEntry.line(306, PokemonRarity.RARE, List.of(570,571)), // Zorua → Zoroark
                EvolutionLineSeedEntry.line(307, PokemonRarity.COMMON, List.of(572,573)), // Minccino → Cinccino
                EvolutionLineSeedEntry.line(308, PokemonRarity.COMMON, List.of(574,575,576)), // Gothita → Gothorita → Gothitelle
                EvolutionLineSeedEntry.line(309, PokemonRarity.COMMON, List.of(577,578,579)), // Solosis → Duosion → Reuniclus
                EvolutionLineSeedEntry.line(310, PokemonRarity.RARE, List.of(580,581)), // Ducklett → Swanna
                EvolutionLineSeedEntry.line(311, PokemonRarity.RARE, List.of(582,583,584)), // Vanillite → Vanillish → Vanilluxe
                EvolutionLineSeedEntry.line(312, PokemonRarity.COMMON, List.of(585,586)), // Deerling → Sawsbuck
                EvolutionLineSeedEntry.line(313, PokemonRarity.COMMON, List.of(587)), // Emolga
                EvolutionLineSeedEntry.line(314, PokemonRarity.COMMON, List.of(588,589)), // Karrablast → Escavalier
                EvolutionLineSeedEntry.line(315, PokemonRarity.COMMON, List.of(590,591)), // Foongus → Amoonguss
                EvolutionLineSeedEntry.line(316, PokemonRarity.COMMON, List.of(592,593)), // Frillish → Jellicent
                EvolutionLineSeedEntry.line(317, PokemonRarity.COMMON, List.of(594)), // Alomomola
                EvolutionLineSeedEntry.line(318, PokemonRarity.COMMON, List.of(595,596)), // Joltik → Galvantula
                EvolutionLineSeedEntry.line(319, PokemonRarity.COMMON, List.of(597,598)), // Ferroseed → Ferrothorn
                EvolutionLineSeedEntry.line(320, PokemonRarity.RARE, List.of(599,600,601)), // Klink → Klang → Klinklang
                EvolutionLineSeedEntry.line(321, PokemonRarity.RARE, List.of(602,603,604)), // Tynamo → Eelektrik → Eelektross
                EvolutionLineSeedEntry.line(322, PokemonRarity.COMMON, List.of(605,606)), // Elgyem → Beheeyem
                EvolutionLineSeedEntry.line(323, PokemonRarity.RARE, List.of(607,608,609)), // Litwick → Lampent → Chandelure
                EvolutionLineSeedEntry.line(324, PokemonRarity.RARE, List.of(610,611,612)), // Axew → Fraxure → Haxorus
                EvolutionLineSeedEntry.line(325, PokemonRarity.COMMON, List.of(613,614)), // Cubchoo → Beartic
                EvolutionLineSeedEntry.line(326, PokemonRarity.RARE, List.of(615)), // Cryogonal
                EvolutionLineSeedEntry.line(327, PokemonRarity.COMMON, List.of(616,617)), // Shelmet → Accelgor
                EvolutionLineSeedEntry.line(328, PokemonRarity.COMMON, List.of(618)), // Stunfisk
                EvolutionLineSeedEntry.line(329, PokemonRarity.RARE, List.of(619,620)), // Mienfoo → Mienshao
                EvolutionLineSeedEntry.line(330, PokemonRarity.RARE, List.of(621)), // Druddigon
                EvolutionLineSeedEntry.line(331, PokemonRarity.COMMON, List.of(622,623)), // Golett → Golurk
                EvolutionLineSeedEntry.line(332, PokemonRarity.RARE, List.of(624,625,983)), // Pawniard → Bisharp → Kingambit
                EvolutionLineSeedEntry.line(333, PokemonRarity.RARE, List.of(626)), // Bouffalant
                EvolutionLineSeedEntry.line(334, PokemonRarity.COMMON, List.of(627,628)), // Rufflet → Braviary
                EvolutionLineSeedEntry.line(335, PokemonRarity.COMMON, List.of(629,630)), // Vullaby → Mandibuzz
                EvolutionLineSeedEntry.line(336, PokemonRarity.COMMON, List.of(631)), // Heatmor
                EvolutionLineSeedEntry.line(337, PokemonRarity.COMMON, List.of(632)), // Durant
                EvolutionLineSeedEntry.line(338, PokemonRarity.RARE, List.of(633,634,635)), // Deino → Zweilous → Hydreigon
                EvolutionLineSeedEntry.line(339, PokemonRarity.RARE, List.of(636,637)), // Larvesta → Volcarona
                EvolutionLineSeedEntry.line(340, PokemonRarity.LEGENDARY, List.of(638)), // Cobalion
                EvolutionLineSeedEntry.line(341, PokemonRarity.LEGENDARY, List.of(639)), // Terrakion
                EvolutionLineSeedEntry.line(342, PokemonRarity.LEGENDARY, List.of(640)), // Virizion
                EvolutionLineSeedEntry.line(343, PokemonRarity.LEGENDARY, List.of(641)), // Tornadus
                EvolutionLineSeedEntry.line(344, PokemonRarity.LEGENDARY, List.of(642)), // Thundurus
                EvolutionLineSeedEntry.line(345, PokemonRarity.LEGENDARY, List.of(643)), // Reshiram
                EvolutionLineSeedEntry.line(346, PokemonRarity.LEGENDARY, List.of(644)), // Zekrom
                EvolutionLineSeedEntry.line(347, PokemonRarity.LEGENDARY, List.of(645)), // Landorus
                EvolutionLineSeedEntry.line(348, PokemonRarity.LEGENDARY, List.of(646)), // Kyurem
                EvolutionLineSeedEntry.line(349, PokemonRarity.MYTHICAL, List.of(647)), // Keldeo
                EvolutionLineSeedEntry.line(350, PokemonRarity.MYTHICAL, List.of(648)), // Meloetta
                EvolutionLineSeedEntry.line(351, PokemonRarity.MYTHICAL, List.of(649)), // Genesect
                EvolutionLineSeedEntry.line(352, PokemonRarity.RARE, List.of(650,651,652)), // Chespin → Quilladin → Chesnaught
                EvolutionLineSeedEntry.line(353, PokemonRarity.RARE, List.of(653,654,655)), // Fennekin → Braixen → Delphox
                EvolutionLineSeedEntry.line(354, PokemonRarity.RARE, List.of(656,657,658)), // Froakie → Frogadier → Greninja
                EvolutionLineSeedEntry.line(355, PokemonRarity.COMMON, List.of(659,660)), // Bunnelby → Diggersby
                EvolutionLineSeedEntry.line(356, PokemonRarity.RARE, List.of(661,662,663)), // Fletchling → Fletchinder → Talonflame
                EvolutionLineSeedEntry.line(357, PokemonRarity.RARE, List.of(664,665,666)), // Scatterbug → Spewpa → Vivillon
                EvolutionLineSeedEntry.line(358, PokemonRarity.COMMON, List.of(667,668)), // Litleo → Pyroar
                EvolutionLineSeedEntry.line(359, PokemonRarity.RARE, List.of(669,670,671)), // Flabébé → Floette → Florges
                EvolutionLineSeedEntry.line(360, PokemonRarity.RARE, List.of(672,673)), // Skiddo → Gogoat
                EvolutionLineSeedEntry.line(361, PokemonRarity.COMMON, List.of(674,675)), // Pancham → Pangoro
                EvolutionLineSeedEntry.line(362, PokemonRarity.COMMON, List.of(676)), // Furfrou
                EvolutionLineSeedEntry.line(363, PokemonRarity.COMMON, List.of(677,678)), // Espurr → Meowstic
                EvolutionLineSeedEntry.line(364, PokemonRarity.RARE, List.of(679,680,681)), // Honedge → Doublade → Aegislash
                EvolutionLineSeedEntry.line(365, PokemonRarity.COMMON, List.of(682,683)), // Spritzee → Aromatisse
                EvolutionLineSeedEntry.line(366, PokemonRarity.COMMON, List.of(684,685)), // Swirlix → Slurpuff
                EvolutionLineSeedEntry.line(367, PokemonRarity.COMMON, List.of(686,687)), // Inkay → Malamar
                EvolutionLineSeedEntry.line(368, PokemonRarity.RARE, List.of(688,689)), // Binacle → Barbaracle
                EvolutionLineSeedEntry.line(369, PokemonRarity.COMMON, List.of(690,691)), // Skrelp → Dragalge
                EvolutionLineSeedEntry.line(370, PokemonRarity.COMMON, List.of(692,693)), // Clauncher → Clawitzer
                EvolutionLineSeedEntry.line(371, PokemonRarity.COMMON, List.of(694,695)), // Helioptile → Heliolisk
                EvolutionLineSeedEntry.line(372, PokemonRarity.RARE, List.of(696,697)), // Tyrunt → Tyrantrum
                EvolutionLineSeedEntry.line(373, PokemonRarity.RARE, List.of(698,699)), // Amaura → Aurorus
                EvolutionLineSeedEntry.line(374, PokemonRarity.COMMON, List.of(701)), // Hawlucha
                EvolutionLineSeedEntry.line(375, PokemonRarity.COMMON, List.of(702)), // Dedenne
                EvolutionLineSeedEntry.line(376, PokemonRarity.COMMON, List.of(703)), // Carbink
                EvolutionLineSeedEntry.line(377, PokemonRarity.RARE, List.of(704,705,706)), // Goomy → Sliggoo → Goodra
                EvolutionLineSeedEntry.line(378, PokemonRarity.COMMON, List.of(707)), // Klefki
                EvolutionLineSeedEntry.line(379, PokemonRarity.COMMON, List.of(708,709)), // Phantump → Trevenant
                EvolutionLineSeedEntry.line(380, PokemonRarity.COMMON, List.of(710,711)), // Pumpkaboo → Gourgeist
                EvolutionLineSeedEntry.line(381, PokemonRarity.COMMON, List.of(712,713)), // Bergmite → Avalugg
                EvolutionLineSeedEntry.line(382, PokemonRarity.RARE, List.of(714,715)), // Noibat → Noivern
                EvolutionLineSeedEntry.line(383, PokemonRarity.LEGENDARY, List.of(716)), // Xerneas
                EvolutionLineSeedEntry.line(384, PokemonRarity.LEGENDARY, List.of(717)), // Yveltal
                EvolutionLineSeedEntry.line(385, PokemonRarity.LEGENDARY, List.of(718)), // Zygarde
                EvolutionLineSeedEntry.line(386, PokemonRarity.MYTHICAL, List.of(719)), // Diancie
                EvolutionLineSeedEntry.line(387, PokemonRarity.MYTHICAL, List.of(720)), // Hoopa
                EvolutionLineSeedEntry.line(388, PokemonRarity.MYTHICAL, List.of(721)), // Volcanion
                EvolutionLineSeedEntry.line(389, PokemonRarity.RARE, List.of(722,723,724)), // Rowlet → Dartrix → Decidueye
                EvolutionLineSeedEntry.line(390, PokemonRarity.RARE, List.of(725,726,727)), // Litten → Torracat → Incineroar
                EvolutionLineSeedEntry.line(391, PokemonRarity.RARE, List.of(728,729,730)), // Popplio → Brionne → Primarina
                EvolutionLineSeedEntry.line(392, PokemonRarity.RARE, List.of(731,732,733)), // Pikipek → Trumbeak → Toucannon
                EvolutionLineSeedEntry.line(393, PokemonRarity.COMMON, List.of(734,735)), // Yungoos → Gumshoos
                EvolutionLineSeedEntry.line(394, PokemonRarity.RARE, List.of(736,737,738)), // Grubbin → Charjabug → Vikavolt
                EvolutionLineSeedEntry.line(395, PokemonRarity.COMMON, List.of(739,740)), // Crabrawler → Crabominable
                EvolutionLineSeedEntry.line(396, PokemonRarity.RARE, List.of(741)), // Oricorio
                EvolutionLineSeedEntry.line(397, PokemonRarity.COMMON, List.of(742,743)), // Cutiefly → Ribombee
                EvolutionLineSeedEntry.line(398, PokemonRarity.COMMON, List.of(744,745)), // Rockruff → Lycanroc
                EvolutionLineSeedEntry.line(399, PokemonRarity.COMMON, List.of(746)), // Wishiwashi
                EvolutionLineSeedEntry.line(400, PokemonRarity.COMMON, List.of(747,748)), // Mareanie → Toxapex
                EvolutionLineSeedEntry.line(401, PokemonRarity.COMMON, List.of(749,750)), // Mudbray → Mudsdale
                EvolutionLineSeedEntry.line(402, PokemonRarity.COMMON, List.of(751,752)), // Dewpider → Araquanid
                EvolutionLineSeedEntry.line(403, PokemonRarity.COMMON, List.of(753,754)), // Fomantis → Lurantis
                EvolutionLineSeedEntry.line(404, PokemonRarity.COMMON, List.of(755,756)), // Morelull → Shiinotic
                EvolutionLineSeedEntry.line(405, PokemonRarity.RARE, List.of(757,758)), // Salandit → Salazzle
                EvolutionLineSeedEntry.line(406, PokemonRarity.COMMON, List.of(759,760)), // Stufful → Bewear
                EvolutionLineSeedEntry.line(407, PokemonRarity.RARE, List.of(761,762,763)), // Bounsweet → Steenee → Tsareena
                EvolutionLineSeedEntry.line(408, PokemonRarity.COMMON, List.of(764)), // Comfey
                EvolutionLineSeedEntry.line(409, PokemonRarity.RARE, List.of(765)), // Oranguru
                EvolutionLineSeedEntry.line(410, PokemonRarity.RARE, List.of(766)), // Passimian
                EvolutionLineSeedEntry.line(411, PokemonRarity.RARE, List.of(767,768)), // Wimpod → Golisopod
                EvolutionLineSeedEntry.line(412, PokemonRarity.COMMON, List.of(769,770)), // Sandygast → Palossand
                EvolutionLineSeedEntry.line(413, PokemonRarity.COMMON, List.of(771)), // Pyukumuku
                EvolutionLineSeedEntry.line(414, PokemonRarity.LEGENDARY, List.of(772,773)), // Type: Null → Silvally
                EvolutionLineSeedEntry.line(415, PokemonRarity.RARE, List.of(774)), // Minior
                EvolutionLineSeedEntry.line(416, PokemonRarity.RARE, List.of(775)), // Komala
                EvolutionLineSeedEntry.line(417, PokemonRarity.COMMON, List.of(776)), // Turtonator
                EvolutionLineSeedEntry.line(418, PokemonRarity.COMMON, List.of(777)), // Togedemaru
                EvolutionLineSeedEntry.line(419, PokemonRarity.RARE, List.of(778)), // Mimikyu
                EvolutionLineSeedEntry.line(420, PokemonRarity.COMMON, List.of(779)), // Bruxish
                EvolutionLineSeedEntry.line(421, PokemonRarity.COMMON, List.of(780)), // Drampa
                EvolutionLineSeedEntry.line(422, PokemonRarity.RARE, List.of(781)), // Dhelmise
                EvolutionLineSeedEntry.line(423, PokemonRarity.RARE, List.of(782,783,784)), // Jangmo-o → Hakamo-o → Kommo-o
                EvolutionLineSeedEntry.line(424, PokemonRarity.LEGENDARY, List.of(785)), // Tapu Koko
                EvolutionLineSeedEntry.line(425, PokemonRarity.LEGENDARY, List.of(786)), // Tapu Lele
                EvolutionLineSeedEntry.line(426, PokemonRarity.LEGENDARY, List.of(787)), // Tapu Bulu
                EvolutionLineSeedEntry.line(427, PokemonRarity.LEGENDARY, List.of(788)), // Tapu Fini
                EvolutionLineSeedEntry.line(428, PokemonRarity.LEGENDARY, List.of(789,790,791)), // Cosmog → Cosmoem → Solgaleo
                EvolutionLineSeedEntry.line(429, PokemonRarity.LEGENDARY, List.of(789,790,792)), // Cosmog → Cosmoem → Lunala
                EvolutionLineSeedEntry.line(430, PokemonRarity.RARE, List.of(793)), // Nihilego
                EvolutionLineSeedEntry.line(431, PokemonRarity.RARE, List.of(794)), // Buzzwole
                EvolutionLineSeedEntry.line(432, PokemonRarity.RARE, List.of(795)), // Pheromosa
                EvolutionLineSeedEntry.line(433, PokemonRarity.RARE, List.of(796)), // Xurkitree
                EvolutionLineSeedEntry.line(434, PokemonRarity.RARE, List.of(797)), // Celesteela
                EvolutionLineSeedEntry.line(435, PokemonRarity.RARE, List.of(798)), // Kartana
                EvolutionLineSeedEntry.line(436, PokemonRarity.RARE, List.of(799)), // Guzzlord
                EvolutionLineSeedEntry.line(437, PokemonRarity.LEGENDARY, List.of(800)), // Necrozma
                EvolutionLineSeedEntry.line(438, PokemonRarity.MYTHICAL, List.of(801)), // Magearna
                EvolutionLineSeedEntry.line(439, PokemonRarity.MYTHICAL, List.of(802)), // Marshadow
                EvolutionLineSeedEntry.line(440, PokemonRarity.RARE, List.of(803,804)), // Poipole → Naganadel
                EvolutionLineSeedEntry.line(441, PokemonRarity.RARE, List.of(805)), // Stakataka
                EvolutionLineSeedEntry.line(442, PokemonRarity.RARE, List.of(806)), // Blacephalon
                EvolutionLineSeedEntry.line(443, PokemonRarity.MYTHICAL, List.of(807)), // Zeraora
                EvolutionLineSeedEntry.line(444, PokemonRarity.MYTHICAL, List.of(808)), // Meltan
                EvolutionLineSeedEntry.line(445, PokemonRarity.MYTHICAL, List.of(809)), // Melmetal
                EvolutionLineSeedEntry.line(446, PokemonRarity.RARE, List.of(810,811,812)), // Grookey → Thwackey → Rillaboom
                EvolutionLineSeedEntry.line(447, PokemonRarity.RARE, List.of(813,814,815)), // Scorbunny → Raboot → Cinderace
                EvolutionLineSeedEntry.line(448, PokemonRarity.RARE, List.of(816,817,818)), // Sobble → Drizzile → Inteleon
                EvolutionLineSeedEntry.line(449, PokemonRarity.COMMON, List.of(819,820)), // Skwovet → Greedent
                EvolutionLineSeedEntry.line(450, PokemonRarity.RARE, List.of(821,822,823)), // Rookidee → Corvisquire → Corviknight
                EvolutionLineSeedEntry.line(451, PokemonRarity.RARE, List.of(824,825,826)), // Blipbug → Dottler → Orbeetle
                EvolutionLineSeedEntry.line(452, PokemonRarity.COMMON, List.of(827,828)), // Nickit → Thievul
                EvolutionLineSeedEntry.line(453, PokemonRarity.COMMON, List.of(829,830)), // Gossifleur → Eldegoss
                EvolutionLineSeedEntry.line(454, PokemonRarity.COMMON, List.of(831,832)), // Wooloo → Dubwool
                EvolutionLineSeedEntry.line(455, PokemonRarity.COMMON, List.of(833,834)), // Chewtle → Drednaw
                EvolutionLineSeedEntry.line(456, PokemonRarity.RARE, List.of(835,836)), // Yamper → Boltund
                EvolutionLineSeedEntry.line(457, PokemonRarity.RARE, List.of(837,838,839)), // Rolycoly → Carkol → Coalossal
                EvolutionLineSeedEntry.line(458, PokemonRarity.RARE, List.of(840,841)), // Applin → Flapple
                EvolutionLineSeedEntry.line(459, PokemonRarity.RARE, List.of(840,842)), // Applin → Appletun
                EvolutionLineSeedEntry.line(460, PokemonRarity.RARE, List.of(840,1011,1019)), // Applin → Dipplin → Hydrapple
                EvolutionLineSeedEntry.line(461, PokemonRarity.COMMON, List.of(843,844)), // Silicobra → Sandaconda
                EvolutionLineSeedEntry.line(462, PokemonRarity.RARE, List.of(845)), // Cramorant
                EvolutionLineSeedEntry.line(463, PokemonRarity.COMMON, List.of(846,847)), // Arrokuda → Barraskewda
                EvolutionLineSeedEntry.line(464, PokemonRarity.RARE, List.of(848,849)), // Toxel → Toxtricity
                EvolutionLineSeedEntry.line(465, PokemonRarity.COMMON, List.of(850,851)), // Sizzlipede → Centiskorch
                EvolutionLineSeedEntry.line(466, PokemonRarity.RARE, List.of(852,853)), // Clobbopus → Grapploct
                EvolutionLineSeedEntry.line(467, PokemonRarity.COMMON, List.of(854,855)), // Sinistea → Polteageist
                EvolutionLineSeedEntry.line(468, PokemonRarity.RARE, List.of(856,857,858)), // Hatenna → Hattrem → Hatterene
                EvolutionLineSeedEntry.line(469, PokemonRarity.RARE, List.of(859,860,861)), // Impidimp → Morgrem → Grimmsnarl
                EvolutionLineSeedEntry.line(470, PokemonRarity.COMMON, List.of(868,869)), // Milcery → Alcremie
                EvolutionLineSeedEntry.line(471, PokemonRarity.RARE, List.of(870)), // Falinks
                EvolutionLineSeedEntry.line(472, PokemonRarity.COMMON, List.of(871)), // Pincurchin
                EvolutionLineSeedEntry.line(473, PokemonRarity.COMMON, List.of(872,873)), // Snom → Frosmoth
                EvolutionLineSeedEntry.line(474, PokemonRarity.COMMON, List.of(874)), // Stonjourner
                EvolutionLineSeedEntry.line(475, PokemonRarity.COMMON, List.of(875)), // Eiscue
                EvolutionLineSeedEntry.line(476, PokemonRarity.RARE, List.of(876)), // Indeedee
                EvolutionLineSeedEntry.line(477, PokemonRarity.COMMON, List.of(877)), // Morpeko
                EvolutionLineSeedEntry.line(478, PokemonRarity.COMMON, List.of(878,879)), // Cufant → Copperajah
                EvolutionLineSeedEntry.line(479, PokemonRarity.RARE, List.of(880)), // Dracozolt
                EvolutionLineSeedEntry.line(480, PokemonRarity.RARE, List.of(881)), // Arctozolt
                EvolutionLineSeedEntry.line(481, PokemonRarity.RARE, List.of(882)), // Dracovish
                EvolutionLineSeedEntry.line(482, PokemonRarity.RARE, List.of(883)), // Arctovish
                EvolutionLineSeedEntry.line(483, PokemonRarity.RARE, List.of(884,1018)), // Duraludon → Archaludon
                EvolutionLineSeedEntry.line(484, PokemonRarity.RARE, List.of(885,886,887)), // Dreepy → Drakloak → Dragapult
                EvolutionLineSeedEntry.line(485, PokemonRarity.LEGENDARY, List.of(888)), // Zacian
                EvolutionLineSeedEntry.line(486, PokemonRarity.LEGENDARY, List.of(889)), // Zamazenta
                EvolutionLineSeedEntry.line(487, PokemonRarity.LEGENDARY, List.of(890)), // Eternatus
                EvolutionLineSeedEntry.line(488, PokemonRarity.LEGENDARY, List.of(891,892)), // Kubfu → Urshifu
                EvolutionLineSeedEntry.line(489, PokemonRarity.MYTHICAL, List.of(893)), // Zarude
                EvolutionLineSeedEntry.line(490, PokemonRarity.LEGENDARY, List.of(894)), // Regieleki
                EvolutionLineSeedEntry.line(491, PokemonRarity.LEGENDARY, List.of(895)), // Regidrago
                EvolutionLineSeedEntry.line(492, PokemonRarity.LEGENDARY, List.of(896)), // Glastrier
                EvolutionLineSeedEntry.line(493, PokemonRarity.LEGENDARY, List.of(897)), // Spectrier
                EvolutionLineSeedEntry.line(494, PokemonRarity.LEGENDARY, List.of(898)), // Calyrex
                EvolutionLineSeedEntry.line(495, PokemonRarity.LEGENDARY, List.of(905)), // Enamorus
                EvolutionLineSeedEntry.line(496, PokemonRarity.RARE, List.of(906,907,908)), // Sprigatito → Floragato → Meowscarada
                EvolutionLineSeedEntry.line(497, PokemonRarity.RARE, List.of(909,910,911)), // Fuecoco → Crocalor → Skeledirge
                EvolutionLineSeedEntry.line(498, PokemonRarity.RARE, List.of(912,913,914)), // Quaxly → Quaxwell → Quaquaval
                EvolutionLineSeedEntry.line(499, PokemonRarity.COMMON, List.of(915,916)), // Lechonk → Oinkologne
                EvolutionLineSeedEntry.line(500, PokemonRarity.COMMON, List.of(917,918)), // Tarountula → Spidops
                EvolutionLineSeedEntry.line(501, PokemonRarity.RARE, List.of(919,920)), // Nymble → Lokix
                EvolutionLineSeedEntry.line(502, PokemonRarity.RARE, List.of(921,922,923)), // Pawmi → Pawmo → Pawmot
                EvolutionLineSeedEntry.line(503, PokemonRarity.COMMON, List.of(924,925)), // Tandemaus → Maushold
                EvolutionLineSeedEntry.line(504, PokemonRarity.COMMON, List.of(926,927)), // Fidough → Dachsbun
                EvolutionLineSeedEntry.line(505, PokemonRarity.RARE, List.of(928,929,930)), // Smoliv → Dolliv → Arboliva
                EvolutionLineSeedEntry.line(506, PokemonRarity.COMMON, List.of(931)), // Squawkabilly
                EvolutionLineSeedEntry.line(507, PokemonRarity.RARE, List.of(932,933,934)), // Nacli → Naclstack → Garganacl
                EvolutionLineSeedEntry.line(508, PokemonRarity.RARE, List.of(935,936)), // Charcadet → Armarouge
                EvolutionLineSeedEntry.line(509, PokemonRarity.RARE, List.of(935,937)), // Charcadet → Ceruledge
                EvolutionLineSeedEntry.line(510, PokemonRarity.COMMON, List.of(938,939)), // Tadbulb → Bellibolt
                EvolutionLineSeedEntry.line(511, PokemonRarity.COMMON, List.of(940,941)), // Wattrel → Kilowattrel
                EvolutionLineSeedEntry.line(512, PokemonRarity.COMMON, List.of(942,943)), // Maschiff → Mabosstiff
                EvolutionLineSeedEntry.line(513, PokemonRarity.COMMON, List.of(944,945)), // Shroodle → Grafaiai
                EvolutionLineSeedEntry.line(514, PokemonRarity.RARE, List.of(946,947)), // Bramblin → Brambleghast
                EvolutionLineSeedEntry.line(515, PokemonRarity.COMMON, List.of(948,949)), // Toedscool → Toedscruel
                EvolutionLineSeedEntry.line(516, PokemonRarity.COMMON, List.of(950)), // Klawf
                EvolutionLineSeedEntry.line(517, PokemonRarity.COMMON, List.of(951,952)), // Capsakid → Scovillain
                EvolutionLineSeedEntry.line(518, PokemonRarity.RARE, List.of(953,954)), // Rellor → Rabsca
                EvolutionLineSeedEntry.line(519, PokemonRarity.COMMON, List.of(955,956)), // Flittle → Espathra
                EvolutionLineSeedEntry.line(520, PokemonRarity.RARE, List.of(957,958,959)), // Tinkatink → Tinkatuff → Tinkaton
                EvolutionLineSeedEntry.line(521, PokemonRarity.COMMON, List.of(960,961)), // Wiglett → Wugtrio
                EvolutionLineSeedEntry.line(522, PokemonRarity.RARE, List.of(962)), // Bombirdier
                EvolutionLineSeedEntry.line(523, PokemonRarity.RARE, List.of(963,964)), // Finizen → Palafin
                EvolutionLineSeedEntry.line(524, PokemonRarity.COMMON, List.of(965,966)), // Varoom → Revavroom
                EvolutionLineSeedEntry.line(525, PokemonRarity.COMMON, List.of(967)), // Cyclizar
                EvolutionLineSeedEntry.line(526, PokemonRarity.RARE, List.of(968)), // Orthworm
                EvolutionLineSeedEntry.line(527, PokemonRarity.RARE, List.of(969,970)), // Glimmet → Glimmora
                EvolutionLineSeedEntry.line(528, PokemonRarity.COMMON, List.of(971,972)), // Greavard → Houndstone
                EvolutionLineSeedEntry.line(529, PokemonRarity.COMMON, List.of(973)), // Flamigo
                EvolutionLineSeedEntry.line(530, PokemonRarity.COMMON, List.of(974,975)), // Cetoddle → Cetitan
                EvolutionLineSeedEntry.line(531, PokemonRarity.COMMON, List.of(976)), // Veluza
                EvolutionLineSeedEntry.line(532, PokemonRarity.RARE, List.of(977)), // Dondozo
                EvolutionLineSeedEntry.line(533, PokemonRarity.COMMON, List.of(978)), // Tatsugiri
                EvolutionLineSeedEntry.line(534, PokemonRarity.RARE, List.of(984)), // Great Tusk
                EvolutionLineSeedEntry.line(535, PokemonRarity.COMMON, List.of(985)), // Scream Tail
                EvolutionLineSeedEntry.line(536, PokemonRarity.COMMON, List.of(986)), // Brute Bonnet
                EvolutionLineSeedEntry.line(537, PokemonRarity.RARE, List.of(987)), // Flutter Mane
                EvolutionLineSeedEntry.line(538, PokemonRarity.RARE, List.of(988)), // Slither Wing
                EvolutionLineSeedEntry.line(539, PokemonRarity.RARE, List.of(989)), // Sandy Shocks
                EvolutionLineSeedEntry.line(540, PokemonRarity.RARE, List.of(990)), // Iron Treads
                EvolutionLineSeedEntry.line(541, PokemonRarity.COMMON, List.of(991)), // Iron Bundle
                EvolutionLineSeedEntry.line(542, PokemonRarity.COMMON, List.of(992)), // Iron Hands
                EvolutionLineSeedEntry.line(543, PokemonRarity.RARE, List.of(993)), // Iron Jugulis
                EvolutionLineSeedEntry.line(544, PokemonRarity.RARE, List.of(994)), // Iron Moth
                EvolutionLineSeedEntry.line(545, PokemonRarity.RARE, List.of(995)), // Iron Thorns
                EvolutionLineSeedEntry.line(546, PokemonRarity.RARE, List.of(996,997,998)), // Frigibax → Arctibax → Baxcalibur
                EvolutionLineSeedEntry.line(547, PokemonRarity.RARE, List.of(999,1000)), // Gimmighoul → Gholdengo
                EvolutionLineSeedEntry.line(548, PokemonRarity.LEGENDARY, List.of(1001)), // Wo-Chien
                EvolutionLineSeedEntry.line(549, PokemonRarity.LEGENDARY, List.of(1002)), // Chien-Pao
                EvolutionLineSeedEntry.line(550, PokemonRarity.LEGENDARY, List.of(1003)), // Ting-Lu
                EvolutionLineSeedEntry.line(551, PokemonRarity.LEGENDARY, List.of(1004)), // Chi-Yu
                EvolutionLineSeedEntry.line(552, PokemonRarity.RARE, List.of(1005)), // Roaring Moon
                EvolutionLineSeedEntry.line(553, PokemonRarity.RARE, List.of(1006)), // Iron Valiant
                EvolutionLineSeedEntry.line(554, PokemonRarity.LEGENDARY, List.of(1007)), // Koraidon
                EvolutionLineSeedEntry.line(555, PokemonRarity.LEGENDARY, List.of(1008)), // Miraidon
                EvolutionLineSeedEntry.line(556, PokemonRarity.RARE, List.of(1009)), // Walking Wake
                EvolutionLineSeedEntry.line(557, PokemonRarity.RARE, List.of(1010)), // Iron Leaves
                EvolutionLineSeedEntry.line(558, PokemonRarity.COMMON, List.of(1012,1013)), // Poltchageist → Sinistcha
                EvolutionLineSeedEntry.line(559, PokemonRarity.LEGENDARY, List.of(1014)), // Okidogi
                EvolutionLineSeedEntry.line(560, PokemonRarity.LEGENDARY, List.of(1015)), // Munkidori
                EvolutionLineSeedEntry.line(561, PokemonRarity.LEGENDARY, List.of(1016)), // Fezandipiti
                EvolutionLineSeedEntry.line(562, PokemonRarity.LEGENDARY, List.of(1017)), // Ogerpon
                EvolutionLineSeedEntry.line(563, PokemonRarity.RARE, List.of(1020)), // Gouging Fire
                EvolutionLineSeedEntry.line(564, PokemonRarity.RARE, List.of(1021)), // Raging Bolt
                EvolutionLineSeedEntry.line(565, PokemonRarity.RARE, List.of(1022)), // Iron Boulder
                EvolutionLineSeedEntry.line(566, PokemonRarity.RARE, List.of(1023)), // Iron Crown
                EvolutionLineSeedEntry.line(567, PokemonRarity.LEGENDARY, List.of(1024)), // Terapagos
                EvolutionLineSeedEntry.line(568, PokemonRarity.MYTHICAL, List.of(1025)) // Pecharunt
        );
    }
}
