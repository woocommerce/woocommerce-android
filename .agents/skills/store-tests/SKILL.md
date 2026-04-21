---
name: store-tests
description: Main app unit testing patterns (BaseUnitTest, BDD naming, mockito-kotlin, AssertJ, StateFlow/LiveData capture, coroutine dispatchers). Use when writing, editing, exploring, debugging, or reviewing unit tests in the store management app. For POS (WooPos*) tests, use the `pos-tests` skill instead.
allowed-tools: Bash, Read, Write, Edit, Grep, Glob
user-invocable: true
---

# Store App Unit Tests

@docs/store-testing.md

## Workflow

1. Read the class under test to understand its dependencies, public API, and state management
2. Find existing tests in the same package/feature for patterns to follow
3. Determine test location: mirror main source path under `src/test/`
4. Write the test class following the conventions in the testing doc above
5. Run the test: `./gradlew :WooCommerce:testWasabiDebugUnitTest --tests "*.ClassName"`
   - For libs: `./gradlew :libs:<module>:testDebugUnitTest --tests "*.ClassName"`
