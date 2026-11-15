#!/usr/bin/env python3
"""Reset a SQLite database from a verified seed database."""

import argparse
import shutil
import sqlite3
import sys
from pathlib import Path

TABLES = (
    "users", "categories", "posts", "tags", "post_tags",
    "comments", "notifications", "post_likes",
)


def main() -> int:
    parser = argparse.ArgumentParser()
    parser.add_argument("--database", required=True)
    parser.add_argument("--seed", required=True)
    args = parser.parse_args()

    database = Path(args.database).resolve()
    seed = Path(args.seed).resolve()
    if not seed.is_file():
        print(f"Seed database not found: {seed}", file=sys.stderr)
        return 2

    database.parent.mkdir(parents=True, exist_ok=True)
    temporary = database.with_suffix(database.suffix + ".reset")
    if temporary.exists():
        temporary.unlink()
    shutil.copy2(seed, temporary)

    conn = sqlite3.connect(temporary, timeout=10)
    try:
        integrity = conn.execute("PRAGMA integrity_check").fetchone()[0]
        foreign_keys = conn.execute("PRAGMA foreign_key_check").fetchall()
        counts = {
            table: conn.execute(f"SELECT COUNT(*) FROM {table}").fetchone()[0]
            for table in TABLES
        }
    finally:
        conn.close()

    try:
        if integrity != "ok" or foreign_keys:
            raise RuntimeError(
                f"invalid seed database: integrity={integrity}, foreign_keys={foreign_keys}"
            )
        temporary.replace(database)
    finally:
        if temporary.exists():
            temporary.unlink()

    print(" ".join(f"{table}={counts[table]}" for table in TABLES))
    return 0


if __name__ == "__main__":
    raise SystemExit(main())
