#!/usr/bin/env python3
"""
Rebuild Generation*Seed.java and write EvolutionLinesSeed.java from current
PokemonSeedEntry lines (rarity + List.of evolvesTo).
"""
from __future__ import annotations

import re
from collections import defaultdict
from pathlib import Path

ROOT = Path(__file__).resolve().parents[1]
SEED_DIR = ROOT / "src/main/java/com/svc/pokeguessteam/config/seed"

RARITY_ORDER = {"COMMON": 0, "RARE": 1, "LEGENDARY": 2, "MYTHICAL": 3}


def parse_list_of(s: str) -> list[int]:
    inner = s[len("List.of(") : -1].strip()
    if not inner:
        return []
    return [int(x.strip()) for x in inner.split(",")]


def parse_pokemon_line(line: str) -> tuple[str, str, str, str, list[int]] | None:
    """Returns (head_before_rarity, rarity, stage, level_str, evolves_to) or None."""
    if "PokemonSeedEntry.pokemon(" not in line:
        return None
    if ", PokemonRarity." not in line:
        return None
    head, tail = line.split(", PokemonRarity.", 1)
    rarity_part, _, rest = tail.partition(", ")
    # rest: EvolutionStage..., null|digits, List.of(...)
    m = re.match(
        r"(EvolutionStage\.\w+|null), (null|\d+), (List\.of\([^)]*\))\)\s*,?\s*$",
        rest,
    )
    if not m:
        raise ValueError(f"Bad tail: {rest!r} in {line[:120]}")
    stage, level, listof = m.group(1), m.group(2), m.group(3)
    return head, rarity_part, stage, level, parse_list_of(listof)


def union_find(vertices: set[int], edges: list[tuple[int, int]]) -> dict[int, int]:
    parent = {v: v for v in vertices}

    def find(x: int) -> int:
        if parent[x] != x:
            parent[x] = find(parent[x])
        return parent[x]

    def union(a: int, b: int) -> None:
        ra, rb = find(a), find(b)
        if ra != rb:
            parent[rb] = ra

    for a, b in edges:
        union(a, b)
    return parent


def components(vertices: set[int], parent: dict[int, int]) -> dict[int, list[int]]:
    def root(x: int) -> int:
        while parent[x] != x:
            x = parent[x]
        return x

    groups: dict[int, list[int]] = defaultdict(list)
    for v in vertices:
        groups[root(v)].append(v)
    return {min(members): sorted(members) for _, members in groups.items()}


def main() -> None:
    all_rows: list[tuple[int, str, str, str, str, list[int]]] = []
    edges: list[tuple[int, int]] = []

    for path in sorted(SEED_DIR.glob("Generation*Seed.java")):
        text = path.read_text(encoding="utf-8")
        for raw_line in text.splitlines():
            stripped = raw_line.strip()
            if not stripped.startswith("PokemonSeedEntry.pokemon("):
                continue
            parsed = parse_pokemon_line(stripped)
            if not parsed:
                continue
            head, rarity, stage, level, evos = parsed
            dex_m = re.search(r"pokemon\((\d+),", head)
            if not dex_m:
                raise ValueError(head[:80])
            dex = int(dex_m.group(1))
            all_rows.append((dex, head, rarity, stage, level, evos))
            for t in evos:
                edges.append((dex, t))

    vertices = {dex for dex, *_ in all_rows}
    for a, b in edges:
        vertices.add(a)
        vertices.add(b)

    parent = union_find(vertices, edges)
    # path compression final
    comp_by_min = components(vertices, parent)

    dex_to_key: dict[int, str] = {}
    line_meta: dict[str, tuple[str, list[int]]] = {}
    dex_to_rarity: dict[int, str] = {dex: r for dex, _, r, _, _, _ in all_rows}

    for min_dex, members in comp_by_min.items():
        key = f"line-{min_dex}"
        for m in members:
            dex_to_key[m] = key
        best = "COMMON"
        for m in members:
            r = dex_to_rarity.get(m, "COMMON")
            if RARITY_ORDER[r] > RARITY_ORDER[best]:
                best = r
        line_meta[key] = (best, members)

    # Write EvolutionLinesSeed.java
    lines_java = sorted(line_meta.items(), key=lambda x: x[1][1][0])
    evo_entries = []
    for key, (rarity, members) in lines_java:
        inner = ",".join(str(x) for x in members)
        evo_entries.append(
            f'                EvolutionLineSeedEntry.line("{key}", PokemonRarity.{rarity}, List.of({inner}))'
        )

    evo_path = SEED_DIR / "EvolutionLinesSeed.java"
    evo_path.write_text(
        """package com.svc.pokeguessteam.config.seed;

import com.svc.pokeguessteam.model.enums.PokemonRarity;

import java.util.List;

public final class EvolutionLinesSeed {

    private EvolutionLinesSeed() {
    }

    public static List<EvolutionLineSeedEntry> entries() {
        return List.of(
"""
        + ",\n".join(evo_entries)
        + """
        );
    }
}
""",
        encoding="utf-8",
    )

    # Rewrite generation seeds
    for path in sorted(SEED_DIR.glob("Generation*Seed.java")):
        out_lines = []
        for raw_line in path.read_text(encoding="utf-8").splitlines():
            stripped = raw_line.strip()
            if not stripped.startswith("PokemonSeedEntry.pokemon("):
                out_lines.append(raw_line)
                continue
            parsed = parse_pokemon_line(stripped)
            if not parsed:
                out_lines.append(raw_line)
                continue
            head, _rarity, stage, level, _evos = parsed
            dex_m = re.search(r"pokemon\((\d+),", head)
            dex = int(dex_m.group(1))
            lk = dex_to_key[dex]
            indent = raw_line[: len(raw_line) - len(raw_line.lstrip())]
            new_line = (
                f'{indent}{head}, {stage}, {level}, "{lk}"),'
            )
            out_lines.append(new_line)

        for i in range(len(out_lines) - 1):
            if out_lines[i + 1].strip() == ");" and out_lines[i].rstrip().endswith("),"):
                out_lines[i] = out_lines[i].rstrip()[:-1]

        body = "\n".join(out_lines) + "\n"
        body = body.replace(
            "import com.svc.pokeguessteam.model.enums.PokemonRarity;\n", ""
        )
        path.write_text(body, encoding="utf-8")

    print(f"Wrote {evo_path.name} with {len(line_meta)} lines")
    print(f"Updated {len(list(SEED_DIR.glob('Generation*Seed.java')))} generation seeds")


if __name__ == "__main__":
    main()
