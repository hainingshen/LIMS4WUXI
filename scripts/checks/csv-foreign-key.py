#!/usr/bin/env python3
"""Validate machine-readable LIMS development specs.

This script is intentionally dependency-light except for PyYAML, which is also
needed by common OpenAPI tooling. It is designed for CI and for a new
development machine before code generation starts.
"""

from __future__ import annotations

import csv
import json
import sys
from pathlib import Path
from typing import Any

try:
    import yaml
except ImportError as exc:  # pragma: no cover - environment guard
    print(f"ERROR: PyYAML is required to validate openapi.yaml: {exc}")
    sys.exit(2)


ROOT = Path(__file__).resolve().parents[2]
SPECS = ROOT / "docs" / "development" / "specs"


def read_csv(name: str) -> list[dict[str, str]]:
    path = SPECS / name
    if not path.exists():
        raise FileNotFoundError(path)
    with path.open("r", encoding="utf-8-sig", newline="") as handle:
        rows = list(csv.DictReader(handle))
    if not rows:
        raise ValueError(f"{path} has no data rows")
    return rows


def assert_unique(rows: list[dict[str, str]], keys: tuple[str, ...], label: str) -> list[str]:
    errors: list[str] = []
    seen: set[tuple[str, ...]] = set()
    for index, row in enumerate(rows, start=2):
        key = tuple((row.get(k) or "").strip() for k in keys)
        if any(not part for part in key):
            errors.append(f"{label}: row {index} has blank key {keys}: {key}")
            continue
        if key in seen:
            errors.append(f"{label}: duplicate key {keys}: {key}")
        seen.add(key)
    return errors


def collect_refs(node: Any, refs: list[str]) -> None:
    if isinstance(node, dict):
        ref = node.get("$ref")
        if isinstance(ref, str):
            refs.append(ref)
        for value in node.values():
            collect_refs(value, refs)
    elif isinstance(node, list):
        for value in node:
            collect_refs(value, refs)


def resolve_ref(document: dict[str, Any], ref: str) -> bool:
    if not ref.startswith("#/"):
        return True
    current: Any = document
    for raw_part in ref[2:].split("/"):
        part = raw_part.replace("~1", "/").replace("~0", "~")
        if not isinstance(current, dict) or part not in current:
            return False
        current = current[part]
    return True


def main() -> int:
    errors: list[str] = []

    roles = read_csv("roles.csv")
    role_codes = {row["roleCode"].strip() for row in roles}
    errors += assert_unique(roles, ("roleCode",), "roles.csv")

    permissions = read_csv("permission_matrix.csv")
    errors += assert_unique(permissions, ("actionCode", "roleCode"), "permission_matrix.csv")
    for index, row in enumerate(permissions, start=2):
        role_code = row.get("roleCode", "").strip()
        if role_code not in role_codes:
            errors.append(f"permission_matrix.csv: row {index} unknown roleCode {role_code!r}")

    dictionaries = read_csv("dictionaries.csv")
    errors += assert_unique(dictionaries, ("dictCode", "itemCode"), "dictionaries.csv")
    dict_codes = {row["dictCode"].strip() for row in dictionaries}
    dict_items = {(row["dictCode"].strip(), row["itemCode"].strip()) for row in dictionaries}

    submission_fields = read_csv("submission_ver15_fields.csv")
    errors += assert_unique(submission_fields, ("field_key",), "submission_ver15_fields.csv")
    for index, row in enumerate(submission_fields, start=2):
        dict_code = row.get("dictionary", "").strip()
        if dict_code and dict_code not in dict_codes:
            errors.append(
                f"submission_ver15_fields.csv: row {index} field {row.get('field_key')} "
                f"references unknown dictionary {dict_code!r}"
            )
        lock_policy = row.get("lock_policy", "").strip()
        if lock_policy and ("LOCK_POLICY", lock_policy) not in dict_items:
            errors.append(
                f"submission_ver15_fields.csv: row {index} field {row.get('field_key')} "
                f"references unknown lock_policy {lock_policy!r}"
            )

    test_catalog = read_csv("test_catalog.csv")
    errors += assert_unique(test_catalog, ("testCode",), "test_catalog.csv")

    report_mapping = read_csv("sterility_report_mapping.csv")
    errors += assert_unique(report_mapping, ("target_field",), "sterility_report_mapping.csv")

    openapi_path = SPECS / "openapi.yaml"
    openapi = yaml.safe_load(openapi_path.read_text(encoding="utf-8"))
    if not isinstance(openapi, dict):
        errors.append("openapi.yaml: root document is not an object")
    else:
        if openapi.get("openapi") != "3.0.3":
            errors.append("openapi.yaml: expected openapi version 3.0.3")
        security_schemes = openapi.get("components", {}).get("securitySchemes", {})
        if "bearerAuth" not in security_schemes:
            errors.append("openapi.yaml: missing components.securitySchemes.bearerAuth")
        refs: list[str] = []
        collect_refs(openapi, refs)
        missing_refs = sorted({ref for ref in refs if not resolve_ref(openapi, ref)})
        errors += [f"openapi.yaml: unresolved $ref {ref}" for ref in missing_refs]

        schemas = openapi.get("components", {}).get("schemas", {})
        for schema_name, schema in schemas.items():
            if not isinstance(schema, dict):
                continue
            properties = schema.get("properties", {})
            if not isinstance(properties, dict):
                continue
            status_schema = properties.get("status")
            if isinstance(status_schema, dict) and "enum" not in status_schema:
                errors.append(f"openapi.yaml: schema {schema_name}.status must declare enum values")
            for property_name in ("paymentStatus", "paymentGateStatus", "healthStatus"):
                property_schema = properties.get(property_name)
                if isinstance(property_schema, dict) and "enum" not in property_schema:
                    errors.append(f"openapi.yaml: schema {schema_name}.{property_name} must declare enum values")

        submission_create = schemas.get("SubmissionCreateRequest", {})
        if isinstance(submission_create, dict):
            for item in submission_create.get("allOf", []):
                if isinstance(item, dict) and item.get("$ref") == "#/components/schemas/Submission":
                    errors.append("openapi.yaml: SubmissionCreateRequest must not reuse response schema Submission")

    fixture_path = SPECS / "fixtures" / "demo_project.json"
    fixture = json.loads(fixture_path.read_text(encoding="utf-8"))
    required_fixture_keys = {
        "submission",
        "project",
        "sample",
        "studyPlan",
        "workOrder",
        "rawRecord",
        "report",
        "reportTemplate",
        "changeRequest",
        "sampleRetention",
        "deviceFile",
        "importExportTask",
    }
    missing_fixture_keys = sorted(required_fixture_keys - set(fixture))
    if missing_fixture_keys:
        errors.append(f"demo_project.json: missing top-level keys {missing_fixture_keys}")

    if errors:
        print("SPEC VALIDATION FAILED")
        for error in errors:
            print(f"- {error}")
        return 1

    print("SPEC VALIDATION PASSED")
    print(f"- roles: {len(roles)}")
    print(f"- permission rows: {len(permissions)}")
    print(f"- dictionary rows: {len(dictionaries)}")
    print(f"- OpenAPI paths: {len(openapi.get('paths', {}))}")
    print(f"- OpenAPI schemas: {len(openapi.get('components', {}).get('schemas', {}))}")
    return 0


if __name__ == "__main__":
    raise SystemExit(main())
