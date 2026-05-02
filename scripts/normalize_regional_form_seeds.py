#!/usr/bin/env python3
"""DEPRECATED: este projeto não usa mais variantes regionais. Não executar salvo migração legada.
Rewrite PokemonSeedEntry lines: pokemon(apiId>1025, -> pokemon(nationalDex, variant, for regional forms."""
from __future__ import annotations

import json
import re
import ssl
from http.client import HTTPSConnection
from pathlib import Path

ROOT = Path(__file__).resolve().parents[1]
SEED_DIR = ROOT / "src/main/java/com/svc/pokeguessteam/config/seed"

HOST = "pokeapi.co"
BASE = "/api/v2"
UA = "pokeguessteam-normalize-forms/1.0"


def fetch_json(conn: HTTPSConnection, path: str) -> dict:
    conn.request("GET", path, headers={"User-Agent": UA})
    r = conn.getresponse()
    body = r.read()
    if r.status != 200:
        raise RuntimeError(f"{path} HTTP {r.status}")
    return json.loads(body)


def species_id_from_url(url: str) -> int:
    return int(url.rstrip("/").split("/")[-1])


def main() -> None:
    ctx = ssl.create_default_context()
    conn = HTTPSConnection(HOST, context=ctx, timeout=120)

    api_ids: set[int] = set()
    pat_lead = re.compile(r"PokemonSeedEntry\.pokemon\(\s*(\d+)\s*,")
    for path in sorted(SEED_DIR.glob("Generation*Seed.java")):
        for line in path.read_text(encoding="utf-8").splitlines():
            m = pat_lead.match(line.strip())
            if m and int(m.group(1)) > 1025:
                api_ids.add(int(m.group(1)))

    species_needed: set[int] = set()
    for pid in sorted(api_ids):
        pk = fetch_json(conn, f"{BASE}/pokemon/{pid}")
        species_needed.add(species_id_from_url(pk["species"]["url"]))

    variant_by_api: dict[int, tuple[int, int]] = {}
    for sid in sorted(species_needed):
        sp = fetch_json(conn, f"{BASE}/pokemon-species/{sid}")
        rows: list[tuple[int, bool]] = []
        for v in sp.get("varieties", []):
            pid = species_id_from_url(v["pokemon"]["url"])
            rows.append((pid, bool(v.get("is_default"))))
        rows.sort(key=lambda x: (not x[1], x[0]))
        for vi, (pid, _) in enumerate(rows):
            variant_by_api[pid] = (sid, vi)

    conn.close()

    pat_already = re.compile(r"PokemonSeedEntry\.pokemon\(\s*\d+\s*,\s*\d+\s*,")
    for path in sorted(SEED_DIR.glob("Generation*Seed.java")):
        lines_out = []
        for line in path.read_text(encoding="utf-8").splitlines():
            st = line.strip()
            if pat_already.match(st):
                lines_out.append(line)
                continue
            m = pat_lead.match(st)
            if not m:
                lines_out.append(line)
                continue
            lead = int(m.group(1))
            if lead <= 1025:
                lines_out.append(line)
                continue
            national, varn = variant_by_api[lead]
            idx = line.find("PokemonSeedEntry.pokemon(")
            paren = line.find(",", idx)
            quote_idx = line.find('"', paren)
            if quote_idx < 0:
                raise ValueError(line)
            indent = line[:idx]
            rest_from_quote = line[quote_idx:]
            lines_out.append(
                f"{indent}PokemonSeedEntry.pokemon({national}, {varn}, {rest_from_quote}"
            )
        path.write_text("\n".join(lines_out) + "\n", encoding="utf-8")
        print(path.name)


if __name__ == "__main__":
    main()
