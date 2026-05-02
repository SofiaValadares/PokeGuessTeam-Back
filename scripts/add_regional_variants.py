#!/usr/bin/env python3
"""DEPRECATED: o backend não mantém mais variantes regionais nas seeds."""
from __future__ import annotations

import json
import re
import ssl
from collections import defaultdict
from http.client import HTTPSConnection
from pathlib import Path
from urllib.parse import urlparse

ROOT = Path(__file__).resolve().parents[1]
SEED_DIR = ROOT / "src/main/java/com/svc/pokeguessteam/config/seed"

HOST = "pokeapi.co"
UA = "pokeguessteam-regional-variants/1.0"
BASE = "/api/v2"

REGION_TO_GEN = {
    "alola": 7,
    "galar": 8,
    "hisui": 8,
    "paldea": 9,
}

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

TYPE_MAP = {
    "normal": "NORMAL",
    "fire": "FIRE",
    "water": "WATER",
    "grass": "GRASS",
    "flying": "FLYING",
    "fighting": "FIGHTING",
    "poison": "POISON",
    "electric": "ELECTRIC",
    "ground": "GROUND",
    "rock": "ROCK",
    "psychic": "PSYCHIC",
    "ice": "ICE",
    "bug": "BUG",
    "ghost": "GHOST",
    "steel": "STEEL",
    "dragon": "DRAGON",
    "dark": "DARK",
    "fairy": "FAIRY",
}


def fetch_json(conn: HTTPSConnection, path: str) -> dict:
    conn.request("GET", path, headers={"User-Agent": UA})
    resp = conn.getresponse()
    body = resp.read()
    if resp.status != 200:
        raise RuntimeError(f"{path}: HTTP {resp.status}")
    return json.loads(body)


def species_id_from_url(url: str) -> int:
    return int(url.rstrip("/").split("/")[-1])


def parse_existing_entries() -> tuple[set[int], dict[int, tuple[str, str, int]]]:
    existing_numbers: set[int] = set()
    # species dex -> (stage, level, lineKey)
    base_meta: dict[int, tuple[str, str, int]] = {}
    pat = re.compile(
        r"PokemonSeedEntry\.pokemon\((\d+), .*?, (EvolutionStage\.\w+|null), (null|\d+), (\d+)\),?$"
    )
    for path in sorted(SEED_DIR.glob("Generation*Seed.java")):
        for line in path.read_text(encoding="utf-8").splitlines():
            s = line.strip()
            m = pat.match(s)
            if not m:
                continue
            dex = int(m.group(1))
            stage, level, line_key = m.group(2), m.group(3), int(m.group(4))
            existing_numbers.add(dex)
            if dex <= 1025:
                base_meta[dex] = (stage, level, line_key)
    return existing_numbers, base_meta


def english_species_name(species: dict) -> str:
    for name in species.get("names", []):
        if name.get("language", {}).get("name") == "en":
            return name.get("name", species["name"].title())
    return species["name"].replace("-", " ").title()


def variant_display_name(base_name: str, pokemon_slug: str, region: str) -> str:
    parts = pokemon_slug.split("-")
    try:
        idx = parts.index(region)
    except ValueError:
        idx = 1
    extra = parts[idx + 1 :]
    region_title = region.title()
    if extra:
        suffix = " ".join(p.title() for p in extra)
        return f"{base_name} ({region_title} {suffix})"
    return f"{base_name} ({region_title})"


def main() -> None:
    existing_numbers, base_meta = parse_existing_entries()

    ctx = ssl.create_default_context()
    conn = HTTPSConnection(HOST, context=ctx, timeout=120)

    plist = fetch_json(conn, f"{BASE}/pokemon?limit=20000&offset=0")
    regional_entries_by_gen: dict[int, list[str]] = defaultdict(list)
    added = 0

    for row in plist.get("results", []):
        name = row.get("name", "")
        region = next((r for r in REGION_TO_GEN if f"-{r}" in name), None)
        if not region:
            continue
        if "totem" in name or "gmax" in name or "mega" in name:
            continue

        poke = fetch_json(conn, urlparse(row["url"]).path)
        poke_id = int(poke["id"])
        if poke_id in existing_numbers:
            continue
        species_id = species_id_from_url(poke["species"]["url"])
        if species_id not in base_meta:
            continue

        species = fetch_json(conn, f"{BASE}/pokemon-species/{species_id}")
        base_name = english_species_name(species)
        display_name = variant_display_name(base_name, name, region)

        stage, level, line_key = base_meta[species_id]
        gen = REGION_TO_GEN[region]
        color = COLOR_MAP.get(species["color"]["name"], "GRAY")

        types = sorted(poke["types"], key=lambda x: x["slot"])
        primary = TYPE_MAP[types[0]["type"]["name"]]
        secondary = None
        if len(types) > 1:
            secondary = TYPE_MAP[types[1]["type"]["name"]]
        secondary_java = f"PokemonType.{secondary}" if secondary else "null"

        h = poke["height"] / 10.0
        w = poke["weight"] / 10.0

        line = (
            f'                PokemonSeedEntry.pokemon({poke_id}, "{display_name}", '
            f"PokemonType.{primary}, {secondary_java}, {gen}, PokedexColor.{color}, "
            f"{h}, {w}, {stage}, {level}, {line_key}),"
        )
        regional_entries_by_gen[gen].append(line)
        existing_numbers.add(poke_id)
        added += 1

    conn.close()

    for gen, entries in regional_entries_by_gen.items():
        path = SEED_DIR / f"Generation{gen}Seed.java"
        lines = path.read_text(encoding="utf-8").splitlines()
        # Insert before closing ');'
        insert_idx = None
        for i, line in enumerate(lines):
            if line.strip() == ");":
                insert_idx = i
                break
        if insert_idx is None:
            raise RuntimeError(f"Could not find List.of end in {path.name}")
        # ensure previous pokemon line has comma
        prev = insert_idx - 1
        while prev >= 0 and not lines[prev].strip():
            prev -= 1
        if prev >= 0 and lines[prev].strip().startswith("PokemonSeedEntry.pokemon(") and not lines[prev].rstrip().endswith(","):
            lines[prev] = lines[prev].rstrip() + ","
        lines = lines[:insert_idx] + entries + lines[insert_idx:]
        # remove trailing comma from last pokemon entry in file
        for i in range(len(lines) - 1):
            if lines[i + 1].strip() == ");" and lines[i].rstrip().endswith(","):
                lines[i] = lines[i].rstrip()[:-1]
                break
        path.write_text("\n".join(lines) + "\n", encoding="utf-8")
        print(f"{path.name}: +{len(entries)}")

    print(f"Added regional variants: {added}")


if __name__ == "__main__":
    main()
