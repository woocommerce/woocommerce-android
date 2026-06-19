# POS Store API integration — Android client decisions

Paired with the WooCommerce server-side architectural spike at
`plugins/woocommerce/src/Internal/POS/StoreApi/` in `woocommerce/woocommerce`,
this is the Android side: behind the
`WOO_POS_STORE_API_CHECKOUT` feature flag, the moment the cashier taps "Charge"
runs the in-memory cart through the new `wc/internal/pos/v1/cart/add-item` +
`/checkout` endpoints instead of `POST /wc/v3/orders`. Everything downstream of
order creation (payment flow, card reader SDK, cash mark-paid) is unchanged.

The namespace is `wc/internal/pos/v1` (not `wc/pos/v1`, which is the public POS
catalog feed): the cart/checkout shape is still a spike and not a committed
public contract, and the routes exist only when the server's `point_of_sale`
feature is enabled.

This document records the judgement calls made while wiring it up.

## Why a flag-gated branch in `WooPosTotalsRepository` rather than a parallel repo

`WooPosTotalsRepository.createOrderFromCartItems` is the single funnel through
which the totals/checkout screen materialises an order. The flag check lives
there because:

- One place to switch on/off; collaborators (ViewModel, payment flow) see no
  difference in the returned `Result<Order>` contract.
- The existing REST path stays the default and is untouched in code, only
  short-circuited when the flag is on. Easy to revert.
- A parallel repository would have required wiring choice into the ViewModel,
  duplicating the orderCreationJob cancellation logic, and divergent tests.

The new flow lives in a separate `PosStoreApiCheckoutUseCase` so it can be
tested in isolation and so the repository's existing responsibilities (Order
assembly, coupon/fee composition) don't grow.

## Why per-item `cart/add-item` instead of a batch endpoint

The Store API has `/wc/store/v1/batch` which can issue multiple operations in
one HTTP request. We could expose it as `/wc/internal/pos/v1/batch` and post all
items + the checkout in one round-trip.

For the spike we stuck with per-item add + final checkout because:

- It matches the canonical Store API web flow, so any extension hooks that
  fire per add (gift card line-item data setup, subscription validation) fire
  in the same sequence as on web.
- Per-call latency is the same order of magnitude as the existing
  REST-`POST /wc/v3/orders` round-trip, which already involves several
  internal calls server-side for line composition.
- Lets us test the Store API integration on the simplest possible code path
  before optimising.

A `/wc/internal/pos/v1/batch` follow-up is a sensible optimisation once the
per-item flow is validated end-to-end against real extensions.

## Why fetch the order after `/checkout` instead of constructing it locally

The Store API `/checkout` response carries the full order schema, so we could
deserialise totals/items directly and build an `Order` without a follow-up
fetch. We chose the fetch path because:

- It keeps the `Order` mapping path identical to the existing REST flow
  (`OrderRestClient` → `OrderEntity` → `OrderMapper.toAppModel`). No new
  mapper, no two-source-of-truth bug surface in this spike.
- The local `OrderEntity` cache is populated as a side effect, which the rest
  of the app already assumes for freshly-created orders.
- It costs one extra round-trip in exchange for fully-shaped `Order` data.
  Worth it for the spike's narrow scope; an obvious optimisation later.

## Open question: session continuity across requests

This is the load-bearing follow-up for end-to-end testing.

The Store API server-side spike uses a custom `POSSessionHandler` that:

1. Generates a guest-style session ID (`t_xxx`) on the first request so the
   cart isn't keyed by the cashier's user ID.
2. Reads the existing `?session=<cart_token>` query parameter to identify the
   cart on subsequent requests.

But:

- `WooNetwork` does not configure an OkHttp `CookieJar`. The default
  WC_Session_Handler sends `Set-Cookie` on responses with cart mutations, but
  we don't capture it.
- `CartTokenUtils::get_cart_token` requires the server's `wp_salt()` — mobile
  cannot mint a fresh cart token client-side.
- The Store API's own `SessionHandler` reads an `HTTP_CART_TOKEN` *header*
  instead of `?session=`, which would be the cleanest fit for mobile, but
  that class is `final` server-side.

Practical paths forward, in order of preference:

1. **Make `StoreApi\SessionHandler` non-final upstream and extend it as
   `POSSessionHandler`.** Mobile sends/receives `Cart-Token` via header. This
   is what Block Checkout already does — POS would share the model.
2. **Add a `/wc/internal/pos/v1/cart-token` endpoint** that issues a fresh signed
   cart-token. Mobile calls it once at sale start; passes the token via
   `?session=` on subsequent requests.
3. **Configure an OkHttp `CookieJar` per POS transaction** in the Android
   network stack. Heavier change to WooNetwork; couples POS to the legacy
   WC_Session_Handler cookie format.

Option 1 is the right long-term answer. Until one of these lands, the new
flow will work for the first `cart/add-item` (creates a new server-side
session) but every subsequent call in the same flow gets its own fresh
session — i.e. the cart on checkout will not contain the prior items. This
is documented in the server-side `DECISIONS.md` too. Unit tests mock the
REST client so they're unaffected.

## Coupons and custom amounts

Both flow through dedicated POS cart endpoints before checkout, so server-side
validation and composition run unchanged:

- **Coupons** (`ItemClickedData.Coupon`) → `cart/apply-coupon`, one call per
  code. Coupon validation (usage limits, per-customer limits, product
  restrictions) runs server-side.
- **Custom amounts** (`ItemClickedData.CustomAmount`) → `cart/add-fee`, one
  call per fee, carrying name/amount/taxable. The server persists each fee in
  the session and re-applies it on every cart recalculation, and fee identity
  is content-derived so re-adding an identical fee is idempotent.

The sequence is items → coupons → custom amounts → checkout; ordering is not
load-bearing (coupons never discount fees, and fee/coupon persistence both span
the cart-building phase), but it keeps the call order predictable.

## What this spike deliberately does NOT include

- **Customer pre-fill on the order** — guest checkout in all cases. The
  agent/customer identity model from the server proposal hasn't been
  implemented yet, so there's no `customer_id` to send.
- **Resilient retry / partial-cart recovery** — if `add-item` succeeds for
  three items and fails on the fourth, we return failure but the
  server-side cart still has three items. Cleanup would need a `/cart`
  destroy call or a transaction-scoped cart token that's discarded.
- **The cash-paid endpoint integration** — server-side spike documents this
  as the one net-new endpoint needed; not in scope for the Android spike
  because the existing cash flow already calls `payment_complete()` on the
  order ID it receives.
- **End-to-end integration tests against a real device with a real site.**
  Unit tests cover the wiring; smoke testing against a real Woo install with
  the server-side spike applied is the natural follow-up.
