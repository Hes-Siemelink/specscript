---
name: specscript-sample-readme
description: Creates README documentation and test infrastructure for SpecScript sample projects. Covers mock server setup with recorded API data, test files with Before/After blocks, connection overrides, and directory layout conventions. Use when writing or improving documentation for samples, or when the user mentions sample README, mock server for samples, or testable examples.
---

## Overview

SpecScript sample projects live under `samples/`. Each sample should have a README explaining what it does, a `tests/`
directory with a mock server and automated tests, and a `specscript-config.yaml` for configuration.

This skill describes the reference pattern established in `samples/digitalai/release/folders/`.

## Directory Layout

```
samples/<vendor>/<product>/<feature>/
  README.md                          # Documentation with CLI usage examples
  specscript-config.yaml             # Script info, no connection config here
  <command>.spec.yaml                # The actual commands
  tests/
    specscript-config.yaml           # Imports + connection override to mock
    mock-server.spec.yaml            # Standalone mock server
    <feature>-tests.spec.yaml        # Test cases
    recorded-data/                   # Recorded API responses
      <api-path>/<query>/<METHOD>    # One file per endpoint response
```

## Step 1: Record API Responses

Capture real API responses and save them as files. The directory structure mirrors the URL path:

```
tests/recorded-data/api/v1/folders/list/depth=10/GET
```

- Path segments become directories
- Query string (without `?`) becomes a directory name
- HTTP method (`GET`, `POST`) is the filename
- File content is the raw JSON response

Clean up the recorded data: simplify IDs, use meaningful names, minimize the dataset to what the tests need.

## Step 2: Create the Mock Server

Create `tests/mock-server.spec.yaml` as a standalone file:

```yaml
Script info:
  description: Mock <product> server for testing
  hidden: true

Http server:
  name: <product>-mock
  port: <port>
  endpoints:
    "{...}":
      get:
        script:
          Read file:
            resource: recorded-data/${request.path}/${request.query}/GET
      post:
        script:
          Output: {}
```

Key details:
- Use `"{...}"` for the catch-all wildcard endpoint (Ktor tailcard syntax). Do NOT use `"*"` — it does not work.
- Port should be unique (25100+ range for samples)
- `resource:` resolves relative to the script's directory (where mock-server.spec.yaml lives)
- For POST endpoints that don't need response data, `Output: {}` returns an empty JSON object
- `hidden: true` prevents the mock server from appearing in CLI listings

## Step 3: Create Test Config

Create `tests/specscript-config.yaml` with imports and connection override:

```yaml
imports:
  ./:
    - mock-server
  <package>:
    - <product>/<feature>/<command1>
    - <product>/<feature>/<command2>

connections:
  <Connection Name>:
    Http request defaults:
      url: http://localhost:<port>
```

- Import the mock server from the local directory (`./`)
- Import commands from the package (the `samples` directory is a package when using `-p samples`)
- Override the connection to point at the mock server URL

## Step 4: Write Tests

Create `tests/<feature>-tests.spec.yaml`:

```yaml
Script info:
  description: Tests for <feature> commands
  hidden: true

Before all tests:
  Http server:
    name: <product>-mock
    port: <port>
    endpoints:
      "{...}":
        get:
          script:
            Read file:
              resource: recorded-data/${request.path}/${request.query}/GET
        post:
          script:
            Output: {}

Tests:

  <Test name>:
    <Command>: {}
    Expected output:
      - expected item 1
      - expected item 2

After all tests:
  Stop http server: <product>-mock
```

Important:
- The mock server definition is duplicated in `Before all tests:` (inline, not via `Run script:`)
- `Before all tests:` runs only with the first test; `After all tests:` runs only with the last
- Use `Expected output:` for checking command return values (lists, objects)
- Use `Expected console output:` for checking printed text
- For interactive commands, use `Answers:` to provide input for `Prompt` commands
- For stateful operations (e.g., move), verify the command runs without error rather than asserting on post-mutation state

## Step 5: Write the README

The README is plain Markdown documentation (not executable). Include:

1. One-line description of what the sample does
2. Setup section pointing to credentials/connection setup
3. One section per command with CLI usage example and sample output
4. Testing section explaining how to run `spec -t -p samples tests`
5. Files table listing all scripts with descriptions

Use `shell ignore` code blocks for CLI examples (not executed by SpecScript).

## Running Tests

```shell
spec -t -p samples samples/<vendor>/<product>/<feature>/tests
```

- `-t` enables test mode
- `-p samples` sets the package path so imports resolve correctly
- The path points to the `tests/` directory

## Rules

- Port numbers: use 25100+ range, check existing samples for conflicts
- Connection names must match exactly what the commands use in `Connect to:`
- Recorded data should be minimal — only include what tests actually need
- Mock server must be stoppable (use `Stop http server:` in `After all tests:`)
- Do NOT use `"*"` as wildcard path — use `"{...}"` (Ktor tailcard syntax)
- Keep test data deterministic — no timestamps, random IDs, or environment-dependent values
