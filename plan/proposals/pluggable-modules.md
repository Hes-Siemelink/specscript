# Pluggable Modules: Extending SpecScript Without a Package Manager

## Problem

SpecScript has 80+ commands. Some are core language (variables, control flow, testing), some are platform integrations
(MCP, SQLite, HTTP). The language-levels doc identifies the natural plugin boundary: Levels 0–1 are the kernel,
everything from Level 3 onward adds external dependencies.

The question is how to let users extend SpecScript with new commands — wrapping tools like ImageMagick, ffmpeg, pandoc,
kubectl, terraform — without building a dependency manager.

### Constraints

1. **Two implementations** (Kotlin now, TypeScript planned). Native plugins need platform-specific code.
2. **No dependency manager.** Building npm/Maven/cargo for SpecScript is a multi-year product. Not happening.
3. **WASM is premature.** WASI is still stabilizing, host API contracts are complex, and the tooling overhead exceeds
   the problem.
4. **Shell-accessible tools are abundant.** Most things people want to integrate already have a CLI.

## Proposed Solution: Three-Tier Extension Model

### Tier 1: Native Modules (shipped with the implementation)

Commands that need in-process state or bidirectional communication: HTTP server, SQLite, MCP, interactive prompts.
These are compiled into the implementation and registered via `CommandHandler`. This is what exists today — no change
needed.

The language-levels system already categorizes these. Each implementation declares which levels it supports.

### Tier 2: Composition Scripts (pure SpecScript, no shell)

Higher-level commands built entirely from existing SpecScript commands. No shell, no native code — just scripts that
compose other commands and expose the result as a named command.

This is arguably the most important tier because it covers the most natural use case: you write a script to solve a
problem, it works, and then you want to reuse it. The progression is:

1. **One-off script.** You write a `.spec.yaml` to hit your prod API, check service health, parse the response.
2. **Reusable command.** You add `Script info` and `Input schema`, move it to a shared directory. Now it's callable
   by name from other scripts.
3. **Shareable module.** You add a `specscript-config.yaml`, write a test, put it in a git repo. Someone else can
   clone it and import it.

This mirrors how Agent Skills work — you document what works, then you share it. The difference is that SpecScript
composition scripts are executable, testable, and have a typed input/output contract.

#### Examples

**API integration module** — a directory of scripts wrapping your company's internal API:

```
acme-api/
├── specscript-config.yaml
├── Get service health.spec.yaml
├── Deploy service.spec.yaml
├── Get recent incidents.spec.yaml
└── tests/
    └── Get service health tests.spec.yaml
```

A wrapper script is just HTTP calls with structure:

```yaml
# Get service health.spec.yaml
- Script info:
    name: Get service health
    description: Check health of an ACME service
- Input schema:
    type: object
    properties:
      service: { type: string }
      environment: { type: string, default: production }
    required: [service]

- Http request defaults:
    base url: https://api.acme.com
    headers:
      Authorization: Bearer ${env.ACME_TOKEN}

- GET: /services/${input.service}/health?env=${input.environment}
- Output:
    status: ${output.status}
    uptime: ${output.uptime_seconds}
    last_deploy: ${output.last_deployment}
```

Caller:

```yaml
- Get service health:
    service: payments
    environment: staging
- Print: "Payments is ${output.status}, uptime ${output.uptime}s"
```

**Data pipeline** — a script that orchestrates multiple data-manipulation commands:

```yaml
# Summarize csv.spec.yaml
- Script info: Summarize a CSV-style dataset
- Input schema:
    type: object
    properties:
      data: { type: array }
      group-by: { type: string }
    required: [data]

- For each:
    in: ${input.data}
    do:
      - Fields: [${input.group-by}, amount]
- As: filtered

- Sort:
    by: ${input.group-by}
    in: ${filtered}
- Output: ${output}
```

These are not toys — they're the kind of scripts people actually write day-to-day. The composition tier gives them a
path from "I wrote a thing" to "my team uses this thing" without any infrastructure beyond a directory and an import
line.

### Tier 3: Shell-Out Command Wrappers (pure SpecScript + OS tools)

A `.spec.yaml` script that wraps a CLI tool and exposes it as a named SpecScript command. This is the new thing.

#### How it works today (almost)

SpecScript already has the machinery:

- **Local file commands:** Any `.spec.yaml` in the same directory is callable by name. `convert-image.spec.yaml`
  becomes the command `Convert image`.
- **Imports:** `specscript-config.yaml` can import scripts from other directories.
- **Input/output contract:** Callers pass an object → `${input}` in the called script → `Output` becomes the return
  value.
- **Shell command:** Execute OS commands with variable interpolation.

A wrapper script today looks like this:

```yaml
- Script info: Convert an image using ImageMagick
- Input schema:
    type: object
    properties:
      source: { type: string }
      target: { type: string }
      resize: { type: string }
    required: [source, target]

- If:
    condition: ${input.resize}
    then:
      - Shell: convert ${input.source} -resize ${input.resize} ${input.target}
    else:
      - Shell: convert ${input.source} ${input.target}
```

A caller in the same directory uses it like:

```yaml
- Convert image:
    source: photo.jpg
    target: photo.png
    resize: 800x600
```

This already works. No new language features needed for the basic case.

#### What's missing

**1. Prerequisite checking.** When `convert` isn't installed, the user gets a raw shell error. The wrapper should be
able to declare its dependency and produce a helpful message:

```yaml
- Script info:
    name: Convert image
    description: Convert an image using ImageMagick
    requires:
      - command: convert
        install-hint: "brew install imagemagick (macOS) / apt install imagemagick (Linux)"
```

At script load time (not execution time), SpecScript checks `which convert` and fails with:

```
Error: Command 'Convert image' requires 'convert' which is not installed.
Install it: brew install imagemagick (macOS) / apt install imagemagick (Linux)
```

This is the one new feature this proposal adds to the language.

**2. Distribution convention.** How do users share wrapper packages? Not a package manager — a convention:

```
imagemagick-specscript/
├── specscript-config.yaml      # module metadata
├── Convert image.spec.yaml
├── Resize image.spec.yaml
├── Image info.spec.yaml
└── tests/
    └── Convert image tests.spec.yaml
```

Users install by cloning/downloading the directory and adding an import:

```yaml
# In the consumer's specscript-config.yaml
imports:
  - ../modules/imagemagick-specscript/Convert image.spec.yaml
  - ../modules/imagemagick-specscript/Resize image.spec.yaml
```

Or importing an entire directory (not currently supported — see open questions).

**3. Structured I/O with shell commands.**

This is the critical piece for making shell wrappers feel like real commands rather than string-munging hacks.

#### The input problem: command-line string fudging

Today, passing data to a Shell command means interpolating variables into the command string:

```yaml
- Shell: convert ${input.source} -resize ${input.resize} ${input.target}
```

This works for simple scalar arguments. It breaks down when:

- Values contain spaces or special characters (quoting hell)
- You need to pass structured data (a JSON object, a list of items)
- The command line becomes long and unreadable with many parameters

The cleaner approach: **pass input via environment variables.** Shell already exposes all SpecScript variables as
environment variables. A wrapper script can build its data in SpecScript — where structured data is native — and let
the shell command read it from the environment:

```yaml
- Script info: Query Kubernetes pods
- Input schema:
    type: object
    properties:
      namespace: { type: string }
      label-selector: { type: string }
      output-format: { type: string, default: json }
    required: [namespace]

- As:
    kubectl_ns: ${input.namespace}
    kubectl_labels: ${input.label-selector}
    kubectl_format: ${input.output-format}

- Shell: kubectl get pods -n "$kubectl_ns" -l "$kubectl_labels" -o "$kubectl_format"
```

For more complex input — say a JSON body to pass to a tool's stdin — build it in SpecScript and pipe it in:

```yaml
- As:
    request_body:
      filters:
        namespace: ${input.namespace}
        status: running

# Write to a temp file and pass it (stdin piping not yet supported)
- Temp file:
    content:
      /Json: ${request_body}
- As: body_file
- Shell: some-tool --config "$body_file"
```

This is more verbose than ideal but keeps the SpecScript side clean and structured. The command line stays short and
readable because it references named variables rather than inlining complex data.

A future improvement could be stdin piping directly on the Shell command:

```yaml
- Shell:
    command: some-tool --from-stdin
    stdin:
      /Json: ${request_body}
```

But that's a separate enhancement, not required for the pluggable modules pattern.

#### The output problem: everything is a string

Today, Shell captures stdout as a plain string. Always. If `kubectl get pods -o json` returns a JSON object, you get
it as a string. To use it as structured data, you need a manual conversion step:

```yaml
- Shell: kubectl get pods -n default -o json
- As: raw_json
- Output:
    /Json: ${raw_json}
```

This works but every wrapper script pays this three-line tax: run, capture, parse. It's ceremony that obscures the
actual intent.

**Proposed: `output format` option on Shell.** A new option that tells Shell to parse stdout before storing it in the
output variable:

```yaml
- Shell:
    command: kubectl get pods -n "$kubectl_ns" -o json
    output format: json
```

When `output format: json` is set, Shell parses stdout as JSON and returns a structured node (ObjectNode/ArrayNode)
instead of a StringNode. If parsing fails, it throws a clear error: "Shell output is not valid JSON."

The full set of format values:

| Value | Behavior |
|---|---|
| `text` | Current default. Return stdout as a plain string. |
| `json` | Parse stdout as JSON. Error if invalid. |
| `yaml` | Parse stdout as YAML. Error if invalid. |
| `lines` | Split stdout into an array of strings, one per line. |

`lines` is useful for tools that output one-item-per-line (`find`, `ls`, `git log --oneline`). Without it, you end
up doing string splitting in SpecScript, which is awkward.

#### What a clean wrapper looks like with both improvements

```yaml
# kubectl-get-pods.spec.yaml
- Script info:
    name: Kubectl get pods
    description: List Kubernetes pods in a namespace
    requires:
      - command: kubectl
        install-hint: "https://kubernetes.io/docs/tasks/tools/"

- Input schema:
    type: object
    properties:
      namespace: { type: string, default: default }
      label-selector: { type: string }
    required: [namespace]

- As:
    kubectl_ns: ${input.namespace}
    kubectl_labels: ${input.label-selector}

- Shell:
    command: kubectl get pods -n "$kubectl_ns" -l "$kubectl_labels" -o json
    output format: json

# Caller gets back a parsed JSON object, not a string.
# No As/Json ceremony needed.
```

And the caller:

```yaml
- Kubectl get pods:
    namespace: production
    label-selector: app=web
- As: pods
- Print: "Found ${pods.items.size} pods"
```

This reads like a native command. The shell plumbing is invisible.

## What This Means for MCP, SQLite, etc.

These stay as Tier 1 native modules. They need in-process state:

| Module | Why it can't shell out |
|---|---|
| MCP server | Persistent JSON-RPC connection, bidirectional communication |
| SQLite | In-process database handle, transactions, connection pooling |
| HTTP server | Long-running process with request routing |
| Prompts | Terminal UI control, cursor positioning, input handling |

The distinction is clear: if the tool has a CLI that takes input and produces output per invocation, it's a Tier 3
shell-out wrapper. If it needs persistent state or bidirectional communication within the SpecScript process, it's
Tier 1. If it only needs existing SpecScript commands (HTTP, data manipulation, etc.), it's a Tier 2 composition
script — the lightest and most portable option.

## New Language Features

### 1. `requires` in Script Info

```yaml
- Script info:
    name: My command
    requires:
      - command: convert           # binary name to check on PATH
        install-hint: "..."        # optional human-readable install instructions
      - command: ffmpeg
        install-hint: "..."
```

Behavior:

- Checked when the script is loaded as a command (not when it's parsed, not when it's executed).
- Uses `which`/`where` to verify the binary exists on PATH.
- On failure, throws a clear error with the install hint.
- Multiple requirements are all checked; all failures reported together.

This is a small, self-contained addition to `Script info` schema and the `SpecScriptFile` loader.

### 2. `output format` on Shell

```yaml
- Shell:
    command: kubectl get pods -o json
    output format: json
```

| Value | Behavior |
|---|---|
| `text` | Current default. Return stdout as a plain string. |
| `json` | Parse stdout as JSON. Return structured node. Error if invalid. |
| `yaml` | Parse stdout as YAML. Return structured node. Error if invalid. |
| `lines` | Split stdout into an array of strings, one per line. |

This eliminates the three-step ceremony (`Shell` → `As` → `/Json`) that every structured-output wrapper would
otherwise need. It's an addition to `Shell.schema.yaml` and a ~20 line change in `Shell.kt` — parse the captured
string before returning it.

## Open Questions

### Directory imports

Today `imports` lists individual files. Should we support importing an entire directory?

```yaml
imports:
  - ../modules/imagemagick-specscript/    # import all .spec.yaml in directory
```

This would reduce boilerplate for multi-command modules. The mechanism would mirror local file commands — scan the
directory, register each file as a command. The `specscript-config.yaml` in the imported directory could declare which
files are public (vs. internal helpers).

### Version compatibility

Should wrapper modules declare which SpecScript level they require?

```yaml
# In the module's specscript-config.yaml
Script info:
  requires-level: 3    # needs Shell command
```

This would let SpecScript fail early if a module is imported into an implementation that doesn't support its required
level. Low priority — useful for the multi-implementation future but not needed now.

### Error handling for shell failures

Shell commands return exit codes. Should wrappers have a standard pattern for translating these to SpecScript errors?
Today you can use `On error` around the `Shell` command. A convention might be enough — no language change needed.

## What We're NOT Doing

- **Runtime plugin loading.** No dynamic class loading, no eval, no JAR/DLL loading.
- **Package manager.** No registry, no version resolution, no transitive dependencies.
- **WASM runtime.** Premature. Revisit when WASI stabilizes.
- **Plugin SDK for native modules.** The `CommandHandler` interface is already the SDK. Formalizing it for external
  contributors is a separate concern — relevant when there are external contributors.

## Implementation Scope

If accepted, the work is:

1. **Add `requires` to Script info schema** — extend `ScriptInfo.schema.yaml` and `ScriptInfoCommand.kt`.
2. **Add prerequisite checking to SpecScriptFile** — check `requires` when loading a file as a command.
3. **Add `output format` to Shell schema** — extend `Shell.schema.yaml` and `Shell.kt` to parse stdout as
   JSON/YAML/lines when requested.
4. **Spec and tests** — new sections in `Script info.spec.md` and `Shell.spec.md`, or dedicated spec files.
5. **One example wrapper module** — e.g., a simple `jq` wrapper to demonstrate the pattern.

`stdin` piping, directory imports, and level requirements are follow-up work.
