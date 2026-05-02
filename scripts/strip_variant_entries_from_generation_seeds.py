#!/usr/bin/env python3
"""Remove PokemonSeedEntry lines that use the 3-arg form pokemon(dex, variant, ...)."""
from __future__ import annotations

import re
from pathlib import Path

ROOT = Path(__file__).resolve().parents[1]
SEED_DIR = ROOT / "src/main/java/com/svc/pokeguessteam/config/seed"

PAT_3ARG = re.compile(r"PokemonSeedEntry\.pokemon\(\s*\d+\s*,\s*\d+\s*,")


def main() -> None:
    for path in sorted(SEED_DIR.glob("Generation*Seed.java")):
        lines = path.read_text(encoding="utf-8").splitlines()
        kept = [ln for ln in lines if not PAT_3ARG.search(ln.strip())]
        path.write_text("\n".join(kept) + "\n", encoding="utf-8")
        print(path.name, flush=True)


if __name__ == "__main__":
    main()
