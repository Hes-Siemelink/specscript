# Agent ideas

Concise one-liners for potential improvements, to be reviewed later.

- Kotlin test runner leaks recorded answers across test cases within a file (shared context/session), while the TypeScript runner isolates them per case — align Kotlin to reset answers between test cases so specs can't accidentally rely on cross-test leakage. Surfaced by the Prompt "Multiple questions" test resolving `sum` from an earlier test's answer.
