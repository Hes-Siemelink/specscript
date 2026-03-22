---
# specscript-kksg
title: Upward directory search for connections
status: completed
type: feature
priority: high
created_at: 2026-03-22T08:50:16Z
updated_at: 2026-03-22T09:06:22Z
---

Implement upward directory search for connection definitions in specscript-config.yaml. When Connect to is executed, search parent directories until a matching connection is found. This is the only real change needed — env var substitution already works out of the box in inline connection blocks.

## Todo
- [x] Update proposal to remove Change 2 (env vars already work)
- [x] Write the specification (update Connect to.spec.md and directory org spec)
- [x] Write test cases for upward search (tests/ file)
- [x] Implement upward directory search in DirectoryInfo.kt
- [x] Update ConnectTo.kt to use upward search
- [x] Run specificationTest to verify

## Samples cleanup

Now that upward search works, consolidate duplicate specscript-config.yaml files in samples:

### goals-app
- `db/` and `goals/` have identical connection: `Goal DB: SQLite defaults: file: db/goals.db`
- `tests/` has a legitimate override: `file: db/test-goals.db`
- Move shared connection to `goals-app/specscript-config.yaml`, keep tests override

### digitalai
- `release/` defines `Digital.ai Release: login/connect.spec.yaml`
- 4 subdirs (`folders/`, `export/`, `live-deployments/`, `cloud-connector/`) repeat it with `../login/connect.spec.yaml`
- `platform/` has 4 subdirs repeating `Digital.ai Platform: ../login/connect.spec.yaml`
- Move connections up to `release/` and `platform/` level

### Other samples (spotify, ticket-db, etc.)
- Already at single-level, no changes needed

- [x] Consolidate goals-app configs
- [x] Consolidate digitalai/release configs
- [x] Consolidate digitalai/platform configs
- [x] Verify samples still work

## Summary of Changes

### Samples consolidated using upward directory search:

**goals-app:** Moved shared Goal DB connection from db/ and goals/ configs to goals-app/specscript-config.yaml. Removed goals/specscript-config.yaml entirely. tests/ keeps its override (test-goals.db).

**digitalai/release:** Connection was already at release/ level. Removed duplicate connection from folders/, export/, live-deployments/, cloud-connector/. Deleted live-deployments/specscript-config.yaml (was connection-only). cloud-connector/ keeps its Digital.ai Platform connection (unique to that dir).

**digitalai/platform:** Added Digital.ai Platform connection to platform/specscript-config.yaml. Removed duplicate from accounts/, analytics/, cloud-connector/, oidc/.

**Other samples** (spotify, ticket-db, etc.): Already single-level, no changes needed.
