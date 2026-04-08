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

One persistent button sits at the bottom of every screen. It never disappears and re-appears - it stays in place and changes its label:

- **On products:** "Cart (2)" - tapping opens the cart
- **On cart:** "Check out" - tapping starts checkout
- **On checkout:** "Cash payment" - tapping starts cash payment
- **During card payment:** hidden (the payment flow has its own UI)

This avoids the jarring effect of two similar buttons appearing during screen transitions. The button is a constant anchor point that the merchant can always reach.

---

## Product Cart Indicators

When a product is in the cart, a **small purple circle badge** with the item count appears at the bottom-right corner of the product image (like Uber). The image stays fully visible - no overlay or dimming.

We tried several approaches before landing on this:
- Stepper controls (- N +) next to the title - truncated long product names
- Stepper on the price row - looked awkward
- Stepper centered on the image with dark overlay - too noisy, competed with the cart button visually
- **Simple count badge** - cleanest solution. Merchant sees which products are in cart and how many at a glance. To adjust quantity, they go to the cart.

**Coupons** show a trash icon overlay when added to cart, since coupons are binary (added or not) and removal should be quick.

---

## Screen Transitions

- **Products to Cart:** slides up from the bottom (the cart button is at the bottom, so the motion feels connected)
- **Cart to Checkout:** slides horizontally left (standard forward navigation)
- **Going back:** reverses the entry animation (cart slides down, checkout slides right)

---

## Orders, Bookings, Settings on Phone

These screens use a two-pane layout on tablet (list on the left, detail on the right). On phone, they become a two-step flow: see the list first, tap an item to see the detail.

A shared component handles this automatically - the same code renders side-by-side on tablet and list-then-detail on phone.

**Settings** shows the categories list first (Store, Hardware, Product catalog). Tapping a category slides to its detail. The detail pane has its own back arrow to return to categories.

---

## Sizing and Readability

POS text is **larger than a typical phone app**. Merchants use POS in busy environments - often at arm's length, sometimes in bright light. We scale fonts to 90% of the tablet size (not the typical phone app scaling of 85% or less).

Buttons, product images, and animations scale to 90% too. The goal is a focused experience that feels roomy, not cramped.

Dialogs take 92% of screen width on phone (vs 75% on tablet) so buttons and text have enough room.

---

## Card Reader / Tap to Pay

On phone, the default payment method is **Tap to Pay** (built-in NFC reader). No external hardware needed - the merchant just holds the customer's card near the phone.

On tablet, the default is an external Bluetooth card reader (standard POS setup).

The card reader connection dialog adapts to phone with wider layout and smaller icons.

---

## What This Means for iOS

When porting to iOS, carry over:

- Hamburger menu, not bottom tabs
- Full-screen linear flow (no bottom sheet for cart)
- Persistent bottom button that morphs between screens
- Quantity stepper on product images
- Same list-then-detail pattern for Orders/Bookings/Settings
- Tap to Pay as default on iPhone
