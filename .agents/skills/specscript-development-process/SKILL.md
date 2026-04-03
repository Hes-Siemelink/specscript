---
name: specscript-development-process
description: End-to-end workflow for developing SpecScript features, from proposal through implementation to review. Covers the planning-implementation-review loop, bean hierarchy, reporting, and commit flow. Use when starting a new feature, planning a multi-step change, or when the user asks how development should be organized.
compatibility: Requires beans CLI for issue tracking and the specscript project structure.
metadata:
  author: specscript
  version: "1.0"
---

## Overview

This skill describes the full development loop for SpecScript features. It extends the 8-step process in AGENTS.md with
operational detail on how to organize work, report progress, and collaborate with the human reviewer.

The loop: **Propose → Plan → Implement → Report → Review → Commit**.

## When to Use

- Starting a new feature or significant change
- Planning work that spans multiple commands or spec files
- Multi-day work where tracking and visibility matter
- When the user says "let's build X" or "implement Y"

Do NOT use for trivial fixes (typos, one-line changes, single-file edits). Just do those directly.

## Phase 1: Propose

Write a proposal in `plan/proposals/`. Plain Markdown, not SpecScript.

Contents:
- Problem statement (what and why)
- Proposed solution (how, at a high level)
- Scope boundaries (what's in, what's explicitly out)
- Impact on existing specs or commands (if any)

**Stop and wait for human confirmation.** Do not proceed without explicit go-ahead.

For bug fixes: skip the proposal. Create a bean directly and describe the bug there.

## Phase 2: Plan

### Create beans

Use the bean type hierarchy:

```
milestone    → target release or checkpoint
  epic       → thematic container (don't work on epics directly)
    feature  → user-facing capability
      task   → concrete piece of work
      bug    → something broken
```

For a single feature: create one `feature` bean with todo items in the body.
For a larger effort: create an `epic` with child `task`/`feature` beans.

```bash
beans create "Feature title" -t feature -d "Description" -s todo
```

Always create beans BEFORE starting work. Update todo items as work progresses:

```bash
beans update <id> --body-replace-old "- [ ] Step 1" --body-replace-new "- [x] Step 1"
```

### Write the spec (spec-first)

This is the most critical step. Write or update the specification BEFORE implementing code.

- Spec goes in `specification/` (appropriate subdirectory)
- For invasive changes, put draft specs in `plan/draft-specs`
- Follow the specification writing style in AGENTS.md
- One executable example per concept, edge cases go in `tests/` files
- Mock with `Output:` commands for early iteration before implementing

Run `./gradlew specificationTest` — tests SHOULD fail at this point (spec written, implementation not yet done).

**Stop and wait for human confirmation of the spec.**

## Phase 3: Implement

Follow existing patterns. Do not introduce new architectural styles without confirmation.

Key rules:
- Start with the simplest part, build up to complex
- Run tests frequently: `./gradlew specificationTest`
- When a spec test reveals undocumented behavior, note it for the report
- Put improvement suggestions in `plan/agent-ideas.md` as one-liners
- Check off bean todo items as work completes

### Commit cadence

- Small features: one commit after human sign-off
- Larger efforts: commit at natural boundaries (e.g., per phase, per sub-feature)
- Always follow git commit rules in AGENTS.md
- Always ask for confirmation before committing

## Phase 4: Report

After implementation is complete and tests pass, write a brief report. This can go in the bean body
(`--body-append`) or as a separate file in `plan/proposals/` for larger efforts.

Report contents:
- Test results (passing, skipped, failing)
- Observations about SpecScript itself (spec gaps, surprising behaviors)
- Architecture decisions made and why
- Anything the reviewer should look for in the diff

The report is HIGH-LEVEL. The human reads git diffs for code detail. Focus on:
- What might surprise them
- What you'd do differently next time
- What this work revealed about the language

Do NOT pad the report with code statistics, file counts, or line-by-line change descriptions.

## Phase 5: Review

Present the report to the human. They will:
- Read the report
- Check git diffs
- Possibly request changes, different implementation choices, or bug fixes

Handle review feedback:
- Fix issues raised
- Update the bean and/or report
- Re-run tests after changes

This phase may have multiple rounds. Stay focused on the current feedback — do not revisit resolved topics.

## Phase 6: Commit and Close

After human sign-off:

1. Prepare commit per git commit rules in AGENTS.md
2. Present commit message for confirmation
3. Commit (human pushes)
4. Mark bean as completed with a `## Summary of Changes` section
5. Move completed plans to `plan/completed/` if applicable
6. Offer to create follow-up beans for deferred work


