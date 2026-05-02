#!/usr/bin/env python3
"""Fetch national dex 152–1025 from PokeAPI and write Generation2Seed…Generation9Seed.java."""
from __future__ import annotations

import json
import ssl
import sys
from http.client import HTTPSConnection
from pathlib import Path
from urllib.parse import urlparse

HOST = "pokeapi.co"
BASE_PREFIX = "/api/v2"
UA = "pokeguessteam-seed-generator/1.0 (https://github.com/PokeAPI/pokeapi)"

COLOR_MAP = {
    "red": "RED",
    "blue": "BLUE",
    "yellow": "YELLOW",
    "green": "GREEN",
    "black": "BLACK",
    "brown": "BROWN",
    "purple": "PURPLE",
    "gray": "GRAY",
    "white": "WHITE",
    "pink": "PINK",
}

NAME_FIX = {
    "mr-mime": "Mr. Mime",
    "mime-jr": "Mime Jr.",
    "type-null": "Type: Null",
    "porygon-z": "Porygon-Z",
    "ho-oh": "Ho-Oh",
    "jangmo-o": "Jangmo-o",
    "hakamo-o": "Hakamo-o",
    "kommo-o": "Kommo-o",
    "sirfetchd": "Sirfetch'd",
    "farfetchd": "Farfetch'd",
    "nidoran-f": "Nidoran♀",
    "nidoran-m": "Nidoran♂",
    "flabebe": "Flabébé",
    "floette": "Floette",
    "florges": "Florges",
}


def main() -> int:
    ctx = ssl.create_default_context()
    conn = HTTPSConnection(HOST, context=ctx, timeout=120)

    def fetch_json(path: str) -> dict:
        conn.request("GET", path, headers={"User-Agent": UA})
        res = conn.getresponse()
        body = res.read()
        if res.status != 200:
            raise RuntimeError(f"{path} -> HTTP {res.status}")
        return json.loads(body)

    def species_id_from_url(url: str) -> int:
        return int(url.rstrip("/").split("/")[-1])

    def english_name(species: dict) -> str:
        for n in species.get("names", []):
            if n.get("language", {}).get("name") == "en":
                return n["name"]
        return species["name"].replace("-", " ").title()

    def find_chain_node(node: dict, target_id: int) -> dict | None:
        if species_id_from_url(node["species"]["url"]) == target_id:
            return node
        for sub in node.get("evolves_to", []):
            found = find_chain_node(sub, target_id)
            if found:
                return found
        return None

    def direct_evolves_to(chain_root: dict, target_id: int) -> list[int]:
        node = find_chain_node(chain_root, target_id)
        if not node:
            return []
        out = [species_id_from_url(ev["species"]["url"]) for ev in node.get("evolves_to", [])]
        return sorted(out)

    def evolution_stage(species: dict, chain_root: dict, target_id: int) -> tuple[str | None, int | None]:
        node = find_chain_node(chain_root, target_id)
        if not node:
            return None, None
        has_parent = species.get("evolves_from_species") is not None
        has_child = len(node.get("evolves_to", [])) > 0
        if not has_parent and not has_child:
            return None, None
        if not has_parent and has_child:
            lvl = _first_min_level(node)
            return "EvolutionStage.BASE", lvl
        if has_parent and has_child:
            return "EvolutionStage.FIRST_STAGE", _first_min_level(node)
        if has_parent and not has_child:
            return "EvolutionStage.SECOND_STAGE", None
        return None, None

    def _first_min_level(node: dict) -> int | None:
        for ev in node.get("evolves_to", []):
            for d in ev.get("evolution_details") or [{}]:
                if d.get("min_level") is not None:
                    return d["min_level"]
        return None

    def rarity_for(species: dict) -> str:
        if species.get("is_mythical"):
            return "PokemonRarity.MYTHICAL"
        if species.get("is_legendary"):
            return "PokemonRarity.LEGENDARY"
        return "PokemonRarity.COMMON"

    chain_cache: dict[str, dict] = {}

    def chain_root_for(species: dict) -> dict:
        path = urlparse(species["evolution_chain"]["url"]).path
        if path not in chain_cache:
            chain_cache[path] = fetch_json(path)["chain"]
        return chain_cache[path]

    def java_escape(s: str) -> str:
        return s.replace("\\", "\\\\").replace('"', '\\"')

    def fmt_type(t: str | None) -> str:
        return "null" if t is None else f"PokemonType.{t}"

    def fmt_list(nums: list[int]) -> str:
        if not nums:
            return "List.of()"
        return "List.of(" + ",".join(str(n) for n in nums) + ")"

    entries_by_gen: dict[int, list[str]] = {g: [] for g in range(2, 10)}

    print("Fetching species 152–1025…", flush=True)
    for sid in range(152, 1026):
        sp = fetch_json(f"{BASE_PREFIX}/pokemon-species/{sid}")
        gen = int(sp["generation"]["url"].rstrip("/").split("/")[-1])
        if gen < 2 or gen > 9:
            continue

        slug = sp["name"]
        name = NAME_FIX.get(slug, english_name(sp))
        color = COLOR_MAP.get(sp["color"]["name"], "GRAY")

        default_pokemon_path = f"{BASE_PREFIX}/pokemon/{sid}"
        for v in sp.get("varieties", []):
            if v.get("is_default"):
                default_pokemon_path = urlparse(v["pokemon"]["url"]).path
                break

        p = fetch_json(default_pokemon_path)
        height_m = p["height"] / 10.0
        weight_kg = p["weight"] / 10.0
        ts = sorted(p["types"], key=lambda x: x["slot"])
        primary = ts[0]["type"]["name"].upper()
        sec = ts[1]["type"]["name"].upper() if len(ts) > 1 else None

        chain = chain_root_for(sp)
        evolves = direct_evolves_to(chain, sid)
        stage_str, evo_lv = evolution_stage(sp, chain, sid)
        rarity = rarity_for(sp)

        if stage_str is None:
            stage_java, lv_java = "null", "null"
        else:
            stage_java = stage_str
            lv_java = "null" if evo_lv is None else str(evo_lv)

        line = (
            f'PokemonSeedEntry.pokemon({sid}, "{java_escape(name)}", '
            f"{fmt_type(primary)}, {fmt_type(sec)}, {gen}, PokedexColor.{color}, "
            f"{height_m}, {weight_kg}, {rarity}, {stage_java}, {lv_java}, {fmt_list(evolves)})"
        )
        entries_by_gen[gen].append(line)

        if sid % 100 == 0:
            print(sid, flush=True)

    conn.close()

    root = Path(__file__).resolve().parents[1] / "src/main/java/com/svc/pokeguessteam/config/seed"
    header = """package com.svc.pokeguessteam.config.seed;

import com.svc.pokeguessteam.model.enums.EvolutionStage;
import com.svc.pokeguessteam.model.enums.PokedexColor;
import com.svc.pokeguessteam.model.enums.PokemonRarity;
import com.svc.pokeguessteam.model.enums.PokemonType;

import java.util.List;

public final class Generation{g}Seed {{

    private Generation{g}Seed() {{
    }}

    public static List<PokemonSeedEntry> entries() {{
        return List.of(
"""
    footer = """        );
    }
}
"""
    for g in range(2, 10):
        lines = entries_by_gen[g]
        body = ",\n".join("                " + L for L in lines)
        path = root / f"Generation{g}Seed.java"
        path.write_text(header.format(g=g) + body + "\n" + footer, encoding="utf-8")
        print(f"Wrote {path.name}: {len(lines)} entries", flush=True)

    return 0


if __name__ == "__main__":
    sys.exit(main())
