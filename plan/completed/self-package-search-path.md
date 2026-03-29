# Self-package in import search path

## Problem

When a script lives inside a package, it currently cannot import from its own package (or sibling packages in the same
library directory) without explicitly specifying `--package-path`. This forces `../` local imports for what should be
simple package imports.

Example: a test script at `myapp/goals/tests/test.spec.yaml` wants to use a command from `myapp/goals/`. Today it must
use `../` local imports. With self-package support, it would use a regular package import.

## Proposal

When resolving package imports, automatically include the **parent directory of the enclosing package** in the search
path. The enclosing package is found by walking up from the script's directory looking for a `Package info` declaration.

### Algorithm

1. From the script's directory, walk up the directory tree.
2. At each level, check if the directory has `Package info` in its `specscript-config.yaml`.
3. If found, add that directory's **parent** to the search path (at lowest priority, after all explicit paths).
4. Stop at the first package found (packages cannot nest, so there's at most one).

### Search path with self-package

| Priority | Location                                            |
|----------|-----------------------------------------------------|
| 1        | Parent of enclosing package (auto-discovered)       |
| 2        | `-p` / `--package-path` CLI argument                |
| 3        | `SPECSCRIPT_PACKAGE_PATH` entries (colon-separated) |
| 4        | `~/.specscript/packages/`                           |

### Example

```
lib/
  greetings/              <- Package (has Package info)
    hello.spec.yaml
    tests/
      test.spec.yaml      <- Script here
  utils/                  <- Sibling package
```

`test.spec.yaml` is inside the `greetings` package. Its enclosing package dir is `lib/greetings/`, so `lib/` is added to
the search path. The test can now `import greetings: hello` and `import utils: ...` without `--package-path`.

### Design decisions

- The auto-discovered path has **highest priority** — the enclosing library is the script's home context.
- The search is bounded: stop at the first `Package info` found (no nested packages).
- This is a `PackageRegistry` concern — the registry needs to know the script's location to discover the enclosing
  package. Currently `searchPath()` is context-free; it will need a `scriptDir` parameter or a second mutable field.
- Simplest implementation: add an `autoPackagePath` field to `PackageRegistry` alongside `packagePath`. Set it when
  creating `FileContext` by walking up from `scriptDir`.

### Implementation approach

**Option A: Set `autoPackagePath` in `FileContext` init** — when constructing a `FileContext`, walk up from `scriptDir`
to find the enclosing package and set `PackageRegistry.autoPackagePath`. Simple, but `PackageRegistry` is a global
singleton, and this couples it to `FileContext`.

**Option B: Pass `scriptDir` to `searchPath()`** — make `findPackage` accept an optional `scriptDir` and derive the
auto-path on each call. More functional, but adds a parameter threading concern.

**Recommendation: Option A** — matches the existing pattern where `PackageRegistry.packagePath` is set globally in
`SpecScriptCli`. Add `autoPackagePath` as another mutable field.

Refinement: the walk-up logic should be a utility on `PackageRegistry` itself (e.g.,
`findEnclosingPackageLibrary(startDir: Path): Path?`).
