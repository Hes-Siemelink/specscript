---
name: release-specscript
description: Releases a new version of SpecScript to GitHub. Handles version bumping, building, GitHub release creation, and post-release snapshot bump. Use when the user wants to release, publish, or cut a new version of SpecScript.
compatibility: Requires Java 21, Gradle, git, and a GITHUB_TOKEN with repo scope.
metadata:
  author: specscript
  version: "1.0"
---

## Overview

SpecScript releases are published to GitHub using the `githubRelease` Gradle task. The process involves three commits:
a version bump, the GitHub release, and a snapshot bump. Two files hold the version: `build.gradle.kts` and
`specscript.conf`.

## Version strategy

SpecScript uses semver with MAJOR always 0 (pre-1.0):

| Level | When to bump |
|-------|-------------|
| MINOR | Any breaking change (look for `💫 ⚠️` in git log since last release) |
| PATCH | Bug fixes, new commands, incremental features |

To determine the correct version, run:

```sh
git log --format="%s" --grep="💫" <last-tag>..HEAD
```

If any line contains `💫 ⚠️`, bump MINOR. Otherwise bump PATCH.

## Prerequisites

Before starting a release, the git working tree must be clean — no uncommitted or untracked changes. Commit or stash
everything first. Run `git status` to verify.

## Release flow

### Step 1: Determine version

1. Find the latest release tag: `git tag -l --sort=-version:refname | head -5`
2. Read current SNAPSHOT version from `build.gradle.kts` line 2
3. Check for breaking changes since last tag (see above)
4. Decide new version number. Confirm with user before proceeding.

### Step 2: Generate changelog and release headline

Extract feature/change list from git history:

```sh
git log --format="%s" --grep="💫" <last-tag>..HEAD
```

Present this to the user as the changelog for the release. No formal CHANGELOG file is maintained — the git history
with 💫 entries serves as the changelog.

Draft a `releaseHeadline` — a concise one-liner summarizing the release themes. Base it on the changelog entries.
Present the proposed headline to the user and confirm before proceeding. Examples of good headlines:

- "Shell/CLI overhaul with breaking changes to defaults and flags"
- "MCP streaming HTTP, test framework, environment variables, and more"
- "Package system, TypeScript engine, and new commands"

### Step 3: Bump version for release

Update both files in a single commit:

- `build.gradle.kts` line 2: change `version = "X.Y.Z-SNAPSHOT"` to `version = "X.Y.Z"`
- `specscript.conf` line 2: change `SPECSCRIPT_VERSION="<old>"` to `SPECSCRIPT_VERSION="X.Y.Z"`

Commit message: `🚀 SpecScript X.Y.Z`

### Step 4: Clean build

```sh
./gradlew clean build
```

This runs all tests (unit + specification). Do not proceed if the build fails.

### Step 5: Push release commit

```sh
git push
```

The commit must be on `main` before the GitHub release is created, because the `githubRelease` task sets
`targetCommitish = "main"` and creates a lightweight tag pointing at `main` HEAD.

### Step 6: Create GitHub release

The `githubRelease` Gradle task requires a `GITHUB_TOKEN` environment variable with `repo` scope. Ask the user for the
token if not set.

```sh
GITHUB_TOKEN=<token> ./gradlew githubRelease -PreleaseHeadline="Short description of this release"
```

The `-PreleaseHeadline` property sets the release body/description on GitHub. Write a concise one-liner summarizing the
release themes (e.g., "MCP streaming HTTP, test framework, environment variables, and more"). If omitted, falls back to
a generic "Release of SpecScript X.Y.Z" message.

This creates a GitHub release with:
- Tag: `X.Y.Z` (created by GitHub API, lightweight)
- Release name: `SpecScript X.Y.Z`
- Body: the headline from `-PreleaseHeadline`
- Assets: `specscript-X.Y.Z.jar` and `specscript-X.Y.Z-full.jar`

### Step 7: Verify

Spot-check that the `spec` wrapper downloads the correct version:

1. Delete the cached jar: `rm ~/.specscript/lib/specscript-X.Y.Z-full.jar` (if it exists)
2. Run `./spec --version` or any simple command
3. Confirm it downloads and runs the new version

### Step 8: Bump to next SNAPSHOT

Update `build.gradle.kts` line 2 to the next PATCH snapshot: `version = "X.Y.(Z+1)-SNAPSHOT"`

Do NOT update `specscript.conf` — it stays at the released version so the wrapper downloads the latest release.

Commit message: `Back to snapshot`

Push: `git push`

## Files that contain versions

Only two files. Do not miss either one during release.

| File | What it holds | Updated at release | Updated for snapshot |
|------|--------------|-------------------|---------------------|
| `build.gradle.kts` line 2 | Build version (`X.Y.Z-SNAPSHOT` or `X.Y.Z`) | Yes | Yes |
| `specscript.conf` line 2 | Version the `spec` wrapper downloads | Yes | No |

## Commit sequence summary

| # | Message | Files changed |
|---|---------|--------------|
| 1 | `🚀 SpecScript X.Y.Z` | `build.gradle.kts`, `specscript.conf` |
| 2 | `Back to snapshot` | `build.gradle.kts` |
