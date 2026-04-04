# Package System Implementation Plan

This plan implements the "Hierarchical Packages with Dot-Notation" design from
[module-packaging-and-imports-discovery.md](module-packaging-and-imports-discovery.md).

## Scope

The package system adds:

1. Package identity (`Package info` with `name`, `version` in `specscript-config.yaml`)
2. Package search path (`./packages/`, `~/.specscript/packages/`, `SPECSCRIPT_PACKAGE_PATH`)
3. Dot-notation fully-qualified command names (`package.dir.Command`)
4. Package imports (`package:` + `use:` syntax in `specscript-config.yaml`)
5. Export control (`exports` in directory-level `specscript-config.yaml`)
6. Collision detection at import time
7. Removal of old relative file-path imports

## Phase 1: Package Discovery and FQN Resolution

**Goal:** Packages can be installed and commands can be invoked by fully-qualified name. No import syntax yet.

### Spec changes

- **New spec:** `plan/draft-specs/Packages.spec.md` — Package definition, search path, FQN syntax.
- **Update spec:** `Organizing SpecScript files in directories.spec.md` — Remove the "Importing files from another
  directory" section (old file-path imports).

### Code changes

1. **`DirectoryInfo.kt`** — Add `Package info` section support as a new data class (separate from the existing `name`
   which is the display name). Add `exports: List<String>?` field.

2. **New: `PackageRegistry.kt`** — Singleton that discovers and caches packages from the search path.
    - `resolvePackage(name: String): PackageInfo?` — finds a package by name on the search path.
    - `resolveCommand(fqn: String): CommandHandler?` — parses a dot-notation FQN, finds the package, locates the
      directory, finds the script file, returns a `SpecScriptFile`.
    - Search path: `./packages/`, `~/.specscript/packages/`, then `SPECSCRIPT_PACKAGE_PATH` entries.
    - Validates: no nested packages (error if a subdirectory also has `Package info`).
    - Validates: directory name matches `name` in `Package info` (warn if different).

3. **New: `PackageInfo.kt`** — Data class representing a resolved package: root path, name, version.

4. **`FileContext.getCommandHandler()`** — Add step 5: if the command contains a dot, attempt FQN resolution via
   `PackageRegistry.resolveCommand()`.

5. **`canonicalCommandName()`** — Must handle dots. FQN comparison should be case-insensitive on the command portion but
   preserve the directory path. Or: lowercase the entire FQN. Decision: lowercase the entire FQN
   (consistent with current behavior).

### Tests

- Spec tests in the draft spec file (executable examples).
- Unit tests for `PackageRegistry` and FQN parsing.

### Deliverables

- A package on the search path can be invoked by FQN from any script.
- `tests/` directories are excluded from command discovery.
- `hidden: true` directories are excluded from command discovery.
- Error on nested package definitions.
- Warning when directory name doesn't match `name` field.

---

## Phase 2: Package Imports

**Goal:** Commands from packages can be imported into the local namespace via `specscript-config.yaml`.

### Spec changes

- **Update:** `plan/draft-specs/Packages.spec.md` — Add import syntax section with examples.

### Code changes

1. **`DirectoryInfo.kt`** — Change `imports` field from `MutableList<String>` to a new type that supports both the old
   format (for migration) and the new `package:` + `use:` format. Or: replace entirely since old imports are being
   removed.

   New `imports` field type: `List<PackageImport>` where `PackageImport` is:
   ```kotlin
   data class PackageImport(
       val `package`: String,                 // package name
       val use: List<String>                  // qualified paths or "all"
   )
   ```

   Jackson deserialization needs a custom deserializer or a polymorphic type since the YAML is:
   ```yaml
   imports:
     - package: digitalai
       use:
         - platform.cloud-connector.Create agent
         - release.folders
   ```

2. **`FileContext.kt`** — Replace `findImportedCommands()` with `findPackageImports()`:
    - For each `PackageImport`, resolve the package via `PackageRegistry`.
    - For each `use` entry, determine if it's a specific command or a directory.
    - Register the command(s) in the imported commands map.
    - Handle `as` aliases.
    - Detect and report collisions.

3. **Collision detection** — At import registration time, check for duplicates across local commands and all import
   sources. Throw `SpecScriptException` with a clear error message naming both sources.

### Tests

- Import specific commands, use by short name.
- Import a directory, use all commands by short name.
- Import with alias, use by alias.
- Collision detection (two imports with same command name → error).
- `exports` filtering (command not in exports list → not importable).

### Deliverables

- Package imports work in `specscript-config.yaml`.
- Selective imports, directory imports, aliased imports, `use: all`.
- Export control via `exports` field.
- Collision detection with clear error messages.

---

## Phase 3: Remove Old File-Path Imports

**Goal:** Clean break from the old import syntax.

### Spec changes

- **Update:** `Organizing SpecScript files in directories.spec.md` — Remove the imports section entirely. Package
  imports are documented in `Packages.spec.md`.
- **Update:** `specification/language/tests/Directory tests.spec.md` — Update or remove the "Imported helper scripts"
  test that uses old-style imports.

### Code changes

1. **`DirectoryInfo.kt`** — Remove the old `imports: MutableList<String>` field entirely (if not already done in Phase
   2). If the YAML contains a plain string list for `imports`, throw a clear error:
   `"File-path imports are no longer supported. Use package imports instead. See: ..."`

2. **`FileContext.kt`** — Remove `findImportedCommands()` (the old file-path-based version).

### Migration of existing code

- `specification/commands/core/files/tests/specscript-config.yaml` — uses old imports. Convert to package or
  restructure tests to use same-directory discovery.
- `samples/goals-app/db/specscript-config.yaml` — uses `imports: [../goals/create.spec.yaml]`.
- `samples/goals-app/tests/specscript-config.yaml` — uses 7 old-style imports.
- `samples/digitalai/platform/cloud-connector/tests/specscript-config.yaml` — uses 2 old-style imports.
- `specification/language/tests/Directory tests.spec.md` — test creates old-style imports in temp files.

### Deliverables

- Old file-path import syntax produces a clear error.
- All existing code migrated to package imports or restructured.
- No regression in spec tests or sample scripts.

---

## Phase Order and Dependencies

```
Phase 1: Package Discovery + FQN Resolution
    ↓
Phase 2: Package Imports
    ↓
Phase 3: Remove Old Imports
```

Phase 1 is the foundation — nothing else works without package discovery and FQN resolution. Phase 2 adds the
convenience layer. Phase 3 is cleanup that should happen soon after Phase 2 but could be a separate commit.

Phases 1 and 2 can be developed together but should be committed separately for clean history. Phase 3 is a breaking
change and should be its own commit with a clear migration note.

## Files Changed (Summary)

| File                                                 | Phase   | Change                                                        |
|------------------------------------------------------|---------|---------------------------------------------------------------|
| `DirectoryInfo.kt`                                   | 1, 2, 3 | Add package fields, new import type, remove old imports       |
| `FileContext.kt`                                     | 1, 2, 3 | Add FQN resolution, add package imports, remove old imports   |
| `CommandHandler.kt`                                  | 1       | `canonicalCommandName()` handles dots                         |
| New: `PackageRegistry.kt`                            | 1       | Package discovery and FQN resolution                          |
| New: `PackageInfo.kt`                                | 1       | Package data class                                            |
| `Organizing SpecScript files in directories.spec.md` | 3       | Remove imports section                                        |
| `Directory tests.spec.md`                            | 3       | Update import tests                                           |
| New: `Packages.spec.md`                              | 1, 2    | Full package specification                                    |
| `specscript-config.yaml` (various samples/tests)     | 3       | Migrate old imports                                           |

## Risk Assessment

- **FQN parsing ambiguity.** A command like `some.thing` — is it the FQN `some.thing` (package `some`, command
  `thing`) or a command literally named `some.thing`? Resolution: dots in command names are not allowed in the current
  system (no existing command uses dots). Treat any command containing a dot as an FQN attempt. If resolution fails,
  report "Unknown package or command: some.thing".

- **Breaking change for file-path imports.** Existing users must migrate. Mitigation: clear error message with migration
  instructions. The user base is small at this stage.

- **Search path resolution at runtime.** The `./packages/` path is relative to what? The working directory where
  `spec` was invoked, or the script's directory? Decision: relative to the working directory (where `spec` is invoked
  from). This matches how most tools work (`node_modules/` is relative to project root, not to individual source files).

- **Performance.** Package discovery scans directories. For a typical setup with a few packages, this is negligible.
  Cache aggressively in `PackageRegistry` (already planned).
