---
name: test
description: Run tests for the project, a specific module, or a specific class
allowed-tools: Bash, Read, Grep, Glob
user-invocable: true
---

# Run Tests

Run unit tests with scoped targeting based on the provided arguments.

## Determine Scope from $ARGUMENTS

- **No arguments:** Run the full test suite for the main app.
  ```
  ./gradlew :WooCommerce:testWasabiDebugUnitTest
  ```

- **Module name** (e.g., `cardreader`, `login`, `fluxc`, `pos`, `commons`): Run that module's tests.
  ```
  ./gradlew :libs:<module>:testDebugUnitTest
  ```

- **Test class name** (e.g., `OrderListViewModelTest`): Search for the test file first, then run it.
  ```
  # Find the test file to determine the module
  find . -name "<ClassName>.kt" -path "*/test/*"

  # Run the specific test class (main app example)
  ./gradlew :WooCommerce:testWasabiDebugUnitTest --tests "*.<ClassName>"

  # Run the specific test class (library module example)
  ./gradlew :libs:<module>:testDebugUnitTest --tests "*.<ClassName>"
  ```

- **Test method name** (e.g., `"when refresh is pulled, then data reloads"`): Run the specific method.
  ```
  ./gradlew :WooCommerce:testWasabiDebugUnitTest --tests "*.<ClassName>.<methodName>"
  ```

## After Running

1. **Report results:** Pass/fail count and overall status.
2. **On failure:** Show the failing test names, read the failing test file, and read the corresponding source file.
3. **Suggest fixes:** Based on common patterns:
   - Missing `whenever()` setup → add mock stub
   - `NullPointerException` → check mock initialization
   - Assertion mismatch → compare expected vs actual state
   - Timeout/hang → check for missing `runCurrent()` or `testBlocking`
