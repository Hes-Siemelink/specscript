# SpecScript TypeScript Implementation

TypeScript implementation of the SpecScript language. This is the secondary implementation — the Kotlin version on the `main` branch is the source of truth.

## Prerequisites

- Node.js 20+
- pnpm

## Build

```bash
pnpm install
pnpm run build
```

## Run

```bash
# Via tsx (development)
pnpm start samples/hello.spec.yaml

# Via built output
node dist/cli.js samples/hello.spec.yaml

# Interactive directory selection
node dist/cli.js -i samples
```

To install as a local command:

```bash
mkdir -p ~/.local/bin
echo '#!/bin/bash
exec node /path/to/specscript/typescript/dist/cli.js "$@"' > ~/.local/bin/spec-ts
chmod +x ~/.local/bin/spec-ts
```

## Test

```bash
pnpm test            # Run all tests
pnpm test:watch      # Watch mode
```

Tests execute the specification files from `../specification/` against the TypeScript implementation. Tests are organized in levels matching implementation maturity:

- **Level 1** — Core language: variables, control flow, data manipulation, expressions
- **Level 2** — Markdown documents, testing, eval syntax
- **Level 3** — Files, shell, script composition, directory organization
- **Level 4** — HTTP client, HTTP server

## Project Structure

```
src/
  cli.ts              CLI entry point and directory/file dispatch
  commands/            Command implementations
  language/            Core engine: context, script execution, types
  markdown/            Markdown spec document scanner and converter
  util/                YAML/JSON utilities
test/
  spec-runner.test.ts  Runs specification files as tests
```
