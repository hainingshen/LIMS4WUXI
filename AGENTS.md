# Repository Guidelines

## Project Structure & Module Organization

This repository contains LIMS planning, contracts, interface specs, and validation utilities. Core references live in `docs/development/`; start with `README.md`, `00_技术栈与工程约定.md`, `06_数据模型与状态机.md`, `07_API与界面契约.md`, and `specs/openapi.yaml`. Machine-readable specs are under `docs/development/specs/`, including roles, permissions, dictionaries, catalog data, and field mappings. Customer source materials are under `docs/feedback/`. UI prototypes are in `docs/UI/`. Repository checks are in `scripts/checks/`.

The intended application structure is documented, but not yet present here: `backend/` for Java 17 Spring Boot, `web-internal/` and `web-portal/` for React PWAs, and `web-shared/` for shared TypeScript.

## Build, Test, and Development Commands

Install check dependencies before editing specs:

```bash
python -m pip install -r scripts/checks/requirements.txt
```

Validate CSV relationships:

```bash
python scripts/checks/csv-foreign-key.py
```

Detect drift between specs and contracts:

```bash
python scripts/checks/drift-detector.py
```

Run both checks after changing `docs/development/specs/*.csv`, `docs/development/specs/openapi.yaml`, or `docs/development/07_API与界面契约.md`.

## Coding Style & Naming Conventions

Follow `docs/development/00_技术栈与工程约定.md` as the source of truth. Database tables and columns use `snake_case`; Java classes use `PascalCase`; Java fields and API JSON fields use `camelCase`; TypeScript types and React components use `PascalCase`; routes use `kebab-case`; enum values use `UPPER_SNAKE_CASE`.

Do not introduce excluded technologies such as Flowable, Drools, Groovy scripts, Vue, Pinia, or MyBatis. Keep configurable behavior in `system_config`, `business_rule`, dictionaries, or `form_schema`, not hard-coded application logic.

## Testing Guidelines

For the current repository, the required verification is the Python check suite in `scripts/checks/`. When implementation modules are added, align tests with the documented stack: Spring Boot/JUnit tests for `backend/` and Vite/React tests for front-end work. Name tests after the behavior under test and keep fixtures traceable to files in `docs/development/specs/`.

## Commit & Pull Request Guidelines

This checkout has no Git history, so no repository-specific convention can be inferred. Use clear, imperative commit subjects such as `Update permission matrix validation` or `Add sterility report mapping`. Pull requests should describe scope, list changed specs or docs, link the related issue or decision record, and include screenshots for UI prototype changes. Include validation output when specs or API contracts change.

## Agent-Specific Instructions

Before generating application code, read `docs/development/README.md` and the documents it prioritizes for the task. Do not add undefined roles, statuses, API fields, or workflow states. All generated behavior must preserve the documented architecture: metadata-driven forms, hand-written state machines with approval records, RBAC plus data scope, audit logging for writes, and report generation through DOCX templates.
