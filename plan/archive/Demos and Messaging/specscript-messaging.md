# SpecScript Messaging Analysis

Comparing the README with the four overview documents in `specification/overview/`.

## Core Identity: Two Competing Narratives

The documents oscillate between two distinct value propositions:

**Narrative A — "Practical scripting tool"** (README leads with this)
> Write a GET request in one line. Make it a CLI with a handful more lines. Add endpoints and it's a server. No
> boilerplate, no build step.

This positions SpecScript as a **developer productivity tool** — a better curl, a faster way to prototype, an instant
CLI builder. The README is almost entirely about this.

**Narrative B — "Executable specifications"** (Overview docs lean this way)
> Documentation that can't lie, because it executes.

This positions SpecScript as a **specification/documentation system** where the killer feature is that docs, tests, and
behavior are unified. The PM and sales overviews are almost entirely about this.

The README buries Narrative B deep (line 342: "The spec IS the test suite") and treats it as a meta curiosity about the
project itself, not as a user-facing value proposition. Meanwhile, the overview docs treat Narrative A as an
afterthought — the developer overview lists "API prototyping" as one of five bullet points at the bottom.

## Specific Messaging Tensions

### 1. Who is this for?

- **README**: A developer who wants to make HTTP calls and build quick CLIs. The persona is someone who's "tired of
  remembering curl syntax."
- **PM overview**: Product managers who want readable specifications and verifiable acceptance criteria.
- **Sales overview**: Organizations with documentation-goes-stale problems.
- **Agent overview**: AI agents that need to generate scripts.

These are very different audiences with very different motivations. The README doesn't acknowledge the non-developer
audiences at all.

### 2. What IS it?

- **README**: "It's just Yaml" — a scripting language.
- **Developer overview**: "A declarative scripting language" that produces "executable specifications."
- **PM overview**: A tool that "collapses specification, documentation, and tests into one artifact."
- **Sales overview**: "Turns specifications into running software."
- **Agent overview**: "Replaces shell scripts, small Python/Node utilities, and boilerplate HTTP glue."

The "what is it" answer changes depending on which doc you read.

### 3. The `.spec.md` story

The README barely mentions `.spec.md` — it's the format the entire specification directory uses, and it's the heart of
the "executable documentation" story, but the README is 100% `.spec.yaml` focused. The developer overview mentions it
but doesn't demo it. The PM/sales overviews reference it conceptually without showing how it works.

### 4. MCP servers

The agent overview gives MCP significant space (code examples and all). The PM and sales overviews mention it as a
differentiator. The README doesn't mention MCP at all. For a product that considers itself "AI-Native," this is a gap.

### 5. The "just YAML" claim vs. reality

The README emphasizes simplicity: "No special syntax — it's Yaml all the way down." But then you need `---` separators,
`${...}` interpolation, capital-letter conventions, `As:` for variable capture — there IS a language on top of YAML. The
agent overview is the most honest about this: "standard YAML with a `${variable}` interpolation layer on top."

## What's Working

- The **README's opening examples** are strong. GET in one line, then build up to a CLI — that's a clear progression.
- The **PM overview** has the tightest single-sentence summary: "SpecScript makes specifications executable, so that
  documentation, tests, and behavior can never disagree."
- The **agent overview** is the most technically precise and practically useful document of the four.
- The **sales competitive positioning table** is effective.

## Key Questions

1. **Which narrative is primary?** Is SpecScript mainly a scripting/prototyping tool (Narrative A) or mainly an
   executable specification system (Narrative B)? The README says A, the overviews say B. These aren't mutually
   exclusive, but the lead message matters.

2. **Should the README address the non-developer audiences?** Right now it's purely a developer-facing doc. The PM/sales
   value propositions are hidden in the overview directory.

3. **Does MCP belong in the README?** Given the AI-native positioning in the overview docs, the README's silence on MCP
   is conspicuous.

4. **Are these four overview documents the right structure?** The PM and sales docs have 80% overlap. The review note on
   the sales doc already flags "Merge with PM overview." The agent and developer docs also overlap substantially.

---

## Deeper Analysis: What Does the Codebase Say SpecScript Actually Is?

The documents claim different things. The codebase reveals what SpecScript actually invested in.

### Command Distribution (82 commands total)

| Category              | Commands | Share  |
|-----------------------|----------|--------|
| Testing               | 11       | 13.4%  |
| Data Manipulation     | 10       | 12.2%  |
| HTTP                  | 9        | 11.0%  |
| MCP / AI              | 8        | 9.8%   |
| Utilities             | 8        | 9.8%   |
| Credentials           | 7        | 8.5%   |
| Control Flow          | 6        | 7.3%   |
| Files                 | 5        | 6.1%   |
| Variables             | 3        | 3.7%   |
| Errors                | 3        | 3.7%   |
| Script Info           | 3        | 3.7%   |
| User Interaction      | 3        | 3.7%   |
| Database              | 3        | 3.7%   |
| Shell                 | 2        | 2.4%   |

Key observation: HTTP + Credentials combined is the single largest functional area at ~20%. Testing is the
largest standalone category. Data manipulation is second. MCP/AI is a substantial and growing area.

The language's center of gravity is **structured data flowing through HTTP endpoints, verified by tests**. Control flow
is deliberately minimal (6 commands) — just enough to loop, branch, and compose. The language prefers declarative
data flow over imperative programming.

### Samples Breakdown (14 sample projects)

| Use Case              | # of Samples |
|-----------------------|--------------|
| HTTP/API automation   | 5+           |
| MCP servers           | 3            |
| SQLite CRUD apps      | 3            |
| Testing showcase      | 2            |
| CLI tool building     | 3            |
| Shell integration     | 2            |
| Programming/algorithms| 2            |

The samples are aimed at **DevOps engineers and platform teams** who interact with REST APIs daily, want quick CLI tools,
and are interested in AI agent integration. HTTP/API automation dominates. MCP is well-represented. General-purpose
programming is minimal (Tower of Hanoi is a proof-of-concept, not a use case).

Notable sample gaps: error handling (no sample), MCP resources and prompts (only tools are shown), data manipulation
commands (most are absent from samples), PATCH/DELETE HTTP methods (barely present).

### Language Docs vs. README: A Philosophical Split

The language docs (`specification/language/`) reveal a telling priority ordering:

1. **YAML as foundation** (245 lines) — "The main idea is to blur the line between code and data."
2. **Executable Markdown** (706 lines — the longest spec file) — The literate programming model gets the deepest
   treatment.
3. **Variables and data flow** (289 lines) — Data composition is foundational.
4. **Packages** (549 lines) — Reuse and distribution.

The language docs know SpecScript is primarily about **executable documentation written in YAML/Markdown**. They don't
mention HTTP, CLI tools, or prototyping — those are command-level concerns, not language-level ones.

The README reverses this: it leads with HTTP and CLI (command-level concerns) and barely mentions executable
documentation (the language-level identity).

### The Three Consistent Messages

Across all docs, three messages survive intact:

1. **"Documentation that can't lie"** — appears in every document. The single most consistent phrase.
2. **"It's just YAML"** — the main README, language docs, and agent overview all reinforce this.
3. **Declarative philosophy** — "say what you want, not how" appears in multiple forms everywhere.

These three could anchor a unified message.

### The AI Story Is Fragmented

- The agent overview opens with "replaces shell scripts" and leans on AI readability.
- The sales overview calls SpecScript "AI-Native."
- The README doesn't mention AI or MCP at all.
- The language docs have zero AI framing.
- The samples have 3 MCP servers but no MCP resources or prompts.

There is a real AI/MCP story here (8 commands, 3 sample projects, tool schema generation from Input schema), but it's
not coherently told anywhere. The README ignores it. The overviews assert it without the README backing it up.

## Synthesis: The Core Problem

SpecScript has a **layered identity** that the messaging doesn't respect:

- **Layer 1 (Language):** Executable YAML/Markdown. "Documentation that can't lie."
- **Layer 2 (Capabilities):** HTTP client/server, testing, data manipulation, CLI generation, MCP servers.
- **Layer 3 (Use Cases):** API automation, living documentation, rapid prototyping, AI tool servers, internal CLI tools.

The README markets Layer 3 use cases (API calls, CLI tools) without explaining Layer 1 (why YAML? why executable docs?).
The PM/sales overviews market Layer 1 (executable specifications) without demonstrating Layer 2 and 3 (what can you
actually DO with it?). The developer and agent overviews try to cover all three but end up as reference documents rather
than narratives.

A unified message would flow from Layer 1 → 2 → 3: here's what SpecScript IS (executable YAML/Markdown), here's what
it CAN DO (HTTP, testing, MCP, CLI), and here's WHERE YOU'D USE IT (API automation, living docs, AI tools).

## Recommendation: Two Decisions to Make

### Decision 1: Lead with Which Layer?

**Option A — Lead with Layer 3 (practical use cases), like the current README.**
Pro: Immediately relatable. Shows what you can do. Hooks developers.
Con: Buries the differentiator. Sounds like "yet another scripting tool."

**Option B — Lead with Layer 1 (executable specifications), like the PM overview.**
Pro: Leads with the unique value proposition. Nothing else does this.
Con: Abstract. Harder to hook someone who just wants to call an API.

**Option C — Lead with both, interleaved.** Open with a practical example (GET in one line), then immediately
explain WHY it's different (it's a specification that executes), then show more capabilities. The current README
does A→A→A→...→B at the very end. A better flow might be A→B→A→B.

### Decision 2: What to Do with the Overview Documents

The four overview docs are all marked `ai-generated: true, human-edited: false`. Options:

1. **Keep all four, polish them.** Each audience gets a tailored pitch.
2. **Merge PM + Sales into one "stakeholder" overview.** They're 80% the same already.
3. **Merge all four into a single "What is SpecScript?" page** with audience-specific sections.
4. **Kill the overviews and put the good stuff into the README.** The README is the front door; everything else
   is secondary.
