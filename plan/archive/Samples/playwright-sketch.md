# Playwright MCP on the SpecScript execution layer

Follow-up to [playwright-investigation.md](playwright-investigation.md). That doc's `Mcp session: { steps: [...] }`
sketch was a mistake: it's a mini-DSL nested inside SpecScript, duplicating control flow SpecScript already has
(`If`, `Assert that`, `For each`, `On error`, variables). A session only needs to solve one problem — keep one MCP
connection open across several `Mcp call tool` calls — so it should be two lifecycle commands, not a block:

- `Mcp start session` — connects once, holds the connection under a name, and marks it as the *current* session.
- `Mcp stop session: <name>` — closes it. Same shape as the existing [`Stop mcp
  server`](../../specification/commands/ai/mcp/Stop%20mcp%20server.spec.md), which already takes a plain string name.
- `Mcp call tool` gains a `session: <name>` alternative to `server: {...}` — reuse that connection instead of
  connecting fresh. When *neither* `server:` nor `session:` is given, it falls back to the current session, so a run
  of calls right after `Mcp start session` doesn't need to repeat the name on every line. `session: <name>` is only
  needed once more than one session is open at a time. `server: {...}` keeps working exactly as it does today for a
  one-shot call with no session at all.

None of this exists yet — no `Mcp start session`, no `Mcp stop session`, no `session:` field, no current-session
fallback. Every example below is what it *would* look like.

Everything between start and stop is just ordinary SpecScript. No new verbs for navigate/click/fill/screenshot — they're
already `Mcp call tool` calls against Playwright MCP's existing tools (`browser_navigate`, `browser_fill_form`,
`browser_click`, `browser_take_screenshot`, `browser_snapshot`). That's the whole point: the toolbox is the four tools,
the scripting is SpecScript's own.

## Three use cases

### 1. One screenshot for a doc page

The minimum viable case — no control flow needed, just proving the session survives across two calls.

```yaml
Mcp start session:
  name: docs
  server:
    url: http://localhost:8931/mcp
    transport: HTTP

Mcp call tool:
  session: docs
  tool: browser_navigate
  input:
    url: http://release.digital.ai.local:5516

---
Mcp call tool:
  session: docs
  tool: browser_take_screenshot
  input:
    filename: docs/screenshots/login-page.png
    fullPage: true

Mcp stop session: docs
```

### 2. Login, assert, screenshot — with real error handling

Shows `Assert that`'s `in` condition doing a substring check on the page snapshot text (it's a real, existing
condition — `container.stringValue().contains(node.stringValue())` in `Conditions.kt`, not something new), and
`On error` guaranteeing the session gets closed even if a step fails — a `finally`, built from primitives SpecScript
already has.

```yaml
${target}: http://release.digital.ai.local:5516

Mcp start session:
  name: docs
  server:
    url: http://localhost:8931/mcp
    transport: HTTP

# Array under a single command = one call per item (a real, existing SpecScript feature — CommandExecution.kt
# auto-iterates any array for a handler that doesn't declare handlesLists()). No session: needed on each item;
# all three fall back to the current session opened above.
Mcp call tool:
  - tool: browser_navigate
    input:
      url: ${target}
  - tool: browser_fill_form
    input:
      fields:
        - target: input[placeholder="User"]
          name: User
          type: textbox
          value: admin
        - target: input[placeholder="Password"]
          name: Password
          type: textbox
          value: admin
  - tool: browser_click
    input:
      element: Log in button
      target: role=button[name="Log in"]

---
Mcp call tool:
  tool: browser_snapshot
As: ${page}

Assert that:
  item: Welcome Release Administrator
  in: ${page}

---
Mcp call tool:
  tool: browser_take_screenshot
  input:
    filename: docs/screenshots/home-logged-in.png

---
On error:
  Print: "Doc screenshot run failed: ${error}"

Mcp stop session: docs
```

`On error` clears `context.error` before its block runs (see `OnError.kt`), so execution falls through to the
`Mcp stop session` line either way — happy path or caught error, the session always gets closed. This only works cleanly
because a failed `Mcp call tool` now throws a real `SpecScriptCommandError` carrying the actual MCP error text (fixed
earlier — it used to get silently rewrapped into a generic message), so `${error}` here is actually useful.

### 3. Batch screenshots across pages, one session

The case that actually justifies "sit on the SpecScript execution layer" over a `steps:` block: reuse `For each`
directly, no bespoke looping construct needed inside the session.

```yaml
Mcp start session:
  name: docs
  server:
    url: http://localhost:8931/mcp
    transport: HTTP

${pages}:
  - name: login
    url: http://release.digital.ai.local:5516
  - name: releases
    url: http://release.digital.ai.local:5516/#/releases
  - name: settings
    url: http://release.digital.ai.local:5516/#/settings

For each:
  ${page} in: ${pages}
  Mcp call tool:
    - tool: browser_navigate
      input:
        url: ${page.url}
    - tool: browser_take_screenshot
      input:
        filename: docs/screenshots/${page.name}.png
        fullPage: true

Mcp stop session: docs
```

Three pages, three screenshots, one browser tab, one MCP session — and the loop, the variable interpolation, and the
filename construction are all just SpecScript doing what it already does elsewhere, not new machinery invented for
Playwright.

### 4. Cropping a screenshot to one element

Verified live: `browser_take_screenshot` takes the same `element`/`target` pair as `browser_click` — give it a
selector instead of `fullPage: true` and it screenshots just that element's bounding box, not the whole viewport.
Under the hood it's literally `page.locator(target).screenshot(...)` — Playwright's own element screenshot, not
something Playwright MCP invented. Tested live: a full-viewport shot came out 15.8KB, one scoped to `main` came out
13.3KB (visibly excludes the page chrome), and one scoped to `role=button[name="Log in"]` came out as just that
button, tightly cropped to its own bounds.

```yaml
Mcp call tool:
  tool: browser_take_screenshot
  input:
    element: Log in button
    target: role=button[name="Log in"]
    filename: docs/screenshots/login-button.png
```

Worth knowing what this *isn't*: there's no `clip`/`x`/`y`/`width`/`height` parameter in the tool's schema, so there's
no way to crop to an arbitrary pixel rectangle unrelated to a real element. "Crop" here always means "screenshot of
whatever this selector resolves to" — fine for docs (you're usually illustrating a specific button, form, or panel
anyway), but not a substitute for pixel-level control if that's ever needed.

## Open questions this sketch doesn't answer

- **Schema**: `session` and `server` on `Mcp call tool` should be mutually exclusive (`oneOf`), matching the
  `Condition` schema's own style of making exactly-one-of explicit rather than silently preferring one.
- **Idle timeout inside a long `For each`**: fine in the tests run so far (rapid calls), untested with a batch large or
  slow enough to hit Playwright MCP's session idle timeout mid-loop. Needs a real test with dozens of pages before
  trusting this for a real docs run.
- **Crash cleanup**: if the SpecScript process dies mid-script, `Mcp stop session` never runs and the browser tab (or
  whole browser, without `--isolated`) is orphaned on the MCP server. Recommend `--isolated` as standard advice for now
  rather than solving it here.
- **Screenshot bytes**: `browser_take_screenshot` returns the image inline as a proper MCP `ImageContent` block *and*
  writes it to `filename`. The examples above rely only on the file path, since `McpCallTool.kt`'s
  `firstTextAsJson()` currently drops non-text content types silently. Fine for this use case; would need fixing if
  something ever wants the bytes without a file round-trip.

---

