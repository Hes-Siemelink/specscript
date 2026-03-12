# Connection Profiles Plan

## Overview

Connection profiles are a future evolution of the current `credentials.yaml` system. They consolidate connection
configuration (URLs, credentials, connection scripts) into a single, manageable structure with support for multiple
named profiles per target and easy profile switching.

This plan depends on Phase 1-4 of `plan/connection-system-improvement.md` being completed first. Profiles are
the natural next step after environment variables and connection inheritance are in place.

## Current State

Today, connection configuration is spread across three systems:

### 1. `.directory-info.yaml` -- connection routing

Maps symbolic names to connection scripts or inline configuration. Repeated in every subdirectory.

```yaml
# samples/digitalai/release/export/.directory-info.yaml
connections:
  Digital.ai Release: ../login/connect.spec.yaml
```

```yaml
# samples/digitalai/release/folders/.directory-info.yaml
connections:
  Digital.ai Release: ../login/connect.spec.yaml     # Same thing, repeated
```

```yaml
# samples/digitalai/release/live-deployments/.directory-info.yaml
connections:
  Digital.ai Release: ../login/connect.spec.yaml     # And again
```

### 2. `~/.specscript/credentials.yaml` -- stored credentials

A flat file storing credentials per target, with a default selector:

```yaml
Digital.ai Platform:
  default: staging-admin
  credentials:
    - name: staging-admin
      url: https://api.staging.digitalai.cloud
      id: my-tenant
      username: admin
      password: secret
    - name: production-admin
      url: https://api.us.digitalai.cloud
      id: prod-tenant
      username: admin
      password: secret
```

### 3. Connection scripts -- glue logic

Scripts that read credentials, negotiate tokens, and set `Http request defaults`:

```yaml
# connect.spec.yaml (simplified)
Get credentials: Digital.ai Platform
As: ${endpoint}

If:
  empty: ${endpoint}
  then:
    Create new account: { }
    As: ${endpoint}

Http request defaults:
  url: ${endpoint.url}
  headers:
    Authorization: Bearer ${endpoint.token}
```

### Problems with current state

1. **Three systems to maintain**: Changing a connection requires touching `.directory-info.yaml`, `credentials.yaml`,
   and potentially the connection script
2. **No profile switching**: To switch between staging and production, you must change the default credential in
   `credentials.yaml` (or modify `.directory-info.yaml`)
3. **No CLI support**: No `spec profile` commands to list, switch, or manage profiles
4. **Credential isolation**: `credentials.yaml` stores raw secrets in a custom format. No integration with env vars,
   secret managers, or OS keychains
5. **Scattered `.directory-info.yaml`**: Every subdirectory repeats the same connection definition (mitigated by
   Phase 4 connection inheritance, but not eliminated)

---

## Proposed Design: `~/.specscript/profiles.yaml`

### Profile file format

Profiles consolidate credentials.yaml and connection routing into one structure:

```yaml
# ~/.specscript/profiles.yaml
Digital.ai Platform:
  default: staging
  profiles:
    staging:
      url: https://api.staging.digitalai.cloud
      id: my-tenant
      username: admin
      password: secret
      connect: login/connect.spec.yaml
    production:
      url: https://api.us.digitalai.cloud
      id: prod-tenant
      token: xxxxxxxxxxx
      connect: login/connect.spec.yaml
    local:
      url: http://localhost:25101

Digital.ai Release:
  default: staging
  profiles:
    staging:
      url: https://staging-release.example.com
      username: admin
      password: secret
    local:
      url: http://localhost:5516
      username: admin
      password: admin

Spotify:
  profiles:
    default:
      client_id: abc123
      client_secret: xyz789
      connect: connect.spec.yaml

Goal DB:
  default: development
  profiles:
    development:
      file: db/goals.db
    test:
      file: db/test-goals.db
```

### Key differences from `credentials.yaml`

| Aspect | credentials.yaml | profiles.yaml |
|--------|-----------------|---------------|
| Structure | `target > credentials[] > {name, ...}` | `target > profiles > name > {...}` |
| Selection | `default` field on target | `default` field on target + CLI override |
| Content | Only credentials | Credentials + URLs + connection script reference |
| Naming | `name` field inside credential object | Profile name is the map key |

### Profile names are user-defined

Profile names are arbitrary strings. There are no hard-coded names like "staging" or "production". Users create
whatever names make sense for their setup. The only special concept is `default`, which marks which profile is
active when no override is specified.

---

## Profile Selection

### Default profile

Each target has an optional `default` field that names the active profile:

```yaml
Digital.ai Platform:
  default: staging     # "staging" profile is active unless overridden
  profiles:
    staging: { ... }
    production: { ... }
```

If no `default` is set and there is exactly one profile, that profile is used. If there are multiple profiles and no
default, the user is prompted in interactive mode (or gets an error in non-interactive mode).

### CLI override

```bash
# Override the profile for all targets
spec --profile production deploy.spec.yaml

# Override for a specific target (future extension)
spec --profile "Digital.ai Platform=production" deploy.spec.yaml
```

### Environment variable override

```bash
# Override default profile for all targets
SPECSCRIPT_PROFILE=production spec deploy.spec.yaml

# Override for a specific target
SPECSCRIPT_DIGITAL_AI_PLATFORM_PROFILE=production spec deploy.spec.yaml
```

The env var name is derived from the target name: uppercase, spaces and dots replaced with underscores,
suffixed with `_PROFILE`.

### In-script override

```yaml
Set profile: production

Connect to: Digital.ai Platform
# Uses the "production" profile
```

### Resolution order for profile selection

1. `Set profile:` command in the script
2. `--profile` CLI flag
3. `SPECSCRIPT_<TARGET>_PROFILE` env var
4. `SPECSCRIPT_PROFILE` env var
5. `default` field in profiles.yaml
6. Only profile (if exactly one exists)
7. Interactive prompt / error

---

## How Profiles Interact with `Connect to:`

### Before profiles (current)

```yaml
# .directory-info.yaml
connections:
  Digital.ai Platform: ../login/connect.spec.yaml
```

```yaml
# Script
Connect to: Digital.ai Platform
# -> runs connect.spec.yaml
#    -> Get credentials: Digital.ai Platform
#    -> resolves from credentials.yaml
#    -> sets Http request defaults
```

### After profiles

```yaml
# Script (unchanged!)
Connect to: Digital.ai Platform
```

Resolution:

1. Check `session["connect-to.overrides"]` (connection inheritance from caller)
2. Check `context.info.connections` (`.directory-info.yaml`) -- still works if present
3. **Check `profiles.yaml` for the target name** (NEW)
4. If profile has a `connect:` script -> run it with profile data as input
5. If profile has `url:` but no `connect:` -> auto-configure `Http request defaults`
6. Error if no connection found

### Auto-configuration (no connect script needed)

For simple connections (URL + basic auth or token), the profile data can directly configure `Http request defaults`
without a connection script:

```yaml
# profiles.yaml
My API:
  profiles:
    default:
      url: https://api.example.com
      username: admin
      password: secret
```

```yaml
# Script
Connect to: My API
# Auto-configures Http request defaults with url, username, password

GET: /items
```

This eliminates the need for trivial connection scripts that just set `Http request defaults`.

### Connection scripts receive profile data

For complex connections (OAuth, token refresh), the connection script receives the active profile's data as input:

```yaml
# profiles.yaml
Digital.ai Platform:
  default: staging
  profiles:
    staging:
      url: https://api.staging.digitalai.cloud
      username: admin
      password: secret
      connect: login/connect.spec.yaml
```

```yaml
# login/connect.spec.yaml receives ${input.url}, ${input.username}, ${input.password}
# It can do OAuth, token negotiation, etc.

POST:
  url: ${input.url}/auth/token
  body:
    username: ${input.username}
    password: ${input.password}
As: ${token_response}

Http request defaults:
  url: ${input.url}
  headers:
    Authorization: Bearer ${token_response.access_token}
```

---

## `.directory-info.yaml` Evolution

### Simplified

With profiles and connection inheritance (Phase 4), `.directory-info.yaml` connections become optional. Most
directories no longer need them:

**Before** (18 `.directory-info.yaml` files with connections):

```yaml
# samples/digitalai/release/export/.directory-info.yaml
connections:
  Digital.ai Release: ../login/connect.spec.yaml

# samples/digitalai/release/folders/.directory-info.yaml
connections:
  Digital.ai Release: ../login/connect.spec.yaml

# samples/digitalai/release/live-deployments/.directory-info.yaml
connections:
  Digital.ai Release: ../login/connect.spec.yaml
```

**After** (one file at the top level, or zero if using profiles):

```yaml
# samples/digitalai/release/.directory-info.yaml
connections:
  Digital.ai Release: login/connect.spec.yaml
# Child directories inherit this via Phase 4 connection inheritance
```

Or with profiles, no `.directory-info.yaml` connections at all -- the profile provides the connection script path.

### Backward compatibility

`.directory-info.yaml` connections continue to work. They are checked before profiles in the resolution chain.
No deprecation planned initially.

---

## CLI Commands

### Profile management

```bash
# List all profiles
spec profile list

# Show active profile for a target
spec profile show "Digital.ai Platform"

# Set the default profile
spec profile set "Digital.ai Platform" production

# Add a new profile interactively
spec profile add "Digital.ai Platform" local

# Remove a profile
spec profile remove "Digital.ai Platform" local
```

These CLI commands replace the current credential commands (`Create credentials`, `Delete credentials`,
`Set default credentials`, `Get credentials`, `Get all credentials`).

### Migration command

```bash
# Migrate credentials.yaml to profiles.yaml
spec profile migrate
```

This reads `~/.specscript/credentials.yaml` and converts it to `~/.specscript/profiles.yaml` format.

---

## Migration Path

### Phase A: Introduce profiles.yaml (backward compatible)

1. Add `profiles.yaml` file support alongside `credentials.yaml`
2. `Connect to:` checks profiles.yaml as a fallback after `.directory-info.yaml`
3. `Get credentials:` also checks profiles.yaml for backward compatibility
4. Add `spec profile` CLI commands
5. Add `spec profile migrate` command

### Phase B: Simplify connection scripts

1. Connection scripts that just do `Get credentials` + `Http request defaults` can be replaced by auto-configuration
2. Complex scripts (OAuth) receive profile data as `${input}` instead of calling `Get credentials`
3. `.directory-info.yaml` connections that just point to trivial connect scripts can be removed

### Phase C: Deprecation (optional, far future)

1. Deprecate `credentials.yaml` in favor of `profiles.yaml`
2. Deprecate credential commands in favor of `spec profile` CLI
3. `.directory-info.yaml` connections remain supported but documented as optional

---

## Before / After: Full Example

### Before (current state, Digital.ai Platform)

Files involved:
- `samples/digitalai/platform/accounts/.directory-info.yaml` -- connection routing
- `samples/digitalai/platform/login/connect.spec.yaml` -- connection script (45 lines)
- `samples/digitalai/platform/login/create-new-account.spec.yaml` -- interactive setup (53 lines)
- `~/.specscript/credentials.yaml` -- stored credentials
- Plus 4 more `.directory-info.yaml` files in sibling directories repeating the same connection

```yaml
# .directory-info.yaml (repeated in 5 directories)
connections:
  Digital.ai Platform: ../login/connect.spec.yaml
```

```yaml
# connect.spec.yaml (45 lines, handles missing credentials, token negotiation)
Get credentials: Digital.ai Platform
As: ${endpoint}
If:
  empty: ${endpoint}
  then:
    Create new account: { }
    As: ${endpoint}
Http request defaults:
  url: ${endpoint.url}
  headers:
    Authorization: Bearer ${output.access_token}
```

### After (with profiles + connection inheritance)

Files involved:
- `~/.specscript/profiles.yaml` -- all connection config in one place
- `samples/digitalai/platform/login/connect.spec.yaml` -- simplified connection script
- `samples/digitalai/platform/.directory-info.yaml` -- ONE file, inherited by children

```yaml
# ~/.specscript/profiles.yaml
Digital.ai Platform:
  default: staging
  profiles:
    staging:
      url: https://api.staging.digitalai.cloud
      id: my-tenant
      username: admin
      password: secret
      connect: login/connect.spec.yaml
    local:
      url: http://localhost:25101
```

```yaml
# connect.spec.yaml (simplified -- receives profile data as input)
Input schema:
  type: object
  properties:
    url:
      description: Platform URL
      env: DAI_PLATFORM_URL
    token:
      description: Bearer token
      env: DAI_PLATFORM_TOKEN
    username:
      env: DAI_PLATFORM_USERNAME
    password:
      env: DAI_PLATFORM_PASSWORD
      secret: true

Http request defaults:
  url: ${url}
  headers:
    Authorization: Bearer ${token}
```

```yaml
# ONE .directory-info.yaml at the platform level (children inherit)
# Or no .directory-info.yaml at all if using profiles
```

Running:

```bash
# Interactive (prompted for profile selection if multiple exist)
spec samples/digitalai/platform/accounts/list-accounts.spec.yaml

# Specific profile
spec --profile local samples/digitalai/platform/accounts/list-accounts.spec.yaml

# CI (all from env vars, no profiles file needed)
DAI_PLATFORM_URL=https://api.staging.digitalai.cloud \
DAI_PLATFORM_TOKEN=xxx \
spec samples/digitalai/platform/accounts/list-accounts.spec.yaml
```
