#!/usr/bin/env python3
"""Detect drift between narrative development docs and machine specs."""

from __future__ import annotations

import csv
import re
import sys
from pathlib import Path
from typing import Any

try:
    import yaml
except ImportError as exc:  # pragma: no cover - environment guard
    print(f"ERROR: PyYAML is required to validate openapi.yaml: {exc}")
    sys.exit(2)


ROOT = Path(__file__).resolve().parents[2]
DOCS = ROOT / "docs" / "development"
SPECS = DOCS / "specs"
HTTP_METHODS = {"GET", "POST", "PUT", "PATCH", "DELETE"}


def read_csv(name: str) -> list[dict[str, str]]:
    with (SPECS / name).open("r", encoding="utf-8-sig", newline="") as handle:
        return list(csv.DictReader(handle))


def normalize_path(path: str) -> str:
    if path.startswith("/api/v1"):
        return path[len("/api/v1") :] or "/"
    return path


def method_path_mentions(text: str) -> list[tuple[str, str]]:
    mentions: list[tuple[str, str]] = []
    pattern = re.compile(r"`((?:GET|POST|PUT|PATCH|DELETE)\s+)(/[^`\s]+)`")
    for match in pattern.finditer(text):
        method = match.group(1).strip()
        path = normalize_path(match.group(2).strip())
        mentions.append((method, path))
    return mentions


def resource_path_mentions(text: str) -> list[str]:
    mentions: list[str] = []
    pattern = re.compile(r"`(/[^`\s]+)`")
    for match in pattern.finditer(text):
        path = normalize_path(match.group(1).strip())
        if path.startswith("/portal") or path.startswith("/internal"):
            continue
        # Resource rows in 07 use bare paths. Exact action paths are covered by
        # method_path_mentions and should not be double-counted here.
        if "{" in path or path.count("/") > 1:
            continue
        mentions.append(path)
    return mentions


def local_markdown_link_errors() -> list[str]:
    errors: list[str] = []
    link_pattern = re.compile(r"\[[^\]]+\]\(([^)]+)\)")
    for doc in sorted(DOCS.glob("*.md")):
        text = doc.read_text(encoding="utf-8")
        for match in link_pattern.finditer(text):
            target = match.group(1).split("#", 1)[0]
            if not target or re.match(r"^[a-zA-Z]+:", target):
                continue
            target_path = (doc.parent / target.replace("%20", " ")).resolve()
            if not target_path.exists():
                errors.append(f"{doc.name}: missing local link target {target}")
    return errors


def route_role_errors(role_codes: set[str]) -> list[str]:
    errors: list[str] = []
    api_doc = next(DOCS.glob("07_*API*.md"))
    text = api_doc.read_text(encoding="utf-8")
    route_row = re.compile(r"\| `[^`]+` \| `[^`]+` \| `[^`]+` \| ([^|]+) \|")
    for row in route_row.finditer(text):
        roles_text = row.group(1).strip()
        if roles_text == "none":
            continue
        for role in [part.strip(" `") for part in roles_text.split(",")]:
            if role and role not in role_codes:
                errors.append(f"07_API与界面契约.md: route references unknown role {role}")
    return errors


def main() -> int:
    errors: list[str] = []
    openapi: dict[str, Any] = yaml.safe_load((SPECS / "openapi.yaml").read_text(encoding="utf-8"))
    paths = openapi.get("paths", {})
    openapi_pairs = {
        (method.upper(), path)
        for path, operations in paths.items()
        for method in operations
        if method.upper() in HTTP_METHODS
    }
    openapi_path_set = set(paths)

    for doc_name in ["07_API与界面契约.md", "10_工作中心详细设计.md"]:
        doc = DOCS / doc_name
        text = doc.read_text(encoding="utf-8")
        for method, path in method_path_mentions(text):
            if (method, path) not in openapi_pairs:
                errors.append(f"{doc_name}: {method} {path} missing from specs/openapi.yaml")
        if doc_name == "07_API与界面契约.md":
            for path in resource_path_mentions(text):
                prefix = path.rstrip("/") + "/"
                if path not in openapi_path_set and not any(item.startswith(prefix) for item in openapi_path_set):
                    errors.append(f"{doc_name}: resource {path} missing from specs/openapi.yaml")

    security_schemes = openapi.get("components", {}).get("securitySchemes", {})
    if "bearerAuth" not in security_schemes:
        errors.append("openapi.yaml: missing bearerAuth security scheme")
    if {"bearerAuth": []} not in openapi.get("security", []):
        errors.append("openapi.yaml: missing global bearerAuth security requirement")

    roles = read_csv("roles.csv")
    role_codes = {row["roleCode"].strip() for row in roles}
    errors += route_role_errors(role_codes)
    errors += local_markdown_link_errors()

    if errors:
        print("DRIFT DETECTED")
        for error in errors:
            print(f"- {error}")
        return 1

    print("NO DRIFT DETECTED")
    print(f"- OpenAPI paths checked: {len(paths)}")
    print("- Markdown local links checked")
    print("- Route role references checked")
    return 0


if __name__ == "__main__":
    raise SystemExit(main())
