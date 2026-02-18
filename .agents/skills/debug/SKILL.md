---
name: debug
description: Diagnose and fix build or test failures
allowed-tools: Bash, Read, Grep, Glob
user-invocable: true
---

# Debug Build or Test Failures

Diagnose and resolve build compilation errors or test failures.

## For Build Failures

1. **Run the build** to capture the full error output:
   ```
   ./gradlew assembleWasabiDebug 2>&1
   ```

2. **Identify the error.** Look for `e:` prefixed lines (Kotlin compiler errors) or `FAILURE:` summary.

3. **Read the failing file** and its surrounding context.

4. **Common build errors and fixes:**

| Error | Likely Cause | Fix |
|-------|-------------|-----|
| `Unresolved reference` | Missing import or dependency | Add import; check module dependency in `build.gradle` |
| `Type mismatch` | Wrong type passed to function | Check function signature and call site |
| `None of the following candidates is applicable` | Wrong overload or missing argument | Check all required parameters are provided |
| `Hilt: Missing binding` | Dependency not provided in DI | Add `@Provides` or `@Binds` in a Hilt module |
| `Navigation: Cannot find...` | Missing nav graph destination | Add destination to `nav_graph.xml` |
| `Kapt/KSP error` | Annotation processing failure | Check `@HiltViewModel`, Room entities, FluxC annotations |

5. **Propose a fix** with specific code changes.

## For Test Failures

1. **Run the specific failing test:**
   ```
   ./gradlew :WooCommerce:testWasabiDebugUnitTest --tests "*.FailingTestName" 2>&1
   ```

2. **Read the test file**, the class under test, and any relevant mocks.

3. **Common test failure patterns:**

| Symptom | Likely Cause | Fix |
|---------|-------------|-----|
| `NullPointerException` | Mock not initialized or missing stub | Add `whenever(mock.method()).thenReturn(value)` |
| `Wanted but not invoked` | Method not called as expected | Check test setup or verify correct method |
| `Argument(s) are different` | Wrong argument passed | Use `argumentCaptor` or `any()` matcher |
| `Expected <X> but was <Y>` | State not updated correctly | Check ViewModel logic and state flow |
| Test hangs | Missing `runCurrent()` with `StandardTestDispatcher` | Add `runCurrent()` after async operations |
| `No value present` | LiveData not observed | Add `observeForever {}` before triggering action |

4. **Verify the fix** by re-running the specific test.
