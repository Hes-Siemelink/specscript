# Agent ideas

Concise one-liners for potential improvements, to be reviewed later.

- Kotlin test runner leaks recorded answers across test cases within a file (shared context/session), while the
  TypeScript runner isolates them per case — align Kotlin to reset answers between test cases so specs can't
  accidentally rely on cross-test leakage. Surfaced by the Prompt "Multiple questions" test resolving `sum` from an
  earlier test's answer.
- Finish the x-default-property built-in sweep from plan/proposals/x-default-property.md §4 (Shorthand support-table
  row + schema annotation). Only GET and Run were done as flagship examples; the rest need per-command decisions because
  the proposal's list conflicts with current tables: POST/DELETE show Value=no, Cli/Read file show Object=no, Expected
  error shows Object=no, and Prompt uses a special resolver. Resolve those before annotating Cli/Shell/Read file/Write
  file/Temp file/Error/Prompt (Error/Expected error also lack a properties block, so shorthand=message needs one added).
