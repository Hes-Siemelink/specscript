# IntelliJ Test Integration — Investigation Report

## Problem

IntelliJ cannot run or rerun individual specification tests:

- Right-clicking `SpecScriptTestSuite.kt` and selecting Run says "No tests were found"
- Running via Gradle config (`:specificationTest --tests "specscript.spec.SpecScriptTestSuite"`) works but shows "Test
  events were not received" — cannot navigate to tests, rerun individual cases, or see results in the test tree
- This worked in the predecessor project (Instacli)

## What Was Tried

1. **Added URIs to DynamicContainers** — changed `dynamicContainer(it.name, tests)` to
   `dynamicContainer(it.name, it.toUri(), tests.stream())`. No effect on IntelliJ integration.

2. **Added `useJUnitJupiter()` to specificationTest suite** in `build.gradle.kts`. The `test` suite had it, but
   `specificationTest` did not. No effect.

3. **Deleted stale `.idea/modules.xml` and `.iml` files** that instacli didn't have. No effect.

4. **Verified `testRunner="PLATFORM"`** in `.idea/gradle.xml` — IntelliJ is configured to use its native JUnit runner,
   not Gradle. Still says "No tests were found".

## Key Differences Between Working (Instacli) and Broken (SpecScript)

### Test infrastructure location

- **Instacli**: `TestUtil.kt` lives in `src/tests/specification/` (the specificationTest source set). JUnit deps are
  `testImplementation`/`testRuntimeOnly`.
- **SpecScript**: `TestUtil.kt` lives in `src/main/`. JUnit deps are `implementation`/`runtimeOnly` on main so that
  TestUtil compiles. The specificationTest suite depends on the main project via `implementation(project())`.

This is the most likely root cause. IntelliJ may not detect JUnit as a test framework in the `specificationTest` module
because the JUnit dependency comes transitively through the main project, not directly as a test dependency in the test
module's own configuration.

### Gradle config

- Instacli's `specificationTest` suite does NOT have `useJUnitJupiter()` — and it works.
- Instacli's JUnit deps are `testImplementation` — test-scoped.
- Both projects have the same Gradle testing DSL structure.

### IntelliJ config

- Instacli run configs are all `type="JUnit"` — native JUnit runner.
- SpecScript run configs for spec tests are `type="GradleRunConfiguration"`.
- Instacli has no `modules.xml` or manual `.iml` files.

## Hypotheses Not Yet Tested

1. **Move TestUtil.kt back to the specificationTest source set** and change JUnit deps back to
   `testImplementation`/`testRuntimeOnly`. This would match the instacli setup exactly. The question is whether the main
   source set still needs the JUnit API (it uses `DynamicTest`, `DynamicContainer`, etc. in TestUtil).

2. **Check if main source set depending on JUnit confuses IntelliJ's test framework detection** — IntelliJ might see
   JUnit in the main classpath and not treat it as a test framework for the specificationTest module.

3. **Try the IntelliJ JUnit runner manually** — create a JUnit run config by hand (not through right-click) targeting
   `specscript.spec.SpecScriptTestSuite` in the `specscript.specificationTest` module. This would bypass IntelliJ's
   auto-detection.

4. **Invalidate IntelliJ caches** (File > Invalidate Caches and Restart) — stale indexes might prevent test discovery.

## Recommendation

The most promising fix is hypothesis #1: move `TestUtil.kt` (and related test infrastructure) back into the
`specificationTest` source set, matching the instacli layout. This would restore the normal dependency structure where
JUnit is a test-scoped dependency and IntelliJ can properly detect the test framework.
