# Connection System Improvement Plan

## Problem Statement

The current `Connect to:` / `.directory-info.yaml` / `credentials.yaml` connection system works at the top and bottom
levels but is brittle to maintain in between:

1. **Connection resolution bug**: When script A calls script B via `Run script:`, B's `Connect to: X` resolves from B's
   `.directory-info.yaml`, not A's. Tests cannot override production connections.
   (See: `plan/connect-to-resolution-bug.md`)

2. **Path resolution confusion**: Inline connection definitions (e.g., `SQLite defaults: file: db/goals.db`) resolve
   relative to the working directory, not the `.directory-info.yaml` location. Scripts only work from one specific
   working directory.

3. **Scattered configuration**: Every subdirectory that needs a connection must declare it in its own
   `.directory-info.yaml`. This leads to 18+ nearly-identical files all pointing to the same connect script.

4. **No environment variable integration**: CI/CD, Docker, and Kubernetes all use environment variables for
   configuration. SpecScript has no way to consume them, forcing custom credential file management.

## Design Decisions

- **`${env.VAR_NAME}`** namespace for environment variables (namespaced to avoid collision with script variables)
- **`${SCRIPT_DIR}`** as a regular SpecScript variable (like `${SCRIPT_TEMP_DIR}`)
- **`env:` property on Input schema** for structured env var resolution (not via `default:`)
- **Caller wins** semantics for connection inheritance
- **No project root concept** (out of scope, may come later)
- **No breaking changes** in any phase

---

## Phase 1: `${SCRIPT_DIR}` Variable

**Goal**: Make `${SCRIPT_DIR}` available as a first-class SpecScript variable everywhere, not just in Shell.

**Draft spec**: `plan/draft-specs/SCRIPT_DIR variable.spec.md`

### Before / After

**Before** -- `SCRIPT_DIR` is only available inside Shell commands:

```yaml
# Works
Shell: cat $SCRIPT_DIR/data.json

# Does NOT work -- SCRIPT_DIR is not a SpecScript variable
Read file: ${SCRIPT_DIR}/data.json
```

SQLite connections in `.directory-info.yaml` are fragile because paths are relative to the working directory:

```yaml
# goals-app/db/.directory-info.yaml
connections:
  Goal DB:
    SQLite defaults:
      file: db/goals.db    # Only works when running `spec` from goals-app/
```

**After** -- `SCRIPT_DIR` is a first-class variable:

```yaml
# Works everywhere
Read file: ${SCRIPT_DIR}/data.json

# Connection scripts can use it for portable path resolution
SQLite defaults:
  file: ${SCRIPT_DIR}/goals.db
```

### Changes

- `ScriptContext.kt` -- add `const val SCRIPT_DIR_VARIABLE = "SCRIPT_DIR"`
- `FileContext` -- set `SCRIPT_DIR` on `variables` during initialization
  (`scriptDir.toAbsolutePath().toString()`)
- Precedent: `SCRIPT_TEMP_DIR` already works this way

---

## Phase 2: `${env}` Namespace

**Goal**: Expose OS environment variables as `${env.VAR_NAME}` in SpecScript.

**Draft spec**: `plan/draft-specs/Environment variables.spec.md`

### Before / After

**Before** -- no way to read environment variables in SpecScript:

```yaml
# Does NOT work
Print: ${HOME}

# Only way to get an env var is through Shell
Shell: echo $HOME
As: ${home}
```

CI/CD pipelines cannot inject credentials into SpecScript scripts.

**After** -- environment variables are accessible via the `${env}` namespace:

```yaml
# Read an environment variable
Print: Home is ${env.HOME}

# Use in HTTP requests
Http request defaults:
  url: ${env.API_URL}
  headers:
    Authorization: Token ${env.API_TOKEN}
```

### Changes

- Create an `ObjectNode` backed by `System.getenv()` and inject it as `env` into `context.variables`
- `Variables.kt` already supports dot-path resolution, so `${env.HOME}` resolves naturally
- `${env.UNSET_VAR}` should produce `MissingNode` gracefully, not throw

---

## Phase 3: `env:` Property on Input Schema

**Goal**: Input parameters can declare an environment variable source, creating a clean resolution chain.

**Draft spec**: `plan/draft-specs/Input schema env property.spec.md`

### Before / After

**Before** -- connection scripts hardcode interactive flows; no way to inject from CI:

```yaml
# samples/digitalai/platform/login/connect.spec.yaml (simplified)
Get credentials: Digital.ai Platform
As: ${endpoint}

If:
  empty: ${endpoint}
  then:
    Create new account: { }        # Interactive prompt -- breaks in CI
    As: ${endpoint}

Http request defaults:
  url: ${endpoint.url}
  headers:
    Authorization: Bearer ${endpoint.token}
```

To run this in CI, you must pre-populate `~/.specscript/credentials.yaml` -- an awkward step in a Docker container.

**After** -- scripts declare env var sources alongside defaults:

```yaml
# connect.spec.yaml (simplified, CI-friendly)
Input schema:
  type: object
  properties:
    url:
      description: Platform API URL
      env: DAI_PLATFORM_URL
      default: https://api.us.digitalai.cloud
    token:
      description: Bearer token
      env: DAI_PLATFORM_TOKEN
    username:
      description: Username
      env: DAI_PLATFORM_USERNAME
    password:
      description: Password
      env: DAI_PLATFORM_PASSWORD
      secret: true

Http request defaults:
  url: ${url}
  headers:
    Authorization: Token ${token}
```

In CI, just set env vars:

```bash
DAI_PLATFORM_URL=https://api.staging.digitalai.cloud DAI_PLATFORM_TOKEN=xxx spec deploy.spec.yaml
```

In interactive mode, missing env vars fall through to the prompt as before.

### Resolution Order (updated)

1. Already exists (passed as input) -> use it
2. Skip if condition invalid
3. **Env var set (via `env:` property) -> use env var value** (NEW)
4. Has default -> use default
5. Has recorded answer -> use recorded answer
6. Interactive -> prompt
7. Error

### Changes

- `ParameterData` -- add `env: String?` property
- `ParameterData.schema.yaml` -- add `env` as optional string property
- `InputParameters.populateInputVariables()` -- insert env var check at step 3
- Update `Input schema.spec.md` to document the `env:` property

---

## Phase 4: Connection Inheritance (Caller Wins)

**Goal**: When script A calls script B, and both define a connection named X, A's definition wins.

**Draft spec**: `plan/draft-specs/Connection inheritance.spec.md`

### Before / After

**Before** -- test directory cannot override a production connection:

```
goals-app/
  db/
    .directory-info.yaml       # Goal DB -> file: db/goals.db (production)
    create-db.spec.yaml        # Contains: Connect to: Goal DB
  tests/
    .directory-info.yaml       # Goal DB -> file: db/test-goals.db (test)
    create-tests.spec.yaml     # Calls: Run script: resource: ../db/create-db.spec.yaml
```

When `create-tests.spec.yaml` calls `create-db.spec.yaml`, the `Connect to: Goal DB` inside resolves from
`db/.directory-info.yaml` (production), ignoring the test override. This is the bug in
`plan/connect-to-resolution-bug.md`.

**After** -- caller's connections override callee's:

```yaml
# tests/create-tests.spec.yaml
# Goal DB here points to test-goals.db (from tests/.directory-info.yaml)

Create test db: {}
# create-db.spec.yaml does "Connect to: Goal DB" which now resolves
# from the CALLER's .directory-info.yaml -> test-goals.db
```

The same Digital.ai pattern works for HTTP:

```yaml
# tests/.directory-info.yaml
connections:
  Digital.ai Platform:
    Http request defaults:
      url: http://localhost:25101     # Mock server for testing

# Tests call production scripts, but connections resolve to the mock
```

### Changes

- `ConnectTo.kt` -- before resolving from `context.info.connections`, check `session["connect-to.overrides"]`
- When creating a child `FileContext` (via `Run script:` or file commands), populate
  `session["connect-to.overrides"]` from the parent's `info.connections`
- Nested calls work because session is shared

---

## Implementation Order

```
Phase 1: ${SCRIPT_DIR}        -- independent
Phase 2: ${env} namespace     -- independent
Phase 3: env: on Input schema -- depends on Phase 2
Phase 4: Connection inherit.  -- independent
```

Phases 1, 2, and 4 can be done in parallel. Phase 3 depends on Phase 2.

## Migration Path

- **No breaking changes** in any phase
- Existing `.directory-info.yaml` connections continue to work unchanged
- Existing `credentials.yaml` continues to work unchanged
- Connect scripts can gradually adopt `env:` properties for CI compatibility
- Scattered `.directory-info.yaml` files can be simplified once connection inheritance (Phase 4) is in place
  (child directories no longer need to repeat parent connection definitions)
