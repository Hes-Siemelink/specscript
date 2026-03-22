# Connection System Redesign — Proposals

## The Problem

Connecting to a service today requires too much ceremony:

- Every subdirectory needs its own `specscript-config.yaml` with a connection definition or a relative path to a login script
- A basic username/password HTTP connection requires 3 files (connect, create-credentials, select-credentials) and 5+ commands
- Credentials are stored in `~/.specscript/credentials.yaml` with no scoping per project or environment
- There's no way to use environment variables directly for credentials
- Switching between environments (dev/staging/prod) means manual credential selection

The pain has two different shapes depending on the protocol:

**HTTP (Digital.ai Release):** 6 directories, each with a `specscript-config.yaml` pointing to `../login/connect.spec.yaml`, plus 3 login scripts totaling ~70 lines — all to say "use basic auth with this URL."

**SQLite (goals-app):** 3 directories, each with an identical inline connection block. Less ceremony, but still repetitive:

```yaml
connections:
  Goal DB:
    SQLite defaults:
      file: db/goals.db
```

### What Connect to actually is

`Connect to` is not HTTP-specific. It's a **named alias for a block of setup commands**. The connection definition can be:

- A path to a script file (runs the script)
- An inline YAML block (executes the commands directly)

This already works for any protocol — the goals-app uses it for SQLite. The core abstraction is sound. The problems are:

1. **Repetition** — no upward directory search, so every subdirectory repeats the definition
2. **HTTP credential ceremony** — basic auth needs too many moving parts
3. **No env var support** — can't parameterize connection definitions with environment variables

---

## Proposal A: Fix What Hurts (Upward Search + Env Vars) ← Selected

**Core idea:** Keep the current model but fix the two real problems: repetition and credentials.

### Change 1: Upward directory search for connections

When `Connect to: Goal DB` is executed, search `specscript-config.yaml` files upward through parent directories until a matching connection is found. Same as how `.gitignore` works.

**Before** (3 files with identical content):
```
goals-app/db/specscript-config.yaml          → Goal DB: SQLite defaults: ...
goals-app/goals/specscript-config.yaml       → Goal DB: SQLite defaults: ...
goals-app/tests/specscript-config.yaml       → Goal DB: SQLite defaults: ...
```

**After** (1 file):
```
goals-app/specscript-config.yaml             → Goal DB: SQLite defaults: ...
```

Subdirectories can still override (closest wins), so `tests/` can point to a test database.

### Change 2: Env var substitution in connection definitions

Allow `${}` variable references in `specscript-config.yaml` connection blocks:

```yaml
connections:
  Digital.ai Release:
    Http request defaults:
      url: ${RELEASE_URL}
      username: ${RELEASE_USER}
      password: ${RELEASE_PASS}
```

This means basic HTTP auth no longer needs a login script — the inline form with env vars is sufficient.

### What the user writes

Scripts are unchanged:

```yaml
Connect to: Digital.ai Release
GET: /api/v1/releases
```

```yaml
Connect to: Goal DB
SQLite:
  query: SELECT * FROM goal;
```

### What changes in the Digital.ai Release sample

**Before:** 6 `specscript-config.yaml` files + 3 login scripts

**After:** 1 `specscript-config.yaml` at the project root:

```yaml
connections:
  Digital.ai Release:
    Http request defaults:
      url: ${RELEASE_URL}
      username: ${RELEASE_USER}
      password: ${RELEASE_PASS}
```

For complex auth (OAuth, session tokens), login scripts still work:

```yaml
connections:
  Spotify:
    script: login/spotify-oauth.spec.yaml
```

### Tradeoffs

- (+) Minimal change — fixes the actual pain without redesigning
- (+) Works for all protocols (HTTP, SQLite, anything future)
- (+) Familiar pattern (upward search is well-understood)
- (-) Still requires a `specscript-config.yaml` file somewhere
- (-) Env var names are up to the user — no convention or discovery
- (-) Doesn't address the credential store complexity (but makes it less necessary)

---

## Proposal B: Connection Profiles (parked)

**Core idea:** Connections are defined once in a project-level file, with named profiles for different environments. Think `~/.kube/config` but simpler and per-project. May revisit if multi-environment switching becomes a real need.
