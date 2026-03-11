---
status: draft
ai-generated: true
complete-garbage: false
human-edited: false
---

# SpecScript -- Overview for Product Managers

## The Problem

In most software projects, three things drift apart over time: the **specification** (what the system should do), the
**documentation** (what we tell people it does), and the **tests** (what we verify it actually does). Keeping them in
sync is a constant, losing battle. Outdated docs mislead teams. Specs gather dust in wikis. Tests exist in code nobody
outside engineering reads.

## What SpecScript Does

SpecScript collapses these three things into one artifact. A single file is simultaneously:

- A **human-readable specification** describing behavior
- A **runnable script** that executes the described behavior
- An **automated test** that verifies correctness

If someone changes the system and the documentation becomes wrong, the build fails. Documentation literally cannot lie.

## How It Works (No Code Knowledge Required)

SpecScript files are written in plain YAML or Markdown -- formats that are widely used and easy to read. Here's a
complete script that calls an API and validates the response:

```yaml
Script info: User greeting API

Input schema:
  type: object
  properties:
    name:
      description: User name
    language:
      description: Greeting language

POST:
  url: http://api.example.com/greeting
  body:
    name: ${input.name}
    language: ${input.language}
```

This file does four things at once:

1. Documents the API contract (what endpoint, what parameters)
2. Defines a runnable command with `--name` and `--language` options
3. Generates `--help` text automatically from the descriptions
4. Serves as a test case when run in test mode

## Key Value Propositions

### Living Documentation

Every code example in the specification executes during the build. If behavior changes, the spec breaks. This eliminates
the "documentation is outdated" problem entirely.

### Stakeholder-Readable Tests

Unlike traditional test code (`assertEquals(response.status, 200)`), SpecScript tests read like plain English
descriptions. Product managers, QA, and business analysts can review and understand them directly.

### Rapid Prototyping

Need to demonstrate an API flow? SpecScript can spin up a mock HTTP server, define endpoints, and run interactive
scripts -- all in YAML, no application code needed. Time from idea to working prototype is measured in minutes.

### AI-Ready (MCP Servers)

SpecScript can define Model Context Protocol (MCP) servers -- the emerging standard for connecting AI assistants to
tools and data. Teams can create AI tool integrations in YAML without writing application code.

### Subcommand-Based CLI

Organize scripts in directories and SpecScript automatically provides subcommand navigation, `--help` generation, and
interactive selection. This means internal tools and workflows can be built with zero UI effort.

## Who Benefits

| Role                  | Benefit                                                         |
|-----------------------|-----------------------------------------------------------------|
| **Product Managers**  | Read and validate specifications directly; no translation layer |
| **Developers**        | Write docs and tests simultaneously; spec-driven development    |
| **QA Engineers**      | Readable test specs; executable acceptance criteria             |
| **Technical Writers** | Documentation that's guaranteed correct                         |
| **DevOps / Platform** | Scriptable API interactions and server mocks                    |

## The Core Idea in One Sentence

SpecScript makes specifications executable, so that documentation, tests, and behavior can never disagree.
