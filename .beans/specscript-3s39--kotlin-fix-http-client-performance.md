---
# specscript-3s39
title: 'Kotlin: Fix HTTP client performance'
status: completed
type: bug
priority: normal
created_at: 2026-04-01T20:16:31Z
updated_at: 2026-04-01T20:28:30Z
---

Each HTTP request creates a new Ktor HttpClient instance (expensive: ServiceLoader, thread pool, SSL context). 6.2s vs 0.2s TypeScript for the same folder listing script. Fix by either reusing the client or switching to a lighter HTTP implementation.

## Implementation Plan\n\n- [x] Replace Ktor client with java.net.http.HttpClient in commands/http/HttpClient.kt\n- [x] Keep io.ktor.http.HttpMethod (shared with server)\n- [x] Keep Ktor client deps for MCP transport (cannot remove)\n- [x] Run specification tests\n- [x] Run the folder listing benchmark

## Summary of Changes

Root cause was two-fold:
1. Java IPv6 DNS lookup for .local domains triggers macOS mDNS with 5s timeout. Fixed by setting java.net.preferIPv4Stack=true at startup.
2. Ktor HttpClient created per request (leaked resources, unnecessary overhead). Replaced with java.net.http.HttpClient singleton.

Result: 6.0s -> 0.93s (including 0.38s JVM startup).
