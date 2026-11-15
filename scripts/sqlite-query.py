#!/usr/bin/env python3
"""Execute a read-only SQLite query and print tab-separated rows."""

import sqlite3
import sys
from pathlib import Path


def main() -> int:
    if len(sys.argv) != 3:
        print("usage: sqlite-query.py <database> <sql>", file=sys.stderr)
        return 2

    db_path = Path(sys.argv[1]).resolve()
    if not db_path.is_file():
        print(f"SQLite database not found: {db_path}", file=sys.stderr)
        return 2

    uri = db_path.as_uri() + "?mode=ro"
    with sqlite3.connect(uri, uri=True, timeout=10) as conn:
        conn.execute("PRAGMA foreign_keys = ON")
        rows = conn.execute(sys.argv[2]).fetchall()
        for row in rows:
            print("\t".join("" if value is None else str(value) for value in row))
    return 0


if __name__ == "__main__":
    raise SystemExit(main())
