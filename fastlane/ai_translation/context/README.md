# Per-key translation context (AINFRA-1707 seam)

`strings_context.json` maps a resource name to a short context string
(screen/feature, surrounding usage, developer notes) that is fed into the
translation prompt — the main quality lever.

```json
{
  "orderdetail_shipping_details": "Section header on the Order Detail screen.",
  "product_selection_count_single": "Toolbar subtitle while selecting products."
}
```

v1 ships an empty map. When the shared AINFRA-1707 strings
context-extraction pipeline output is finalized, only
`ContextProvider#context_for` changes — the engine and prompt are agnostic to
where context comes from. The context value is part of the manifest cache key,
so adding/refining context for a key correctly re-translates just that key.
