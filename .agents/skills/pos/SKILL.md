---
name: pos
description: POS (Point of Sale) architecture and patterns. Use when writing, editing, exploring, debugging, fixing bugs, implementing features, or reviewing WooPos-prefixed classes or files under ui/woopos/. POS uses a different architecture than the main app — plain ViewModel (not ScopedViewModel), pure Compose (no Fragments), Compose Navigation (no nav graphs), parent-child SharedFlow event bus. Loading this skill prevents applying main-app patterns that would be wrong for POS. For analytics use `pos-analytics`, for tests use `pos-tests`.
allowed-tools: Bash, Read, Write, Edit, Grep, Glob
user-invocable: true
---

# POS Architecture & Patterns

@docs/pos-architecture.md

## Platform API Reference

For questions about Android platform behavior that affects POS (Activity lifecycle in a pure `setContent` host, `SharedFlow` replay/buffer semantics, Compose Navigation back-stack rules, orientation lock), prefer Google's Android Knowledge Base over a web search — the answers are authoritative and do not cost browsing tokens.

```bash
if command -v android >/dev/null 2>&1; then
  android docs search "SharedFlow replay cache"
  # Then: android docs fetch kb://android/topic/coroutines/...
fi
```

If the CLI is not installed, skip this block and use the Android developer website as before.
