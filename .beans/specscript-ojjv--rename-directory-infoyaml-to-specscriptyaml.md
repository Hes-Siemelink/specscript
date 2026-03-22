---
# specscript-ojjv
title: Rename .directory-info.yaml to specscript-config.yaml
status: completed
type: feature
priority: normal
created_at: 2026-03-22T08:18:09Z
updated_at: 2026-03-22T08:32:20Z
---

Rename the directory config file from .directory-info.yaml to specscript.yaml. It's actively authored project config, not hidden infrastructure. Should be visible in directory listings.\n\nTodo:\n- [x] Update DirectoryInfo.kt to load specscript.yaml (fallback to .directory-info.yaml)\n- [x] Rename all 29 .directory-info.yaml files to specscript.yaml\n- [x] Update specification docs (3 spec.md files + 1 test yaml)\n- [x] Update src/tests/resources sample\n- [x] Update AGENTS.md sensitive areas reference (no refs found)\n- [x] Update agent skills references\n- [ ] Run all tests\n- [ ] Update Kotlin implementation (DirectoryInfo.kt and related)\n- [ ] Update all .directory-info.yaml files in samples/\n- [ ] Update all .directory-info.yaml files in specification/ test fixtures\n- [ ] Update AGENTS.md references\n- [ ] Support both names during transition (specscript.yaml preferred, .directory-info.yaml as fallback)\n- [ ] Run all tests

## Summary of Changes\n\nRenamed .directory-info.yaml to specscript.yaml across the entire codebase:\n\n- Updated DirectoryInfo.kt to prefer specscript.yaml with fallback to .directory-info.yaml for backward compatibility\n- Renamed all 29+ .directory-info.yaml files to specscript.yaml (samples, specification, src/tests)\n- Updated 3 specification docs and 1 test file to reference the new name\n- Updated specscript-tests agent skill\n- All tests pass (BUILD SUCCESSFUL)

\n## Final Summary\n\nChanged name from specscript.yaml to specscript-config.yaml after review showed specscript.yaml looked like the main script entry point.\n\n- DirectoryInfo.kt: prefers specscript-config.yaml, falls back to .directory-info.yaml\n- All 29+ files renamed to specscript-config.yaml\n- All spec docs updated with backticked filename references\n- All tests pass (BUILD SUCCESSFUL)
