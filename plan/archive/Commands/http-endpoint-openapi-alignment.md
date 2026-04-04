# HTTP Endpoint — OpenAPI Alignment

## Problem Statement

The Http server endpoint definitions currently support only `output` and `script` as handler properties. They lack
metadata that would make endpoints self-describing: descriptions, parameter schemas, response definitions. Meanwhile, the
MCP server/tool ecosystem already supports `description` and `inputSchema` — both aligned with the MCP protocol's own
JSON Schema usage and derived from the `Input schema` command. The Http server should follow a similar path, borrowing
from OpenAPI where it makes sense.

## Current State

### Http server endpoint structure (today)

```yaml
Http server:
  name: my-server
  port: 3000
  endpoints:
    /users/{id}:
      get:
        output: ...        # static response
        script: ...        # inline script or file reference
      post:
        script: ...
```

The `MethodHandler` schema allows only two properties: `output` and `script`. The `EndpointData` (path item) is a
bare map of method names to handlers — no metadata at all.

Path parameters already work at runtime (`:id` and `{id}` syntax both normalize to Ktor's `{id}`), but there is no
schema-level declaration of them.

### MCP tool structure (today)

```yaml
Mcp tool:
  greet:
    description: Generate a greeting     # required
    inputSchema:                          # optional, JSON Schema subset
      type: object
      properties:
        name:
          type: string
          description: Name to greet
      required: [name]
    script: greet.spec.yaml              # or inline script
    output: ...                          # or static output
```

Key observations:
- `description` is required on every tool
- `inputSchema` uses the same JSON Schema subset as `Input schema` (type, properties, required, description, default,
  enum)
- When `script` references a file with `Input schema`, the `inputSchema` is derived automatically
- `HandlerInfo` interface is shared between MCP tools and Http endpoints (`output` + `script`)
- MCP tools do NOT currently derive `description` from the referenced script's `Script info` — it's always explicit

### OpenAPI structure (reference)

OpenAPI 3.2 defines the following hierarchy for an endpoint:

```
Paths Object
  /{path}  →  Path Item Object
    summary, description
    parameters (shared across methods)
    get/post/put/patch/delete  →  Operation Object
      summary, description, operationId
      parameters (operation-specific)
      requestBody  →  Request Body Object
        description, required
        content  →  Media Type Object  →  schema (JSON Schema)
      responses  →  map of status codes → Response Object
        description
        content  →  Media Type Object  →  schema (JSON Schema)
```

## Gap Analysis

| Concept | OpenAPI | MCP Tool | Http Server (today) | Notes |
|---|---|---|---|---|
| Operation description | `summary` + `description` | `description` (required) | none | Most important gap |
| Input schema (body) | `requestBody.content.*.schema` | `inputSchema` | none | MCP uses JSON Schema subset |
| Input schema (query/path params) | `parameters[].schema` | n/a (flat inputSchema) | none | OpenAPI separates by location |
| Response schema | `responses.*.content.*.schema` | n/a | none | Low priority for SpecScript |
| Response status codes | `responses` map | n/a | implicit 200 | Low priority |
| Operation ID | `operationId` | tool name (map key) | n/a | Path+method is sufficient |
| Path-level metadata | `summary`, `description`, `parameters` | n/a | none | Nice to have |
| Content types | `content` map by media type | n/a | implicit JSON | Low priority |
| Security | `security` | n/a | none | Out of scope |
| Deprecation | `deprecated` | n/a | none | Out of scope |

## Design Principles

1. **Reuse the MCP pattern.** The MCP tool already solved "description + input schema + handler" in a SpecScript-native
   way. Http endpoints should follow the same structure rather than inventing a different one.

2. **Keep it flat.** OpenAPI's `requestBody.content["application/json"].schema` nesting is for multi-format APIs.
   SpecScript endpoints serve JSON only — no need for media type negotiation.

3. **Derive when possible.** Like MCP tools, when a `script` file has `Input schema`, derive the endpoint's input
   schema automatically. Extend this to `description` — when a `script` file has `Script info`, derive the endpoint's
   description from it. This makes script files fully self-describing: write `Script info` and `Input schema` once, and
   every consumer (CLI, MCP tool, Http endpoint) picks them up.

4. **Don't model responses (yet).** SpecScript endpoints return whatever `output`/`script` produce. Modeling response
   schemas adds complexity without clear SpecScript benefit today.

5. **Path + method = identity.** No need for `operationId` — the path and method are the natural identifiers.

## Proposals

### Proposal A: Minimal — Add `description` only (with derivation)

Add a single `description` field to the method handler. When `script` references a file, derive `description` from the
script's `Script info` if not provided explicitly.

```yaml
Http server:
  name: api
  port: 3000
  endpoints:
    /users/{id}:
      get:
        description: Get a user by ID       # explicit
        script: get-user.spec.yaml
      post:
        script: create-user.spec.yaml       # description derived from Script info
```

Given `create-user.spec.yaml`:

```yaml
Script info: Create a new user

Input schema:
  type: object
  properties:
    name:
      description: User name
  required: [name]

# ... implementation
```

The endpoint's `description` resolves to "Create a new user" automatically.

Schema change — `MethodHandler` gains `description: string` (optional). Explicit `description` takes precedence over
derived.

Implementation — trivial: `SpecScriptFile` already exposes `description` from `Script info`. The derivation function
reads `scriptFile.description`, same pattern as `deriveInputSchema` but simpler (single property read).

Pros:
- Minimal change, zero breaking impact
- Script files become single source of truth for description
- Consistent with MCP tool structure

Cons:
- No input schema means no validation, no auto-derived schemas from script files
- Endpoints remain opaque boxes compared to MCP tools

### Proposal B: Mirror MCP Tool — Add `description` + `inputSchema` (both with derivation)

Add both `description` and `inputSchema` to the method handler, matching the MCP tool definition. Both are derived
from the referenced script file when not provided explicitly.

```yaml
Http server:
  name: api
  port: 3000
  endpoints:
    /users:
      post:
        script: create-user.spec.yaml       # description AND inputSchema derived
    /users/{id}:
      get:
        description: Get a user by ID       # explicit override
        script: get-user.spec.yaml          # inputSchema derived
```

Given `create-user.spec.yaml`:

```yaml
Script info: Create a new user

Input schema:
  type: object
  properties:
    name:
      type: string
      description: User name
    email:
      type: string
      description: User email
  required: [name, email]

# ... implementation
```

The endpoint automatically gets `description: "Create a new user"` and the full `inputSchema` — zero duplication.

You can still provide explicit values when the endpoint needs to differ from the script:

```yaml
    /users:
      post:
        description: Create a user (admin only)    # overrides Script info
        inputSchema:                                # overrides Input schema
          type: object
          properties:
            name:
              type: string
          required: [name]
        script: create-user.spec.yaml
```

Schema change — `MethodHandler` gains:
- `description: string` (optional, derived from Script info if absent)
- `inputSchema: InputSchema` (optional, derived from Input schema if absent — same JSON Schema subset as MCP)

Implementation — extends the existing `deriveInputSchema` pattern. Both derivations happen in one pass over the
script file:

```kotlin
// Pseudocode — actual implementation reads SpecScriptFile once
fun deriveFromScript(handler: MethodHandlerData, context: ScriptContext): DerivedMetadata? {
    if (handler.script !is StringNode) return null
    val scriptFile = SpecScriptFile(context.scriptDir.resolve(handler.script.stringValue()))
    return DerivedMetadata(
        description = scriptFile.description,     // already available
        inputSchema = deriveInputSchema(scriptFile) // existing MCP logic
    )
}
```

This is simpler than the MCP tool implementation because it consolidates two separate concerns into one derivation.
The MCP tool currently derives `inputSchema` but requires explicit `description` — this proposal makes both derivable,
which is actually more consistent.

Derivation precedence (same for both fields):
1. Explicit value on the handler — wins
2. Derived from referenced script file — fallback
3. Not set — omitted (both fields are optional)

Note: this derivation should also be applied to MCP tools as a follow-up, making the behavior consistent across both
server types.

Pros:
- Full parity with MCP tools — same `HandlerInfo` extension pattern
- Script files become single source of truth — write once, used by CLI, MCP, and Http server
- Enables future input validation
- Enables automatic schema derivation from script files
- Sufficient for OpenAPI export of the most common patterns
- Actually reduces boilerplate compared to current MCP tool definitions (which require explicit description)

Cons:
- No separation of body vs query vs path parameters (OpenAPI's `parameters` array)
- `inputSchema` on a GET endpoint is semantically odd (GET has no body), though it maps naturally to query parameters in
  SpecScript since `${input}` already unifies body and query params

### Proposal C: OpenAPI-flavored — Add `description`, `inputSchema`, `parameters`, and `responses` (all with derivation)

Adopt a SpecScript-pragmatic subset of OpenAPI's Operation Object. All metadata derivable from script files.

```yaml
Http server:
  name: api
  port: 3000
  endpoints:
    /users:
      post:
        script: create-user.spec.yaml       # description and inputSchema derived
        responses:
          201:
            description: User created
          400:
            description: Invalid input
    /users/{id}:
      get:
        script: get-user.spec.yaml          # description derived
        parameters:
          id:
            description: The user ID
            type: string
            required: true
        responses:
          200:
            description: The user object
          404:
            description: User not found
```

Schema changes — `MethodHandler` gains:
- `description: string` (optional, derived from Script info if absent)
- `inputSchema: InputSchema` (optional, derived from Input schema if absent)
- `parameters: map<string, ParameterInfo>` (optional, for path/query params — simplified from OpenAPI's array-of-objects
  with `in` field)
- `responses: map<string, ResponseInfo>` (optional, description only — no schema)

Where `ParameterInfo` is: `{ description, type, required, default, enum }` — reusing the `Input schema` property
subset.

Where `ResponseInfo` is: `{ description }` — metadata only, no schema.

Derivation works the same as Proposal B for `description` and `inputSchema`. `parameters` and `responses` are always
explicit (no derivation source for these in script files).

Pros:
- Closest to OpenAPI structure, easing future export/import
- Separates body schema from parameter schema (semantically cleaner)
- Response descriptions enable documentation generation

Cons:
- More surface area to implement and maintain
- `parameters` as a flat map loses OpenAPI's `in` location concept (but for SpecScript's purposes, path params are
  inferred from path template and everything else is a query param)
- Response descriptions without response schemas are of limited value
- Adds complexity without clear runtime benefit (SpecScript doesn't validate schemas today)
- Risk of scope creep — once you start modeling responses, users will want response schemas
- `parameters` and `responses` cannot be derived, so they don't benefit from the derivation pattern

## Recommendation

**Proposal B** is the pragmatic sweet spot. Derivation of both `description` and `inputSchema` from referenced script
files makes it the zero-boilerplate option — an endpoint backed by a well-documented script needs no inline metadata at
all:

```yaml
    /users:
      post:
        script: create-user.spec.yaml     # everything derived
```

This mirrors the MCP tool pattern but improves on it (MCP tools currently require explicit `description`). The
derivation infrastructure is already in place: `SpecScriptFile.description` exists and `deriveInputSchema` is proven.
Adding description derivation is a single property read — it makes things simpler, not more complex.

Proposal A is too thin — without `inputSchema`, endpoints can't participate in schema derivation. Proposal C adds
non-derivable metadata (`parameters`, `responses`) that requires manual maintenance and provides documentation value
but no runtime benefit.

If Proposal B is adopted and `parameters` or `responses` are needed later, they can be added as incremental extensions
without breaking changes.

### Follow-up: Apply description derivation to MCP tools

If Proposal B is adopted, the same description derivation should be applied to MCP tools. Currently, `description` is
required on every MCP tool definition, even when the script already has `Script info`. Making it derivable would:
- Remove the last bit of duplication between tool definition and script file
- Make `description` optional on MCP tools when backed by a script file
- Align MCP tools and Http endpoints on the same derivation behavior

This is a minor breaking change (relaxing a required field) and should be proposed separately.
