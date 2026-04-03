# SpecScript Porting Process

How the TypeScript porting exercise was conducted, documented as a reusable process for porting SpecScript to another
language. This describes the agent-assisted workflow and the human-agent collaboration loop.

## Overview

Porting SpecScript to a new language is a structured exercise that combines the spec-first philosophy with AI agent
collaboration. The process was validated during the TypeScript port (Mar 25–28, 2026), producing a working implementation
covering Levels 0–5 with 397 passing spec tests.

The process has three macro-phases: **Planning**, **Implementation**, and **Review**.

## Prerequisites

Before starting:

1. Read `specification/levels.yaml` — defines the level system and which commands/files belong to each level.
2. Read the relevant `specification/` documents starting with `specification/overview/`.
3. Familiarize yourself with the Kotlin reference implementation in `src/main/kotlin/specscript/`.
4. Read `plan/proposals/language-levels.md` — explains the rationale behind the level decomposition.
5. Read `plan/proposals/go-implementer-guide.md` — implementation-specific guidance.

## Phase 1: Planning

### 1.1 Language Levels Proposal

Before touching code, produce a high-level plan that maps the levels to the target language. Store it in
`plan/proposals/`.

**Agent creates**: `plan/proposals/{language}-implementation.md`
- Project structure (where does the new code live?)
- Data model mapping (how do you represent JSON values?)
- Key architectural decisions (sync vs async, error handling, concurrency model)
- Estimated timeline per level

**Human reviews and confirms.** Do not proceed without explicit go-ahead.

### 1.2 Work Breakdown in Beans

Create a bean hierarchy:

```
milestone: {Language} SpecScript implementation
  └── epic: Level {N} implementation (one per level)
       └── feature/task: individual commands or concerns
```

The TypeScript port used flat feature beans per level (e.g., "TypeScript Level 0 implementation"). Either approach works.

**Key**: Create beans BEFORE starting work. Update them as work progresses. This gives the human visibility into what's
happening and creates an audit trail.

## Phase 2: Implementation

### The Level Loop

For each level (0 through 6), repeat this cycle:

#### 2.1 Level Plan

**Agent creates**: `plan/proposals/{language}-level-{N}-plan.md`
- List of commands to implement with their handler types (handlesLists, delayedResolver, errorHandler)
- Internal dependencies between commands
- Known gotchas from the Go implementer guide
- Test file list (from `levels.yaml`)
- Estimated lines of code

**Human confirms** or adjusts scope.

#### 2.2 Implementation

Agent implements level N. Key rules:

- Follow the test file list in `levels.yaml` — those are the spec tests that must pass
- Start with the simplest commands, build up to complex ones
- Run the test suite frequently (every few commands)
- When a spec test reveals a behavior that isn't documented, note it for the report
- If a spec test seems wrong (testing implementation quirks rather than language behavior), note it but match the
  behavior anyway — the spec is the spec

**Commit cadence**: One commit per level is fine for early levels. For larger levels (Level 1 has 22 commands), multiple
commits are acceptable.

**Bean updates**: Check off todo items as commands are implemented. This is critical for visibility.

#### 2.3 Level Report

After each level passes its tests:

**Agent creates**: `plan/proposals/level-{N}-implementation-report.md`
- Test results (X passing, Y skipped, Z failing)
- Bugs found in the spec or reference implementation
- Surprises and gotchas encountered
- Architecture decisions made
- Deviations from the reference implementation (and why)

The report is high-level and focuses on observations about SpecScript itself, not code statistics. The human can read git
diffs for code details.

**Human reviews** the report and the code (via git diff). May request:
- Different implementation choices
- Bug fixes
- Additional test coverage
- Clarifications

#### 2.4 Review and Commit

After human review:
- Fix any issues raised
- Prepare commit according to git commit rules in AGENTS.md
- Human confirms commit message
- Agent commits; human pushes

Move completed plans and reports to `plan/completed/` (or leave them — they're historical artifacts).

### Cross-Level Tasks

Some tasks span multiple levels:

- **Async retrofit** (TypeScript-specific): After Level 4, the synchronous HTTP architecture was replaced with async.
  This kind of architectural revision should get its own bean and report.
- **CLI implementation**: Needed components from Levels 0, 3, and 4. Can be done as a post-levels task.
- **Test coverage expansion**: Adding spec.md files to the test runner (beyond the initial .spec.yaml files) is a
  separate task that can expand coverage significantly.

## Phase 3: Review

### 3.1 Gap Analysis

Once all target levels are implemented:

**Agent creates**: gap analysis comparing the new implementation against the Kotlin reference.
- Missing commands
- Missing spec files in the test runner
- Behavioral differences

This was done as bean `specscript-t3z3` in the TypeScript port.

### 3.2 Retrospective Documents

Three deliverables:

1. **Implementer guide for the next language** — Architecture mapping, gotchas, level-by-level notes.
2. **Language designer lessons learned** — Spec gaps, surprising behaviors, improvement suggestions.
3. **Process description** — This document.

### 3.3 Kotlin Improvements

The porting exercise will uncover bugs and design issues in the Kotlin reference. These should be:

- Fixed in Kotlin when they're clearly bugs (e.g., Exit propagation, alignment bug)
- Documented as spec changes when they're design decisions (e.g., Do command list handling)
- Tracked as beans for future consideration when they're larger changes

## Timeline Reference

The TypeScript port timeline (for calibration):

| Day | Work |
|-----|------|
| Day 1 (afternoon) | Language levels proposal, project scaffold, Level 0 (37 tests) |
| Day 1→2 (overnight) | Level 1 (101 tests) |
| Day 2 (morning) | Level 2 (113 tests), Level 3 (179 tests), Level 4 (226 tests) |
| Day 2 (afternoon) | Async retrofit |
| Day 3 (morning) | CLI, bug fixes, test expansion (341 tests) |
| Day 3 (afternoon) | Check type, Prompt commands, Kotlin backports (397 tests) |
| Day 4 | Retrospective and cleanup |

Total: ~3.5 days for Levels 0–5, CLI, and stabilization, with AI agent assistance. A Go port would likely be faster
(goroutines eliminate the async complexity, strong typing catches errors earlier).

## Artifacts Produced

By the end of the process, the following should exist:

```
plan/proposals/
  {language}-implementation.md          # Initial plan
  {language}-level-{0..N}-plan.md       # Per-level plans
  level-{0..N}-implementation-report.md # Per-level reports
  {language}-*.md                       # Ad-hoc proposals (async retrofit, CLI edge cases, etc.)
  go-implementer-guide.md              # (or next-language guide)
  language-designer-lessons-learned.md  # Language improvement suggestions

.beans/
  {various beans tracking all work}

{language}/
  src/                                  # Implementation source code
  test/                                 # Test harness
  package.json / go.mod / etc.          # Build configuration
```

## Key Principles

1. **Spec is truth**: When in doubt, match the spec test behavior. Even if the spec seems wrong, the spec is the spec.
   Document disagreements in the report.
2. **One level at a time**: Don't skip ahead. Each level builds on the previous.
3. **Test early, test often**: Run the spec test suite after every few commands. Catching regressions early saves hours.
4. **Reports over code reviews**: The human reads reports for architecture and observations, git diffs for code. The
   report should tell them what to look for in the diff.
5. **Beans for visibility**: Every piece of work gets a bean. Check off items as you go. The human should be able to run
   `beans list` and see exactly where things stand.
6. **Plans before code**: Write the level plan, get confirmation, then implement. This prevents rework.
7. **Backport improvements**: When the new implementation finds a better pattern, backport it to Kotlin. The porting
   exercise improves both implementations.
