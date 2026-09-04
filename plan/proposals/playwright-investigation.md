## Findings (from actually driving Playwright MCP)

Replayed the login/Releases scenario for real against a live `@playwright/mcp` server, first through SpecScript's
`Mcp call tool`, then via raw HTTP so a single MCP session could be held open across multiple tool calls. Answers below
are backed by what actually happened, not guesses.

### Replaying the scenario

Working call sequence, one session, one browser tab, no accessibility refs after the initial navigate:

1. `browser_navigate` → `http://release.digital.ai.local:5516`
2. `browser_fill_form` with
   `fields: [{target: 'input[placeholder="User"]', ...}, {target: 'input[placeholder="Password"]', ...}]`
3. `browser_click` with `target: 'role=button[name="Log in"]'`
4. `browser_click` with `target: 'role=link[name="Releases"]'`
5. `browser_snapshot` → confirmed `Welcome Release Administrator` in the tree

Each step's response includes the literal Playwright code it ran, e.g. `await page.locator('role=button[name="Log
in"]').click();` — worth surfacing to a SpecScript user as debug output, the same way `Shell`'s `show_command` does.

### Where do the `[ref=eNN]` refs come from? Are they static?

No. A ref is the element's position in a fresh accessibility-tree walk done at snapshot time, not a stable ID. Confirmed
empirically in one unbroken session: the "Log in" button was `ref=e33` right after navigate, shifted to
`e34` on a second snapshot of the *same, unchanged* page, then dropped back to `e33` after filling the form (which only
changed input values). Refs are recomputed every snapshot and are documented as valid only for the tool call immediately
following the snapshot that produced them.

The escape hatch — and the important discovery — is that every tool's `target` parameter accepts "an exact target
element reference from the page snapshot, **or a unique element selector**". Ordinary Playwright selectors
(`css=...`, `role=button[name="..."]`, `text=...`) work directly, no snapshot required first. That's what made the whole
scenario above replayable without ever reading a ref back into a later call. (One real gotcha hit along the way:
`text=Log in` was ambiguous — matched both a page-title div and the button — Playwright's strict-mode selector rules
apply as normal.)

### Playwright MCP's "ephemeral" philosophy

Confirmed two distinct ways this bites a longer-lived script, not just one:

1. **Browser state is scoped to the MCP session, not the server process.** SpecScript's `Mcp call tool`
   (`McpCallTool.kt`) opens a brand-new `Client`/transport and closes it per command. Two sequential `Mcp call tool`
   commands calling `browser_navigate` then `browser_snapshot` landed on two different blank tabs — the second command
   never saw the first one's page. `--shared-browser-context` did *not* fix this: a fresh `about:blank` tab appeared for
   the new session, and `browser_tabs list` didn't even show the old tab as a background tab — it was gone.
2. **A live session has a short idle timeout.** Within one held-open session, rapid-fire tool calls all succeeded; calls
   separated by roughly the time it takes a human (or an LLM composing a request) to think between actions got
   `Session not found`. The design assumes tight, continuous act/observe loops.

So the mismatch is real and specific: Playwright MCP is built for one continuous agentic loop over one session. A
SpecScript file that calls `Mcp call tool` multiple times today gets neither shared browser state nor a guarantee of
session survival between steps — that's an architectural gap, not a syntax question. Any multi-step declarative syntax
needs an explicit session-scoped construct — one connect, several tool calls, one close — mirroring the
`Mcp server` / `Stop mcp server` explicit-lifecycle pattern already used elsewhere in this codebase, e.g.:

```yaml
Mcp session:
  server: { url: http://localhost:8931/mcp, transport: HTTP }
  steps:
    - ...
```

### Sketch of a more declarative syntax

Not a proposal to implement — three simple examples, leaning on what actually worked above (selectors over refs,
`browser_fill_form`-style batching, one session per script):

```yaml
Mcp session:
  server: { url: http://localhost:8931/mcp, transport: HTTP }
  steps:
    - Navigate to: http://release.digital.ai.local:5516
    - Expect text: Log in
```

```yaml
Mcp session:
  server: { url: http://localhost:8931/mcp, transport: HTTP }
  steps:
    - Navigate to: http://release.digital.ai.local:5516
    - Fill form:
        User: admin
        Password: admin
    - Click: Log in
```

```yaml
Mcp session:
  server: { url: http://localhost:8931/mcp, transport: HTTP }
  steps:
    - Click: Releases
    - Expect text: Welcome Release Administrator
```

`Click: Releases` would need to resolve a human-readable name to a selector itself (e.g. try
`role=link[name="Releases"]`, fall back to `text=Releases`) — exactly the resolution done by hand above.
