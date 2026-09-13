# Proposal: 5-minute "wow" demo of SpecScript

Source idea: `plan/ideas/specscript-demo.md`.

## Constraints (from clarifying round)

- **Venue:** conference / meetup talk, live projector, mixed technical + semi-technical crowd.
- **Format:** hybrid — slogan slides + static code slides + a couple of genuinely live moments (CLI or agent).
- **Spine:** theme "AI needs a language of its own." The other points (Markdown+YAML, declarative-not-clever,
  CLI≡MCP, testable, composable, connect-to-anything) get *folded in* as escalations, not as separate chapters.
- **Escalation shape:** "If we have this (hat), what about THIS (rabbit)!" Keep piling up. No
  introduce→feature→introduce rhythm.
- **Depth:** slide skeleton + beats. No word-for-word talk track.

## The hero example (the surprise)

**Aurora watch.** A spec that asks NOAA's public space-weather feed "will the northern lights be visible tonight?"
Reasons it beats the boring options:

- Public JSON, **no API key** — safe to run live.
- Semi-technical crowd instantly *gets it* and *wants it*: "wake me when the sky glows."
- Emotional payoff for the escalation: a plain script becomes a thing your AI agent can call on your behalf.

Fallbacks if NOAA is flaky on the day: **ISS overhead tracker** (open-notify) or **earthquake feed** (USGS) — same
shape, same escalations. Pick one, pre-record the live segment as insurance.

Real endpoint shape (grounds the code slides):

```yaml
GET: https://services.swpc.noaa.gov/products/noaa-planetary-k-index.json
As: ${kp}
```

Across all three threads the escalation ladder is the same four rungs, revealed at different speeds:

1. **A spec** (YAML: declarative, readable) →
2. **a CLI** (`spec aurora`) →
3. **an MCP tool for free** (same file, `spec --mcp`) →
4. **a tested, trustworthy spec** (`spec --test`).

The "connect to anything / DB / credentials" points are sprinkled as one-liners, never as a stop.

---

## Timing budget (all three threads)

~5 min core + **~1 min finale** ≈ **16–18 slides**. Slogan slides 8–12s, code slides 15–20s, live moment 30–45s
each (max two in the core), finale 60s. Rule: never let a code slide sit without a preceding one-line context
sentence.

---

## The finale: "Now the LLM writes it" (shared closer, ~1 min)

The whole demo has argued SpecScript is the LLM's language. **Prove it live: one-shot an app from a prompt.**

- **Payoff:** everything piled up in the thread — self-describing (`Script info`), CLI≡MCP, testable, repeatable —
  now falls out of a *single prompt*, not human typing.
- **Beat:** paste one prompt into an agent → out comes a `.spec.yaml` with `Script info`, an `Input schema`, the
  logic, and a `Test case`. Then `spec --test` it live: **green.** The machine wrote a spec that proves itself.
- **Prompt shape (pre-tested, on a slide):** "Write a SpecScript spec that reports whether the ISS is currently
  overhead a given latitude/longitude. Include a Script info line, an Input schema, and a Test case."
- **Why it lands:** it's not "AI generated some code we hope works" — it's a self-describing, testable, repeatable
  artifact. Chaos → contract, authored by the model itself.

Risk: this is the most fragile live moment. **Pre-record it and have the finished file on disk** as a one-keystroke
fallback (`spec --test iss.spec.yaml`). If the room is hot, run it live; if not, play the recording and still hit
the green test live.

Fold into each thread by replacing the final slogan slide (A#15 / B#14 / C#13) with a two-slide finale:
**"Now the LLM writes it"** (live/recorded one-shot) → **"…and it's their language."** (land the theme).

---

## Thread A — "Give the machine its mother tongue" (recommended)

Spine: LLMs are drowning in prose and choking on code. SpecScript is the register in between. Each rung shows the
language doing *more* for the *same* file.

| # | Slide | Type | Beat |
|---|-------|------|------|
| 1 | "AI wants spec-first." | Slogan | Hook: the model asks for the spec, not the code. |
| 2 | Wall of Markdown | Slogan | Spec-first means *a lot* of Markdown. Informal, ambiguous. |
| 3 | "Formal, please." | Slogan | We need something a machine can execute without guessing. |
| 4 | Markdown + YAML side by side | Static code | SpecScript is both: informal prose, formal YAML. |
| 5 | The curl flash | Static code | 2-sec flash of arcane `curl -X POST -H ...`. "Chaos." Cut. |
| 6 | Same call as YAML | Static code | Declarative POST with headers. Readable. Reveal: *this is the language.* |
| 7 | "You don't code. You spec." | Slogan | YAML stops you being clever. Anti-Cobol. |
| 8 | Aurora spec on screen | Static code | Hero example rung 1: 6 lines, hits NOAA, outputs verdict. |
| 9 | **LIVE:** `spec aurora` | Live CLI | Rung 2. It's a CLI. Instant. |
| 10 | "It's already an MCP tool." | Slogan | Rung 3. Same file. No extra work. |
| 11 | **LIVE:** agent calls the tool | Live agent | Agent in a chat window invokes `aurora`. The wow. |
| 12 | "A spec you can't test is a lie." | Slogan | Set up rung 4. |
| 13 | `spec --test aurora` | Static code + live flash | Test case + assert. Green. Trust. |
| 14 | Composability pile-up | Slogan | One line each: Connect to, SQLite history, Shell/MCP reach. |
| 15 | "SpecScript is the LLM's language." | Slogan | Wordy, formal, testable, composable. Land it. |

Folded-in points: curl-chaos (5), declarative-not-clever (6–7), CLI≡MCP (9–11), testable (12–13), DB + connect +
composable as the closing pile-up (14).

---

## Thread B — "The one file that kept growing powers"

Spine: literally never leave the aurora file. Every slide the *same* file gains an ability. Maximal "hat → rabbit"
because nothing is ever introduced twice — it just levels up on screen.

| # | Slide | Type | Beat |
|---|-------|------|------|
| 1 | "AI wants spec-first → mountains of Markdown." | Slogan | Compress idea's first three bullets into one. |
| 2 | Blank file `aurora.spec.yaml` | Static code | Start from nothing. |
| 3 | + the GET | Static code | One declarative HTTP call. (Curl flash as the "before".) |
| 4 | + verdict logic | Static code | `If kp >= threshold` → "Lights tonight!" Still readable. |
| 5 | **LIVE:** run it | Live CLI | It runs. It's a CLI. Rung 2 sneaks by unannounced. |
| 6 | "Wait — it's also an MCP tool." | Slogan | Reveal rung 3 as a surprise, not a plan. |
| 7 | **LIVE:** agent calls it | Live agent | Rabbit out of the hat. |
| 8 | + `Input schema` (a `--location` flag) | Static code | Same file grows a parameter. Credentials via `env:` one-liner. |
| 9 | + `SQLite` history | Static code | Fold DB storage in matter-of-factly. Now it remembers. |
| 10 | + `Connect to` | Static code | Named connection hides the URL/secret. Composable. |
| 11 | + `Test case` / `Assert` | Static code | The file tests itself. |
| 12 | `spec --test` green | Live flash | Trust earned. |
| 13 | Zoom out: the whole file | Static code | It was *always one file.* CLI + MCP + DB + tests. |
| 14 | "This is why LLMs will pick it." | Slogan | Wordy, formal, repeatable, composable. |

Folded-in points: curl (3), declarative (whole thread), CLI≡MCP (5–7), DB (9), credentials/connect (8,10),
testable (11–12), composable (13).

Risk note: two live segments back-ish to back (5,7). Pre-record 7 as insurance.

---

## Thread C — "From prompt to promise"

Spine: journey of trust. Starts with a vague AI prompt (chaos), ends with a signed, tested, callable contract.
Best if the crowd is skeptical of "AI slop" — leans into repeatability.

| # | Slide | Type | Beat |
|---|-------|------|------|
| 1 | A vibe-coded blob + curl flash | Slogan/static | "This is what 'AI, do it' gives you." Chaos. |
| 2 | "Spec-first fixes this — but Markdown alone can't run." | Slogan | Bridge. |
| 3 | The aurora spec | Static code | Declarative YAML. Prose says *what*, YAML *does* it. |
| 4 | "You spec, you don't code." | Slogan | Anti-Cobol guardrail. |
| 5 | **LIVE:** `spec aurora` | Live CLI | A promise you can run. |
| 6 | "Same promise, now an MCP tool." | Slogan | CLI≡MCP, free. |
| 7 | **LIVE:** agent honors the promise | Live agent | Wow. |
| 8 | "A promise you can't test isn't one." | Slogan | Turn. |
| 9 | Test + assert | Static code | Rung 4 as the emotional core of *this* thread. |
| 10 | `spec --test` green | Live flash | Repeatable. Signed. |
| 11 | Reach: Shell + MCP + DB | Slogan | Connect to anything. One line each. |
| 12 | "The observation" | Slogan | LLMs prefer the CLI once they find it. Insider wink. |
| 13 | "SpecScript: the LLM's language." | Slogan | Close. |

Folded-in points: curl + slop (1), declarative (3–4), CLI≡MCP (5–7), testable/repeatable as the spine (8–10),
connect/DB/composable (11), the CLI-over-MCP observation as a knowing closer (12).

---

## Recommendation

**Thread A** for a mixed conference crowd: clearest arc, the theme is explicit at both ends, and it caps the two
risky live moments cleanly. **Thread B** if you want the strongest "one file, endless rabbits" spectacle and trust
the live segments. **Thread C** if the room is AI-skeptical and "repeatable/trustworthy" is the message that lands.

## Open decisions for you

- Hero example: Aurora (recommended) vs ISS vs earthquakes.
- One live agent segment or two? (Two = more wow, more risk. Pre-record either way.)
- Which agent UI for the MCP moment (matters for slide 11 mockups).
- Finale live vs recorded, and whether to reuse the hero example or one-shot a fresh one (ISS) for contrast.
