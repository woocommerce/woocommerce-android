---
name: lint
description: Run detekt static analysis on the project
allowed-tools: Bash, Read, Grep, Glob
user-invocable: true
---

# Run Detekt Lint

Run detekt static analysis with optional auto-correction.

## Determine Mode from $ARGUMENTS

- **No arguments or "check":** Run detekt in check-only mode.
  ```
  ./gradlew detektAll
  ```

- **"fix" or "auto-correct":** Run detekt with auto-correction enabled.
  ```
  ./gradlew detektAll --auto-correct
  ```

## After Running

1. **Report results:**
   - Total violation count
   - Violations grouped by rule name
   - File paths and line numbers for each violation

2. **For violations that cannot be auto-corrected:**
   - Read the violating file
   - Explain the rule being violated
   - Suggest a specific fix

3. **Common detekt rules in this project:**
   - `MaxLineLength` — 120 characters (disabled for test names)
   - `LongParameterList` — ignored for `@Inject` and `@Composable` constructors
   - `LongMethod` — ignored for `@Composable` functions
   - `FunctionNaming` — ignored for `@Composable` functions
   - `MagicNumber` — ignored in enums, properties, and `@Composable` functions
   - `ReturnCount` — max 4 returns per function

4. **HTML report location:** `WooCommerce/build/reports/detekt/detekt.html`
