---
name: pos
description: POS (Point of Sale) architecture and patterns. Use when writing, editing, exploring, debugging, fixing bugs, implementing features, or reviewing WooPos-prefixed classes or files under ui/woopos/. POS uses a different architecture than the main app — plain ViewModel (not ScopedViewModel), pure Compose (no Fragments), Compose Navigation (no nav graphs), parent-child SharedFlow event bus. Loading this skill prevents applying main-app patterns that would be wrong for POS. For analytics use `pos-analytics`, for tests use `pos-tests`.
allowed-tools: Bash, Read, Write, Edit, Grep, Glob
user-invocable: true
---

# POS Architecture & Patterns

@docs/pos-architecture.md
