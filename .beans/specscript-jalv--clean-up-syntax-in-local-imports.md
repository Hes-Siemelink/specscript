---
# specscript-jalv
title: Clean up ../ syntax in local imports
status: completed
type: task
priority: normal
created_at: 2026-03-29T10:52:30Z
updated_at: 2026-03-29T14:27:59Z
blocked_by:
    - specscript-d32u
---

Investigate the use of '../' paths in local imports (e.g. './../goals:', './..:') in test configs and intra-package references. The spec says 'Only downward paths are allowed — no ../ references' but several migrated configs use ../. Either update the spec to allow ../ or restructure the sample projects to avoid it. Affected files: samples/goals-app/tests/specscript-config.yaml, samples/goals-app/db/specscript-config.yaml, samples/digitalai/platform/cloud-connector/tests/specscript-config.yaml.

## Summary of Changes\n\nConverted goals-app and digitalai samples to packages (added Package info). Replaced all ../ imports with self-package imports:\n- goals-app/tests/: ./../goals → goals-app: goals/...\n- goals-app/db/: ./../goals → goals-app: goals/create\n- digitalai/platform/cloud-connector/tests/: ./.. → digitalai: platform/cloud-connector/...\n\nUpdated spec to remove the no-../ restriction, replaced with guidance to use package imports with self-package discovery.
