---
name: specscript-changelog
description: Extracts changelog from git history between SpecScript releases. Uses 💫 emoji convention in commit messages to identify user-facing changes. Use when the user asks about changes between versions, release notes, or what changed since the last release.
metadata:
  author: specscript
  version: "1.0"
---

## Overview

SpecScript does not maintain a CHANGELOG file. The git commit history IS the changelog, using emoji conventions in
commit summaries.

## Commit conventions

| Prefix | Meaning |
|--------|---------|
| `💫` | User-facing feature or change |
| `💫 ⚠️` | Breaking change |
| `Bug fix` | Bug fix |
| `🚀 SpecScript X.Y.Z` | Release commit |

## How to extract a changelog

Between two versions:

```sh
git log --format="%s" 0.7.0..0.8.0
```

Features and breaking changes only:

```sh
git log --format="%s" --grep="💫" 0.7.0..0.8.0
```

Since the last release (when there is no tag yet for the new version):

```sh
git log --format="%s" --grep="💫" $(git tag -l --sort=-version:refname | head -1)..HEAD
```

## Presentation

Present the changelog grouped by type:

1. **Breaking changes** — lines containing `💫 ⚠️` (strip the emojis for readability)
2. **New features / changes** — lines containing `💫` but not `⚠️`
3. **Bug fixes** — lines matching `Bug fix`

Omit categories that have no entries. Strip emoji prefixes when presenting to the user.
