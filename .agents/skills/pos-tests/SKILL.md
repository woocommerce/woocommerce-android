---
name: pos-tests
description: POS unit testing patterns (WooPosCoroutineTestRule, runTest, advanceUntilIdle, mockito-kotlin, event bus mocking, analytics verification). Use when writing, editing, exploring, debugging, or reviewing unit tests for POS (WooPos*) code. NOT for main store app tests — use the `store-tests` skill instead.
allowed-tools: Bash, Read, Write, Edit, Grep, Glob
user-invocable: true
---

# POS Unit Tests

@docs/pos-testing.md

## Workflow

1. Read the class under test to understand its dependencies and state management
2. Find existing tests in the same package for patterns to follow
3. Test location: mirror main source path under `src/test/` within `ui/woopos/`
4. Write the test class following the POS conventions in the testing doc above
5. Run the test: `./gradlew :WooCommerce:testWasabiDebugUnitTest --tests "*.ClassName"`
