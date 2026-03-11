---
status: draft
ai-generated: true
complete-garbage: false
human-edited: false
review-notes:
  - Merge with PM overview
---

# SpecScript -- Overview for Sales

## Elevator Pitch

SpecScript turns specifications into running software. Write what your system should do in plain YAML or Markdown, and
SpecScript makes it executable -- as documentation, as tests, and as live prototypes. One file, three deliverables,
always in sync.

## The Problem We Solve

Every software organization struggles with the same issue: **documentation goes stale**. Specifications are written in
Word or Confluence, tests are written in code, and over time they diverge. The result is confusion, bugs, and wasted
effort reconciling what the system is *supposed* to do versus what it *actually* does.

SpecScript eliminates this problem by making specifications executable. If the documentation says the API returns a
greeting, that claim is verified every time the build runs. If it stops being true, the build breaks. **Documentation
that can't lie.**

## What Makes SpecScript Different

### 1. Readable by Everyone

SpecScript uses YAML and Markdown -- formats that business stakeholders, product managers, and QA teams already know. No
programming expertise required to read or review specifications.

### 2. Executable Out of the Box

Every specification is a runnable script. A file that describes an API call actually makes that API call. A file that
defines test expectations actually validates them. There is no gap between "spec" and "implementation."

### 3. Instant Prototyping

Need to demo an API integration? SpecScript can spin up mock HTTP servers, define endpoints, and produce interactive
command-line tools -- all without writing application code. Go from concept to working demo in minutes.

### 4. AI-Native

SpecScript supports the Model Context Protocol (MCP), the emerging standard for connecting AI assistants like Claude or
ChatGPT to tools and data. Teams can define AI tool integrations in simple YAML, making SpecScript a fast path to
AI-powered workflows.

### 5. Built-in Testing

Test assertions, HTTP validation, data checks, and user interaction flows are all first-class features. Teams get a
complete testing framework without adopting a separate tool.

## Use Cases

**API Specification & Testing** -- Define REST API contracts in YAML. Run them to validate real endpoints. Use them as
living documentation that stays current.

**Compliance & Audit** -- Executable specifications provide verifiable evidence that systems behave as documented. If
the spec runs green, the system matches the documentation.

**Developer Onboarding** -- New team members read specifications that they can also run. Understanding and validation
happen in the same step.

**Internal Tooling** -- Build interactive CLI tools for operations teams. Define input parameters, user prompts, API
calls, and output formatting -- all without a traditional development cycle.

**AI Tool Development** -- Define MCP servers that expose tools, resources, and prompts to AI assistants. Prototype AI
integrations without writing backend code.

## Competitive Positioning

| Approach                              | Documentation | Tests        | Executable | Readable by Non-Devs |
|---------------------------------------|---------------|--------------|------------|----------------------|
| Traditional (Word/Confluence + JUnit) | Separate      | Separate     | No         | Docs only            |
| Swagger/OpenAPI                       | API docs      | Limited      | No         | Partially            |
| Postman                               | Collections   | Via runner   | Partially  | Partially            |
| **SpecScript**                        | **Built-in**  | **Built-in** | **Yes**    | **Yes**              |

## Key Metrics to Highlight

- **440+ specification tests** validate SpecScript's own behavior -- the product eats its own dog food.
- **56+ built-in commands** covering HTTP, testing, control flow, data manipulation, user interaction, file I/O, shell
  execution, and AI/MCP integration.
- **Zero-config CLI generation** -- input parameters automatically become command-line options with help text.
- **JVM-based** -- runs anywhere Java runs. Single-file deployment (fat JAR).

## The Core Message

SpecScript is for organizations that are tired of documentation that's wrong, specs that nobody reads, and tests that
nobody outside engineering understands. It unifies all three into a single, human-readable, executable artifact.

**Everything is Markdown and YAML. Keep it simple.**
