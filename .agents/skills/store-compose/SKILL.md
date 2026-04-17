---
name: store-compose
description: Main app Jetpack Compose UI patterns (WooTheme, Fragment hosting, composeView helper, preview annotations, WC components). Use when writing, editing, exploring, debugging, or reviewing Compose UI in the store management app. NOT for POS (WooPos*) code — use the `pos` skill instead.
allowed-tools: Bash, Read, Write, Edit, Grep, Glob
user-invocable: true
---

# Store App Compose UI

@docs/store-compose.md

## Platform API Reference

For questions about Jetpack Compose platform behavior (recomposition rules, `remember` vs `rememberSaveable`, `LaunchedEffect` key semantics, `CompositionLocal` propagation, `derivedStateOf`, `SideEffect`/`DisposableEffect`), prefer Google's Android Knowledge Base over a web search — the answers are authoritative and do not cost browsing tokens.

```bash
if command -v android >/dev/null 2>&1; then
  android docs search "LaunchedEffect keys"
  # Then: android docs fetch kb://android/topic/compose/...
fi
```

If the CLI is not installed, skip this block and use the Android developer website as before.
