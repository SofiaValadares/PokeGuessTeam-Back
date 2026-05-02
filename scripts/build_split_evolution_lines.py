#!/usr/bin/env python3
"""
Build EvolutionLinesSeed + patch Generation*Seed (PokeAPI).
- Uma linha por caminho raiz→folha; membros = {@code pokedexNumber} (int) em {@code List.of(1, 2, 3)}.
- Raridade: MYTHICAL / LEGENDARY / RARE (capture_rate <= 45) / COMMON.
- Seeds: apenas pokemon(dex, \"Nome\", ...) — sem variantes.
"""
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
BASE_PREFIX = "/api/v2"
UA = "pokeguessteam-split-lines/1.0"
DEX_MIN, DEX_MAX = 1, 1025

RARITY_ORDER = {"COMMON": 0, "RARE": 1, "LEGENDARY": 2, "MYTHICAL": 3}


def fetch_json(conn: HTTPSConnection, path: str) -> dict:
    conn.request("GET", path, headers={"User-Agent": UA})
    res = conn.getresponse()
    body = res.read()
    if res.status != 200:
        raise RuntimeError(f"{path} -> HTTP {res.status}")
    return json.loads(body)


def species_id_from_url(url: str) -> int:
    return int(url.rstrip("/").split("/")[-1])


def find_chain_node(node: dict, target_id: int) -> dict | None:
    if species_id_from_url(node["species"]["url"]) == target_id:
        return node
    for sub in node.get("evolves_to", []):
        found = find_chain_node(sub, target_id)
        if found:
            return found
    return None


def direct_children(chain_root: dict, target_id: int) -> list[int]:
    node = find_chain_node(chain_root, target_id)
    if not node:
        return []
    out = []
    for ev in node.get("evolves_to", []):
        cid = species_id_from_url(ev["species"]["url"])
        if DEX_MIN <= cid <= DEX_MAX:
            out.append(cid)
    return sorted(out)


def rarity_label(species: dict) -> str:
    if species.get("is_mythical"):
        return "MYTHICAL"
    if species.get("is_legendary"):
        return "LEGENDARY"
    cr = int(species.get("capture_rate", 255))
    return "RARE" if cr <= 45 else "COMMON"


def all_paths_from(root: int, children: dict[int, list[int]]) -> list[list[int]]:
    paths: list[list[int]] = []

    def dfs(node: int, path: list[int]) -> None:
        path.append(node)
        ch = children.get(node, [])
        if not ch:
            paths.append(list(path))
        else:
            for c in ch:
                dfs(c, path)
        path.pop()

    dfs(root, [])
    return paths


def parse_seed_pokemon_lines() -> tuple[dict[str, str], dict[int, set[str]]]:
    """pokemonId (dex string) -> display name; lineKey -> set of pokemonIds."""
    name_by_pid: dict[str, str] = {}
    ids_by_line: dict[int, set[str]] = defaultdict(set)
    tail_re = re.compile(
        r", (EvolutionStage\.\w+|null), (null|\d+), (\d+)\)\s*,?\s*$"
    )
    pat2 = re.compile(
        r"PokemonSeedEntry\.pokemon\(\s*(\d+)\s*,\s*\"((?:[^\"\\]|\\.)*)\""
    )
    for path in sorted(SEED_DIR.glob("Generation*Seed.java")):
        for raw in path.read_text(encoding="utf-8").splitlines():
            st = raw.strip()
            if not st.startswith("PokemonSeedEntry.pokemon("):
                continue
            if re.match(r"PokemonSeedEntry\.pokemon\(\s*\d+\s*,\s*\d+\s*,", st):
                continue
            tm = tail_re.search(st)
            if not tm:
                continue
            lk = int(tm.group(3))
            m2 = pat2.match(st)
            if not m2:
                continue
            dex = int(m2.group(1))
            name = m2.group(2).replace('\\"', '"')
            pid = str(dex)
            name_by_pid[pid] = name
            ids_by_line[lk].add(pid)
    return name_by_pid, ids_by_line


def augment_evolution_lines_seed(
        line_path: Path,
        path_members_by_line: dict[int, list[int]],
        name_by_pid: dict[str, str],
) -> None:
    """Garante que List.of coincide com o caminho API (pokedexNumber int)."""
    entry_re = re.compile(
        r"^(\s*)EvolutionLineSeedEntry\.line\((\d+),\s*(PokemonRarity\.\w+),\s*List\.of\(([^)]*)\)\)(,?)(\s*//.*)?$"
    )
    lines_out: list[str] = []
    for raw in line_path.read_text(encoding="utf-8").splitlines():
        m = entry_re.match(raw.rstrip())
        if not m:
            lines_out.append(raw)
            continue
        indent, lk_s, rar_tok, inner, comma, _comment = m.groups()
        lk = int(lk_s)
        from_path = path_members_by_line.get(lk, [])
        parsed_inner: list[int] = []
        for part in inner.split(","):
            p = part.strip()
            if not p:
                continue
            parsed_inner.append(int(p.strip('"')))
        merged: set[int] = set(parsed_inner)
        merged.update(from_path)
        ordered = sorted(merged)
        label = " → ".join(name_by_pid.get(str(x), str(x)) for x in ordered)
        inner_q = ",".join(str(x) for x in ordered)
        lines_out.append(
            f"{indent}EvolutionLineSeedEntry.line({lk}, {rar_tok}, List.of({inner_q})){comma or ''} // {label}"
        )
    line_path.write_text("\n".join(lines_out) + "\n", encoding="utf-8")


def main() -> None:
    ctx = ssl.create_default_context()
    conn = HTTPSConnection(HOST, context=ctx, timeout=120)
    chain_cache: dict[str, dict] = {}

    def chain_root_for(species: dict) -> dict:
        path = urlparse(species["evolution_chain"]["url"]).path
        if path not in chain_cache:
            chain_cache[path] = fetch_json(conn, path)["chain"]
        return chain_cache[path]

    children: dict[int, list[int]] = {}
    rarity_by_dex: dict[int, str] = {}

    print("Fetching species", DEX_MIN, "–", DEX_MAX, "…", flush=True)
    for sid in range(DEX_MIN, DEX_MAX + 1):
        sp = fetch_json(conn, f"{BASE_PREFIX}/pokemon-species/{sid}")
        rarity_by_dex[sid] = rarity_label(sp)
        root = chain_root_for(sp)
        children[sid] = direct_children(root, sid)
        if sid % 200 == 0:
            print(sid, flush=True)

    conn.close()

    parents: dict[int, list[int]] = defaultdict(list)
    for u, cs in children.items():
        for v in cs:
            parents[v].append(u)

    roots = sorted(d for d in range(DEX_MIN, DEX_MAX + 1) if not parents[d])

    line_members: list[list[int]] = []
    seen: set[tuple[int, ...]] = set()

    def add_line(members: list[int]) -> None:
        t = tuple(members)
        if t in seen:
            return
        seen.add(t)
        line_members.append(members)

    for r in roots:
        for path in all_paths_from(r, children):
            add_line(path)

    line_members.sort(key=lambda m: (m[0], len(m), m[-1], tuple(m)))

    def line_rarity(members: list[int]) -> str:
        best = "COMMON"
        for d in members:
            r = rarity_by_dex.get(d, "COMMON")
            if RARITY_ORDER[r] > RARITY_ORDER[best]:
                best = r
        return best

    numbered = [(i + 1, mem, line_rarity(mem)) for i, mem in enumerate(line_members)]

    path_members_by_line: dict[int, list[int]] = {}
    for lid, mem, _ in numbered:
        path_members_by_line[lid] = list(mem)

    candidates: dict[int, list[tuple[int, list[int]]]] = defaultdict(list)
    for lid, mem, _ in numbered:
        for d in mem:
            candidates[d].append((lid, mem))

    def pick_line_id(dex: int) -> int:
        cands = candidates[dex]
        if len(cands) == 1:
            return cands[0][0]
        return min(cands, key=lambda x: (-len(x[1]), x[0]))[0]

    dex_to_line = {d: pick_line_id(d) for d in range(DEX_MIN, DEX_MAX + 1)}

    name_by_pid, _ids_by_line = parse_seed_pokemon_lines()

    evo_lines: list[str] = []
    for idx, (lid, mem, rar) in enumerate(numbered):
        mem_ids = path_members_by_line[lid]
        inner = ",".join(str(x) for x in mem_ids)
        label = " → ".join(name_by_pid.get(str(x), str(x)) for x in mem_ids)
        comma = "," if idx < len(numbered) - 1 else ""
        evo_lines.append(
            f"                EvolutionLineSeedEntry.line({lid}, PokemonRarity.{rar}, List.of({inner})){comma} // {label}"
        )

    evo_path = SEED_DIR / "EvolutionLinesSeed.java"
    evo_path.write_text(
        """package com.svc.pokeguessteam.config.seed;

import com.svc.pokeguessteam.model.enums.PokemonRarity;

import java.util.List;

/**
 * Linhas evolutivas por caminho na API PokeAPI.
 * Membros: {@code pokedexNumber} (número nacional), alinhado a {@link com.svc.pokeguessteam.model.pokemon.PokemonModel#getPokedexNumber()}.
 */
public final class EvolutionLinesSeed {

    private EvolutionLinesSeed() {
    }

    public static List<EvolutionLineSeedEntry> entries() {
        return List.of(
"""
        + "\n".join(evo_lines)
        + """
        );
    }
}
""",
        encoding="utf-8",
    )

    augment_evolution_lines_seed(evo_path, path_members_by_line, name_by_pid)

    for path in sorted(SEED_DIR.glob("Generation*Seed.java")):
        out: list[str] = []
        for raw in path.read_text(encoding="utf-8").splitlines():
            stripped = raw.strip()
            if not stripped.startswith("PokemonSeedEntry.pokemon("):
                out.append(raw)
                continue
            m_dex = re.search(r"pokemon\((\d+),", stripped)
            if not m_dex:
                out.append(raw)
                continue
            dex = int(m_dex.group(1))
            rm = re.search(
                r", (EvolutionStage\.\w+|null), (null|\d+), (\"[^\"]+\"|\d+)\)\s*,?\s*$",
                stripped,
            )
            if not rm:
                raise ValueError(f"Cannot parse tail: {stripped[:100]}")
            prefix = stripped[: rm.start()]
            stage, level, existing_key = rm.group(1), rm.group(2), rm.group(3)
            indent = raw[: len(raw) - len(raw.lstrip())]
            lid = dex_to_line.get(dex, int(existing_key))
            out.append(f"{indent}{prefix}, {stage}, {level}, {lid}),")

        for i in range(len(out) - 1):
            if out[i + 1].strip() == ");" and out[i].rstrip().endswith("),"):
                out[i] = out[i].rstrip()[:-1]

        path.write_text("\n".join(out) + "\n", encoding="utf-8")

    print(f"Wrote EvolutionLinesSeed.java: {len(numbered)} lines", flush=True)
    print("Patched Generation*Seed.java", flush=True)


if __name__ == "__main__":
    main()
