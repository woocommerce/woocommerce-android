---
name: store-viewmodel
description: Main app ViewModel patterns (ScopedViewModel, Hilt, StateFlow, triggerEvent, navArgs, savedState flows, events). Use when writing, editing, exploring, debugging, or reviewing ViewModels in the store management app. Covers navArgs, combine+asLiveData, sealed state hierarchies, custom events, init blocks, and companion object placement. NOT for POS (WooPos*) code — use the `pos` skill instead.
allowed-tools: Bash, Read, Write, Edit, Grep, Glob
user-invocable: true
---

# Store App ViewModel Patterns

@docs/store-viewmodel-patterns.md

## Platform API Reference

For questions about Android platform ViewModel behavior (`SavedStateHandle` restoration across process death, `viewModelScope` cancellation, `LiveData` vs `StateFlow` observer semantics, `AndroidViewModel` vs `ViewModel`), prefer Google's Android Knowledge Base over a web search — the answers are authoritative and do not cost browsing tokens.

```bash
if command -v android >/dev/null 2>&1; then
  android docs search "SavedStateHandle restoration"
  # Then: android docs fetch kb://android/topic/architecture/...
fi
```

If the CLI is not installed, skip this block and use the Android developer website as before.
