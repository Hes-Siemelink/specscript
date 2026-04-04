# Proposal: Reference-grade README for the Release folders sample

## Problem

The `samples/digitalai/release/folders/` directory has a functional README but no testable code examples, no mock
server, and no way to verify the documentation stays in sync with the scripts. Interactive commands (`move`) are
completely untestable.

We want this sample to be the template for all future SpecScript sample projects.

## Goals

1. **README as executable spec** — the README stays `README.md` but is runnable by SpecScript directly.
2. **Mock server for deterministic tests** — record real Release API responses, replay them via a standalone mock
   server.
3. **Interactive examples become testable** — use `Answers` to provide input for `Prompt` commands.
4. **Friendly tutorial style** — the README reads naturally, with setup/teardown in comment blocks.
5. **Agent skill** — generalize the pattern into a reusable skill for writing READMEs with testable examples.

## Approach

### Phase 1: Record API responses

Use the existing recording proxy pattern (`samples/http-server/simple-proxy/`) to capture the Release API responses we
need. Record against the local Release server (`http://localhost:5516`).

Store recorded data under `tests/recorded-data/` following the established convention:

```
tests/recorded-data/
  api/v1/folders/list/depth=10/GET     ← folder listing response
  api/v1/folders/<id>/move/newParentId=<id>/POST   ← move response (empty body)
```

### Phase 2: Clean up recorded data

Replace the raw server output with a minimal, readable folder structure. Simplify IDs and use meaningful names:

```json
[
  {
    "id": "folder-1",
    "title": "Team Alpha",
    "children": []
  },
  {
    "id": "folder-2",
    "title": "Team Beta",
    "children": [
      {
        "id": "folder-3",
        "title": "Sprint 1",
        "children": []
      }
    ]
  },
  {
    "id": "folder-4",
    "title": "Archive",
    "children": []
  }
]
```

This gives us enough structure for list (flat + nested), move, and move-by-id tests. The data is hand-edited JSON,
committed to git.

### Phase 3: Mock server as standalone file

Create `tests/mock-server.spec.yaml` — a standalone mock server that can be started independently:

```yaml
Script info: Starts a mock Release server with recorded data

Http server:
  name: release-mock
  port: 25102
  endpoints:
    "*":
      get:
        script:
          Read file:
            resource: recorded-data/${request.path}/${request.query}/GET
      post:
        script:
          Read file:
            resource: recorded-data/${request.path}/${request.query}/POST
```

This is separate from the test runner so you can:

- Start it manually for development (`spec tests/mock-server`)
- Reference it from both the README and the test file

### Phase 4: README as executable spec

Rewrite `README.md` with executable code blocks. It keeps the `.md` extension (GitHub auto-displays it) but is runnable
by SpecScript:

```markdown
# Digital.ai Release — Folder Management

Working with folders in Digital.ai Release.

## Setup

These commands require a connection to the Release server. See [credentials](../credentials/README.md).

<!-- Start mock server for testing -->

```specscript comment
Run script: tests/mock-server
Http request defaults:
  url: http://localhost:25102
```

## List Folders

The `list` command shows all folders with full paths:

```specscript
List: {}
```

```expected console output
Team Alpha
Team Beta
Team Beta/Sprint 1
Archive
```

## Move a Folder (Interactive)

The `move` command lets you interactively select a source and target folder:

```specscript
Answers:
  - Team Beta/Sprint 1
  - Team Alpha
Move: {}
```

## Move by ID (Non-interactive)

For scripting, use `move-by-id` with explicit folder IDs:

```specscript
Move by id:
  source: folder-3
  target: folder-1
```

<!-- Cleanup -->

```specscript comment
Stop http server: release-mock
```

Key patterns:

- **Comment blocks for setup/teardown**: `specscript comment` blocks run but don't render in Markdown.
- **`Answers` for interactive commands**: prerecords selections for `Prompt` commands.
- **Mock server in separate file**: `Run script: tests/mock-server` starts it; keeps the README clean.

### Phase 5: Move test considerations

The `move` command flow:

1. GET /api/v1/folders/list?depth=10
2. Flatten to list with titles
3. Prompt: "Select the folder you want to move" (user picks by title)
4. Prompt: "Select the new parent folder" (user picks by title)
5. POST /api/v1/folders/{id}/move?newParentId={id}
6. List again to show results

The mock serves static data, so the post-move listing will be identical to the pre-move listing. For now, skip asserting
on post-move output — the value is demonstrating the interactive flow with `Answers`, not verifying the API's state
change. Stateful mocking can be added later if needed.

### Phase 6: Agent skill

Create a generalized Agent Skill (`samples/SKILL.md` or similar) for writing sample READMEs. The skill captures:

- How to structure a README with executable examples
- How to set up a mock server with recorded data
- How to use comment blocks for setup/teardown
- How to use `Answers` for interactive commands
- Directory layout conventions (`tests/`, `tests/recorded-data/`)

This skill can be reused by other sample projects to create consistent, testable documentation.

## File changes

### New files

- `samples/digitalai/release/folders/tests/mock-server.spec.yaml` — standalone mock server
- `samples/digitalai/release/folders/tests/recorded-data/...` — cleaned-up mock data files
- `samples/digitalai/release/folders/tests/folder-tests.spec.yaml` — test runner (optional, if edge case tests needed
  beyond the README)

### Modified files

- `samples/digitalai/release/folders/README.md` — rewritten with executable code blocks

### Unchanged

- All existing `.spec.yaml` scripts (`list`, `move`, `move-by-id`, etc.) — no changes needed
