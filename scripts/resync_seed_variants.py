#!/usr/bin/env python3
"""
DEPRECATED: este projeto não usa mais variantes. Use strip_variant_entries_from_generation_seeds.py.
Reassign variantNumber in Generation*Seed.java using PokeAPI species.varieties.
Matches seed display names to API pokemon.name slugs via normalized alphanumeric keys.
"""
from __future__ import annotations

import json
import re
import ssl
import unicodedata
from http.client import HTTPSConnection
from pathlib import Path

ROOT = Path(__file__).resolve().parents[1]
SEED_DIR = ROOT / "src/main/java/com/svc/pokeguessteam/config/seed"

HOST = "pokeapi.co"
BASE = "/api/v2"
UA = "pokeguessteam-resync-variants/1.0"


def fetch_json(conn: HTTPSConnection, path: str) -> dict:
    conn.request("GET", path, headers={"User-Agent": UA})
    r = conn.getresponse()
    body = r.read()
    if r.status != 200:
        raise RuntimeError(f"{path} HTTP {r.status}")
    return json.loads(body)


def species_id_from_url(url: str) -> int:
    return int(url.rstrip("/").split("/")[-1])


def norm_key(s: str) -> str:
    s = unicodedata.normalize("NFKD", s)
    s = "".join(c for c in s if not unicodedata.combining(c))
    return re.sub(r"[^a-z0-9]+", "", s.lower())


def variety_rows(conn: HTTPSConnection, national_dex: int) -> list[tuple[str, bool, int]]:
    sp = fetch_json(conn, f"{BASE}/pokemon-species/{national_dex}")
    rows: list[tuple[str, bool, int]] = []
    for v in sp.get("varieties", []):
        pid = species_id_from_url(v["pokemon"]["url"])
        pk = fetch_json(conn, f"{BASE}/pokemon/{pid}")
        slug = pk["name"]
        is_def = bool(v.get("is_default"))
        rows.append((slug, is_def, pid))
    rows.sort(key=lambda x: (not x[1], x[2]))
    return rows


def slug_key(slug: str) -> str:
    return norm_key(slug.replace("-", ""))


def match_variant_index(seed_name: str, rows: list[tuple[str, bool, int]]) -> int:
    sk = norm_key(seed_name)
    exact = [i for i, (slug, _, _) in enumerate(rows) if slug_key(slug) == sk]
    if len(exact) == 1:
        return exact[0]
    if len(exact) > 1:
        return exact[0]
    for i, (slug, _, _) in enumerate(rows):
        if sk == norm_key(slug.replace("-", " ")):
            return i
    for i, (slug, _, _) in enumerate(rows):
        if sk == norm_key(slug.split("-")[0]):
            return i
    raise LookupError(f"No API variety for name={seed_name!r} among {rows}")


def main() -> None:
    ctx = ssl.create_default_context()
    conn = HTTPSConnection(HOST, context=ctx, timeout=120)

    pat3 = re.compile(
        r"^([ \t]*)PokemonSeedEntry\.pokemon\(\s*(\d+)\s*,\s*(\d+)\s*,\s*"
        r'"((?:[^"\\]|\\.)*)"(.*)$'
    )
    pat2 = re.compile(
        r"^([ \t]*)PokemonSeedEntry\.pokemon\(\s*(\d+)\s*,\s*"
        r'"((?:[^"\\]|\\.)*)"(.*)$'
    )

    dex_needed: set[int] = set()
    for path in sorted(SEED_DIR.glob("Generation*Seed.java")):
        for line in path.read_text(encoding="utf-8").splitlines():
            m3 = pat3.match(line)
            m2 = pat2.match(line)
            if m3:
                dex_needed.add(int(m3.group(2)))
            elif m2:
                dex_needed.add(int(m2.group(2)))

    cache: dict[int, list[tuple[str, bool, int]]] = {}
    for d in sorted(dex_needed):
        cache[d] = variety_rows(conn, d)
        if d % 100 == 0:
            print("species", d, flush=True)

    conn.close()

    for path in sorted(SEED_DIR.glob("Generation*Seed.java")):
        out: list[str] = []
        for line in path.read_text(encoding="utf-8").splitlines():
            m3 = pat3.match(line)
            m2 = pat2.match(line)
            if not m3 and not m2:
                out.append(line)
                continue
            if m3:
                indent, dex_s, _old_v, name_raw, tail = m3.groups()
                dex = int(dex_s)
            else:
                indent, dex_s, name_raw, tail = m2.groups()
                dex = int(dex_s)
            name = name_raw.replace('\\"', '"')
            rows = cache[dex]
            try:
                vi = match_variant_index(name, rows)
            except LookupError as e:
                print(f"{path.name}: {e}", flush=True)
                out.append(line)
                continue
            if m2 and vi == 0:
                out.append(f'{indent}PokemonSeedEntry.pokemon({dex}, "{name_raw}"{tail}')
            else:
                out.append(
                    f'{indent}PokemonSeedEntry.pokemon({dex}, {vi}, "{name_raw}"{tail}'
                )
        path.write_text("\n".join(out) + "\n", encoding="utf-8")
        print("wrote", path.name, flush=True)


if __name__ == "__main__":
    main()
