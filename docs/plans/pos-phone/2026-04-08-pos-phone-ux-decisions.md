# POS Phone - UX/UI Decisions

How the phone POS works and why we made these choices.

---

## Overall Approach

The phone POS is a **focused, single-purpose payment tool**. Every decision prioritizes giving maximum screen space to the payment flow: browse products, build a cart, and check out.

The tablet POS shows products and cart side by side. On phone, we show one thing at a time with clear transitions between steps.

---

## Navigation

**Hamburger menu instead of bottom tabs.** Bottom tabs would permanently take ~56dp for features used maybe 5% of the time (Orders, Bookings, Settings). The hamburger menu keeps these accessible but out of the way.

**Linear payment flow.** Products -> Cart -> Checkout. Each step is a full-screen experience. The merchant focuses on one task at a time.

**Portrait only.** Phone POS is a single-column experience. Landscape doesn't add value on a phone-sized screen.

---

## The Bottom Button

One persistent button sits at the bottom of every screen. It lives OUTSIDE the NavHost so it never unmounts. It stays in place and changes its label:

- **On products:** "Cart - $37.49" showing the subtotal (computed client-side from item prices). Slides in when cart has items.
- **On cart:** "Check out" (filled/primary style)
- **On checkout:** "Cash payment" (outlined/secondary style)
- **During card payment or payment success:** hidden (the payment flow has its own UI)

This avoids the jarring effect of two similar buttons appearing during screen transitions. The button is a constant anchor point that the merchant can always reach.

---

## Product Cart Indicators

When a product is in the cart, a **small purple circle badge** (28dp) with the item count appears at the bottom-right corner of the product image with 6dp margin from edges (like Uber). The image stays fully visible - no overlay or dimming.

We tried several approaches before landing on this:
- Stepper controls (- N +) next to the title - truncated long product names
- Stepper on the price row - looked awkward
- Stepper centered on the image with dark overlay - too noisy, competed with the cart button visually
- Purple pill stepper on the image - still too visually dominant
- **Simple count badge** - cleanest solution. Merchant sees which products are in cart and how many at a glance. To adjust quantity, they go to the cart.

**Coupons** show a trash icon badge (same style, 28dp purple circle) when added to cart. Second tap on the coupon removes it from cart.

---

## Cart Screen

- **Back arrow always visible** using WooPosToolbar-consistent spacing
- **Checkout button hidden on phone** - the persistent bottom button handles checkout instead
- **Cart slides up from the bottom** when opened (the cart button is at the bottom, so the motion feels connected)

---

## Screen Transitions

- **Products to Cart:** slides up from the bottom
- **Cart to Checkout:** slides horizontally left (standard forward navigation)
- **Checkout to Products (new order):** pops all the way back to products
- **Going back:** reverses the entry animation (cart slides down, checkout slides right)
- **Settings/Orders/Bookings from menu:** standard horizontal slide from right (same as root navigation)
- All transitions use consistent 300ms timing

---

## Orders, Bookings, Settings on Phone

These screens use a two-pane layout on tablet (list on the left, detail on the right). On phone, they become a two-step flow: see the list first, tap an item to see the detail.

A shared `WooPosListDetailLayout` component handles this automatically.

**Key phone behaviors:**
- No auto-selection of first item on phone (on tablet, first item is pre-selected)
- Detail screen has its own toolbar with back arrow and item-specific title ("Order #526", "Booking #498")
- Parent screen toolbar is hidden when detail is showing on phone
- Back arrow on detail returns to list, system back from list exits the screen
- 16dp horizontal margins on list items and detail content
- Detail content skips `statusBarsPadding` since the toolbar handles it

**Settings** starts showing categories list (not detail) on phone. The `isDetailPaneOpen` starts false and becomes true when a category is tapped. No visible animation on first render.

---

## Sizing and Readability

POS text is **larger than a typical phone app**. Merchants use POS in busy environments - often at arm's length, sometimes in bright light. We scale fonts to 90% of the tablet size for 880-1200dp screens, 75% for screens under 880dp.

Buttons, product images, and animations scale to 90%/75% too. The goal is a focused experience that feels roomy, not cramped.

Dialogs take 92% of screen width on phone (vs 75% on tablet). Connection dialog uses 92% on phone, 55% on tablet.

Dialog close icon (40dp) and Lottie animations (256dp, 160dp) use adaptive component sizing.

---

## Card Reader / Tap to Pay

On phone, the default payment method is **Tap to Pay** (built-in NFC reader via `CardReaderType.BUILT_IN`). No external hardware needed.

On tablet, the default is an external Bluetooth card reader (`CardReaderType.EXTERNAL`).

The card reader connection dialog (from `woomob-1854` branch) adapts to phone with wider layout and smaller icons.

---

## Known Outstanding Items

- Corner radius may need to be less rounded on phone (currently using tablet values)
- Card payment screen (from bookings "Collect payment") buttons need to be pushed to bottom of screen
- Attendance status buttons on booking detail could use more layout refinement
- Price on cart button uses client-side subtotal (pre-tax, no coupon discounts) - same as food delivery apps

---

## What This Means for iOS

When porting to iOS, carry over:

- Hamburger menu, not bottom tabs
- Full-screen linear flow (no bottom sheet for cart)
- Persistent bottom button that morphs between screens
- Count badge on product images (not stepper controls)
- Same list-then-detail pattern for Orders/Bookings/Settings with item-specific detail titles
- Tap to Pay as default on iPhone
- 16dp horizontal margins on phone screens
- Subtotal on cart button
