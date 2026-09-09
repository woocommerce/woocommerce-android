# Toolbar scroll-state divider — investigation and implementation findings

Status: **implemented in production.** The validated approach is now part of `WooDesignSystemToolbar`, with explicit
regression coverage in the design-system test suite.

The design-system top app bar ([Figma](https://www.figma.com/design/50XIH5MmOf4xUYEkM6fAm6/Mobile-Design-System?node-id=5011-5933&m=dev))
shows a bottom divider only when the content beneath it is scrolled away from the top. Everything else in the new
bar is static styling and straightforward to apply. This document covers only the divider, because it is the one
part that needs a scroll-state signal, and records what we found while building it.

## The problem

A toolbar has to know whether *someone else's* content is scrolled. In Compose that relationship is expressed
through `nestedScroll` and is already solved by the framework. In the View layer our toolbar is a plain sibling of
whatever scrolls, with no structural link to it.

Three ways to establish that link were considered:

| | Approach | Call-site cost |
|---|---|---|
| **A** | `AppBarLayout` + `app:liftOnScroll`, converting layout roots to `CoordinatorLayout` | Restructure ~65 layouts |
| **B** | Widget flag plus `setScrollTarget(view)` called from each fragment | ~60 one-line fragment edits |
| **C** | Auto-discovery inside the widget | Zero call-site edits |

The production implementation uses **C**, with **B** retained as an explicit override.

## What was built

All in `WooDesignSystemToolbar`:

- The divider is no longer baked into `woo_ds_toolbar_background.xml`. It is drawn in `dispatchDraw()` after
  `super`, as a rect of `woo_ds_color_tint_layer_on_surface_opacity16` and height `woo_ds_toolbar_divider_height`
  (0.5dp). `getDimensionPixelSize` is used rather than `getDimensionPixelOffset` so a sub-pixel dp value still
  rounds up to 1px instead of vanishing at low density.
- A `ViewTreeObserver.OnScrollChangedListener` plus `OnGlobalLayoutListener`, registered in `onAttachedToWindow`
  and removed in `onDetachedFromWindow`, drive `updateDivider()`.
- `DividerMode { AUTO, ALWAYS, NEVER }`, settable in code or via `app:wooToolbarDividerMode`.
- A scroll target resolved in priority order: an explicit `setScrollTarget(view)`, then
  `app:wooToolbarScrollTarget`, then auto-discovery.
- New styleable `WooDesignSystemToolbar` in `attrs_design_system.xml`.

Auto-discovery walks outward from the toolbar — searching each ancestor level while skipping the branch already
searched, stopping at `android.R.id.content`.

## The false positive that shaped the design

The obvious predicate, `canScrollVertically(-1)`, produces a **divider at rest** on the Payments hub. Root cause,
reproduced arithmetically:

Row 0 of `paymentsHubRv` is a `ComposeView` (`PayoutSummaryViewHolder`) that composes to nothing while payout
state is `null`, `Loading` or `Error`, so it lays out 0x0 at `top=0`. `RecyclerView` does not track a real scroll
offset; `ScrollbarHelper.computeScrollOffset` *estimates* one from a reference child chosen by a strict-overlap
test (`child-end > parent-start`). A zero-height child at `top=0` fails that test, so item 0 is skipped and item 1
becomes the reference:

```
itemsBefore = 1  ->  offset = 1 x 174.286 = 174  ->  canScrollVertically(-1) == true
```

Logged metrics on device were `offset=174, range=1394, extent=1220`, all reproducing by hand to the exact integer.
The smoking gun: `findFirstCompletelyVisibleItemPosition()` returns `0` on the same frame.

The control screen (order detail) is a `NestedScrollView`, which does not override `computeVerticalScrollOffset()`
— `View`'s implementation returns the real `scrollY`, with no estimation layer. Hence the asymmetry.

**This is a defect in the standard predicate, not in our code.** `AppBarLayout.shouldLift()` in Material 1.14.0
decompiles to `canScrollVertically(-1) || getScrollY() > 0`, so option A would reproduce the same false positive.

The fix is a container-aware predicate. For `RecyclerView` we scan attached children for the lowest adapter
position and ask whether it is item 0 sitting at the top inset; other containers keep `canScrollVertically(-1)`.

## Defects found and fixed during review

Five findings were closed, with regression coverage for the relevant behavior in the design-system test suite.

1. **Discovery masked a real scroller.** Making eligibility type-based rather than state-based fixed flapping but
   meant an inert-but-eligible container permanently masked a later scrolling sibling — for example
   `assignedTermList` ahead of `globalTermList` in `fragment_add_attribute_terms.xml`. Discovery is now two-pass:
   prefer a candidate that can currently scroll, fall back to the first type-eligible one only when nothing on
   screen scrolls. That keeps binding stable on screens where nothing scrolls yet without letting an empty list
   mask a real one.
2. **Wrong geometric edge.** The scan compared raw `child.top` against `paddingTop`. With an `ItemDecoration` that
   adds a top inset, the decorated boundary moves above `paddingTop` while raw `top` is still below it, creating a
   false-negative window exactly the size of the inset. Now uses `layoutManager.getDecoratedTop(child)`.
3. **Reversed layouts.** The lowest-adapter-position-is-visually-top assumption is false for `reverseLayout` or
   `stackFromEnd`, and unsound for `StaggeredGridLayoutManager`. Those now fall back to `canScrollVertically(-1)`.
   `GridLayoutManager` extends `LinearLayoutManager` and deliberately keeps the exact path.
4. **Explicit target silently discarded.** `setScrollTarget(view)` stored only `view.id`. For a view with no
   `android:id` — legal, and not forbidden by the KDoc — a moment of not being shown caused re-resolution to fall
   through to auto-discovery and swap in a different view, unrecoverably. The caller's view is now held in its own
   field and always wins.
5. **Discovery never gave up.** With a null target, `scrollTarget?.isShown != true` is always true, so a screen
   with no View-based scroll host re-walked the tree on every global layout for the whole attach cycle. Capped at
   `MAX_AUTO_DISCOVERY_ATTEMPTS = 5`, reset on success and on attach. A small cap rather than one attempt still
   catches a scroller arriving via async inflation or a `ViewStub`. Severity note: `OnGlobalLayoutListener` fires
   on layout traversals, not per frame — `RecyclerView` scrolling offsets children inside a suppressed-layout
   window and `NestedScrollView` calls `scrollTo` — so this was waste, not jank.

## Verification

- The design-system unit-test suite, including toolbar styling, divider discovery, scroll-state, and collapse behavior,
  passes. The relevant detekt checks are clean.
- On-device pixel decoding, toolbar bounds `[0,121][1080,289]`, divider row y=288, dark mode:

| Screen | State | Row y=288 | Expected |
|---|---|---|---|
| Payments hub | at rest | flat `(16,21,23)` | no divider |
| Order detail | at rest | flat `(16,21,23)` | no divider |
| Order detail | scrolled | `(54,59,60)` across full width, rows 287/289 flat | divider |

## Comparison with `AppBarLayout` + `liftOnScroll`

Worth stating plainly because it is the obvious "why not just use the platform" question. Split across three axes,
androidx lands differently on each.

**Rendering — nothing usable.** `liftOnScroll` drives exactly two effects and neither is a divider: a
`stateListAnimator` animating View Z-elevation, or an M3 background-colour crossfade gated on
`getBackground() instanceof MaterialShapeDrawable`. There is no hairline or stroke anywhere in `AppBarLayout`; the
AAR's divider resources all belong to the separate `MaterialDivider` widget. `Theme.Woo` descends from
`Theme.MaterialComponents` and pins `appBarLayoutStyle` to `Woo.AppBarLayout` ->
`Widget.MaterialComponents.AppBarLayout.Surface`, so every AppBarLayout here resolves to the *legacy* elevation
animator and the M3 colour path is unreachable without a theme migration. We would draw our own divider regardless.

Reading the lifted state from outside is not free either: `state_lifted` is an ordinary drawable-state attr, and
drawable state reaches a child only if that child opts in with `android:duplicateParentState` — which replaces the
toolbar's entire drawable state with its parent's. Contrast `setActivated`, which `ViewGroup.dispatchSetActivated`
forces onto every descendant unconditionally; that difference is why a `setActivated`-based approach was rejected.

**Predicate — worse than ours.** `shouldLift()` shares the defect described above, and
`updateAppBarLayoutDrawableState` is called from `onLayoutChild` with `force=true` on *every layout pass*, so an
untouched screen has the predicate evaluated at rest continuously. The Payments-hub false positive would appear
there too.

**Binding — better than ours, and the only axis where it wins.** `findFirstScrollingChild` scans only immediate
`CoordinatorLayout` children, and `ScrollingViewBehavior.onDependentViewChanged` works off the exact sibling named
by `appbar_scrolling_view_behavior`. Structural and unambiguous: the masking regression above cannot occur.

### Migration cost of adopting A

Across the 74 layouts containing `WooDesignSystemToolbar`:

| Bucket | Count | Meaning |
|---|---|---|
| Mechanical | 44 | Toolbar plus one other top-level branch; convert root, add `layout_behavior` |
| Expensive | 12 | 3+ root children — pinned progress bars, empty states, tab bars, a second toolbar |
| No scroller | 13 | Nothing to lift against; conversion is meaningless |
| Toolbar inside the scroller | 2 | `fragment_refund_summary.xml`, `fragment_order_detail.xml` — cannot be separated |

Three cross-cutting costs the option-A sketch never accounted for:

- **21 of 74 use plain `<ScrollView>` and none sets `android:nestedScrollingEnabled`.** Platform `ScrollView` calls
  `startNestedScroll` but never enables the flag, which defaults off. Each needs the attribute or lift silently
  never fires — after a full structural conversion.
- **2 are WebView-hosted** (`fragment_feedback_survey.xml`, `fragment_receipt_preview.xml`). AOSP `WebView`
  contains zero nested-scroll call sites, so the opt-in attribute does not help. Permanently inert under A.
- **7 wrap their scroller in `ScrollChildSwipeRefreshLayout`**, stacking a second nested-scroll parent under the
  `CoordinatorLayout`. Workable, not mechanical.

### `liftOnScroll` is switched on nowhere in this app today

Of the 9 layouts using `AppBarLayout`, **7 have no `CoordinatorLayout` anywhere in the file**, so their
`state_lifted` can never flip — it is only ever set from inside `AppBarLayout.BaseBehavior`, and Behaviors attach
only to direct `CoordinatorLayout` children. Of the remaining two, `activity_main.xml` pins `liftOnScroll=false`
with `elevation=0dp` twice over via `Widget.Woo.MainActivity.AppBarLayout`, and `fragment_variation_detail.xml`
never sets it. `fragment_printing_instructions.xml` explicitly sets `liftOnScroll="false"`. There is no live
example in the repo to point at.

### One point in favour of pull-based

WebView is permanently inert under `AppBarLayout` because it dispatches no nested scroll. Our approach *queries*
scroll state rather than listening for it, so it is not blocked by that wall. Whether `WebView` reports usable
offsets and fires scroll-changed needs testing before claiming it, but if it does, one extra branch in the type
gate covers two screens option A can never reach.

## Known limitations

- **Dynamic host resolution is bounded.** When auto-discovery initially falls back to an eligible but inert host,
  `globalLayoutListener` re-resolves while the bound host is not active, allowing a later scrollable sibling to take
  over. The attempt cap prevents repeated tree walks on screens without a View-based scroll host, and regression tests
  cover both the previously inert host and the cap. Explicit targets remain stable for the lifetime of the attachment.
- **Screens auto-discovery cannot serve.** `fragment_refund_summary.xml` has the toolbar *inside* the
  `NestedScrollView`. `two_pane_mode_toolbar` in `fragment_order_create_edit_form.xml` hosts a 4-destination nav
  graph with no static scrollable id, so the XML attribute cannot name a target either.
- **`getChildAdapterPosition` returns `NO_POSITION`** during adapter updates, pre-layout and disappearing
  animations. Broadly reachable — Product Reviews sets a `DefaultItemAnimator`. Handled by skipping such children,
  but not specifically tested.
- **Minor, not actioned.** `dividerPaint`/`dividerHeight` initialisers resolve the raw constructor `context` while
  the rest of the class uses the themed `View.getContext()`. Harmless while both are direct resource-id lookups;
  would diverge if either became a `?attr/` lookup.

## The Compose side

The migration is now implemented. `WooTopAppBar` draws a `WooDivider` only when its supplied
`WooTopAppBarScrollBehavior` reports content overlap (`overlappedFraction > 0.01f`). Callers attach the
behavior's `nestedScrollConnection` to the scrolling container; headers without a behavior remain divider-free.

The original inventory below records the pre-migration sizing and scroll-host shapes. The direct migration is
concentrated in the design-system component and the `ui/compose/component/Toolbar.kt` bridge, with explicit
scroll wiring for the migrated collapsible screens.

| Shape | Count | Note |
|---|---|---|
| `Scaffold` + lazy list | 16 in 15 files | straightforward |
| `Scaffold` + `verticalScroll` | 32 | straightforward |
| `Scaffold` + `WCWebView` | 8 | blocked — no nested-scroll participation |
| `Scaffold`, non-scrolling | 9 | no change needed |
| No `Scaffold` | 3 | needs a decision |

Material3 supplies the mechanism: `TopAppBarDefaults.exitUntilCollapsedScrollBehavior()` exposes
`TopAppBarState.overlappedFraction`, and `WooTopAppBarScrollBehavior` delegates nested scrolling while adding
programmatic expansion. Production wiring exists through `DashboardScreen` -> `DashboardWidgets` and the Product
List, with component tests covering divider visibility, discovery, and scroll behavior.

Note the cross-platform symmetry: WebView screens are the blind spot on both sides.

## Recommendation

**C is the cheapest to deploy but it is not cheap.** Zero call-site edits and the work concentrated in one file are
real advantages, and it handles the 15 screens that have no scroller or an inseparable toolbar for free — those are
migration problems under A but simply produce no divider under C, which is correct. Against that, the predicate has
to be written per container type, and there are screens C cannot serve.

The honest shape is **C's auto-discovery with B's explicit override as the escape hatch**, which is what the production
implementation uses. Adopting A would still leave us writing our own predicate and drawing our own divider, on top of a
74-layout restructure that at least 17 of those layouts cannot complete. The one thing A does better is binding, and
the two-pass discovery here is an approximation of that guarantee in software.

The remaining limitations are screen-specific: the two WebView screens and `fragment_refund_summary.xml` still need
an explicit product decision if their content must drive a divider. They do not invalidate the production behavior on
the supported View scroll hosts.
