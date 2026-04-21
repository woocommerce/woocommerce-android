# Maestro Smoke Tests for WooCommerce Android

Automated UI smoke tests using [Maestro](https://maestro.mobile.dev/) that cover the manual testing flows defined in the [Smoke Testing P2 post](https://woomobilep2.wordpress.com/flows-for-app-features-smoke-testing/).

## Prerequisites

1. **Install Maestro CLI:**
   ```bash
   curl -fsSL "https://get.maestro.mobile.dev" | bash
   ```
   Requires Java 17+.

2. **Android emulator or device** running with the WooCommerce dev (wasabi) build installed:
   ```bash
   ./gradlew :WooCommerce:installWasabiDebug
   ```

3. **Environment variables** for credentials (never hardcoded):
   ```bash
   export MAESTRO_WOO_EMAIL="your-email@example.com"
   export MAESTRO_WOO_PASSWORD="your-password"
   export MAESTRO_WOO_STORE_URL="https://your-store.wpcomstaging.com"
   ```
   See `env.example` for all available variables.

## Running Tests

### All smoke tests
```bash
maestro test .maestro/flows/
```

### Single flow
```bash
maestro test .maestro/flows/login_successful.yaml
```

### By tag (category)
```bash
maestro test --include-tags=smoke .maestro/flows/
maestro test --include-tags=login .maestro/flows/
maestro test --include-tags=orders .maestro/flows/
maestro test --include-tags=products .maestro/flows/
maestro test --include-tags=hub_menu .maestro/flows/
maestro test --include-tags=pos .maestro/flows/
```

### Pass credentials inline
```bash
maestro test \
  -e WOO_EMAIL="user@example.com" \
  -e WOO_PASSWORD="password" \
  -e WOO_STORE_URL="https://store.wpcomstaging.com" \
  .maestro/flows/login_successful.yaml
```

### Generate reports
```bash
maestro test --format junit --output report.xml .maestro/flows/
maestro test --format html --output report.html .maestro/flows/
```

### Local runner (recommended)
Use the wrapper script — it runs preflight checks, executes every flow in
the exact order declared in the P2 post, captures a per-flow screen
recording (kept **only** for flows that fail), and emits a self-contained
HTML report plus a JUnit XML file.

Artifacts are written **outside the repo** under
`$HOME/woocommerce-maestro-output/<timestamp>/` so runs don't pollute the
working tree. Override with `--output-dir <path>` or the
`WOO_MAESTRO_OUTPUT_DIR` env var.

```bash
.maestro/scripts/run-smoke-tests.sh                         # all flows, P2 order
.maestro/scripts/run-smoke-tests.sh --apk path/to/app.apk   # install APK first
.maestro/scripts/run-smoke-tests.sh -t login                # filter by tag
.maestro/scripts/run-smoke-tests.sh --output-dir /tmp/run1  # custom output dir
.maestro/scripts/run-smoke-tests.sh --no-record             # skip screen recording
.maestro/scripts/run-smoke-tests.sh .maestro/flows/login_successful.yaml  # single
```

Why "per-flow recording" requires its own wrapper: Maestro's single
`maestro test .maestro/flows/` invocation can't bracket each flow with
`adb shell screenrecord`, because `screenrecord` only supports one
concurrent invocation per device. The script therefore runs Maestro once
per flow and starts/stops the recorder around each run.

CI continues to use `.buildkite/commands/run-maestro-tests.sh`, which uses
Maestro's single-invocation JUnit output for Buildkite Test Analytics.

### Interactive development (auto-rerun on changes)
```bash
maestro test -c .maestro/flows/login_successful.yaml
```

### Inspect UI elements (Maestro Studio)
```bash
maestro studio
```

## Directory Structure

```
.maestro/
  config.yaml              # Workspace configuration (execution order, tags, etc.)
  env.example              # Template for environment variables
  README.md                # This file
  flows/                   # Top-level test flows (auto-executed by maestro test)
    login_successful.yaml
    login_not_wp_site.yaml
    login_wrong_credentials.yaml
    dashboard_stats.yaml
    dashboard_view_all_analytics.yaml
    dashboard_customize.yaml
    orders_list_and_search.yaml
    orders_create.yaml
    orders_details_and_actions.yaml
    orders_mark_complete.yaml
    orders_cash_payment.yaml
    orders_refund.yaml
    products_list_and_sort.yaml
    products_detail.yaml
    products_variations_and_tags.yaml
    products_create.yaml
    products_media_upload.yaml
    hub_menu_settings.yaml
    hub_menu_payments.yaml
    hub_menu_coupons.yaml
    hub_menu_customers_inbox.yaml
    hub_menu_admin_and_store.yaml
    blaze_campaign.yaml
    google_for_woo.yaml
    pos_search_and_coupons.yaml
    pos_cash_payment.yaml
  subflows/                # Reusable subflows (NOT auto-executed)
    ensure_logged_in.yaml
    login.yaml
    navigate_to_orders.yaml
    navigate_to_products.yaml
    navigate_to_more_menu.yaml
```

## Selectors and `testTag`

`WooTheme` / `WooThemeWithBackground` apply
`Modifier.semantics { testTagsAsResourceId = true }` at the root of the
Compose tree (`ui/compose/theme/Theme.kt`), so Compose `testTag` values are
exposed to Maestro as resource IDs and can be used directly with `id:`
selectors. Flows use resource IDs where available (bottom nav, dashboard
cards, POS) and fall back to text selectors for labels and strings.

Maestro treats text selectors as regular expressions — use `".*foo.*"` for
partial matches and escape regex metacharacters in exact matches.

## Test Coverage vs P2 Smoke Testing Flows

Status legend: **Yes** = fully covered, **Partial** = flow exists but doesn't
exercise every P2 sub-item, **No** = not automated (reason in Notes).

| P2 Category | Flow File | Automated? | Notes |
|---|---|---|---|
| **Installation** | | | |
| Upgrade from previous version | - | No | Requires APK version swap outside Maestro |
| Fresh install smoke | Implicit via `clearState: true` in every flow | Partial | Each flow starts from fresh state |
| **Login** | | | |
| Successful store login | `login_successful.yaml` | Yes | |
| Not a WP site (google.com) | `login_not_wp_site.yaml` | Yes | |
| Wrong credentials | `login_wrong_credentials.yaml` | Yes | |
| Help section | `login_help.yaml` | Yes | |
| Not a Woo store | `login_not_woo_store.yaml` | Yes | Reuses primary creds + `WOO_NOT_A_WOO_STORE_URL` |
| Wrong account for the store | `login_wrong_account.yaml` | Yes | Reuses primary creds + `WOO_WRONG_ACCOUNT_STORE_URL` |
| No Jetpack | `login_no_jetpack.yaml` | Yes | Needs a non-Jetpack WP+WC site + `WOO_JN_*` site credentials; asserts the "Explore Jetpack" dashboard banner |
| Social login (Google) | `login_google.yaml` | Yes | Assumes Google account pre-signed-in on device |
| Jetpack not connected | - | No | Needs JN site + manual Jetpack disconnect |
| Passwordless login | - | No | Requires Mailosaur inbox access |
| Social login (Apple) | - | No | iOS-only per P2 |
| Login with 2FA | - | No | Requires authenticator app / TOTP secret |
| **Dashboard/Stats** | | | |
| Charts respond, date ranges | `dashboard_stats.yaml` | Yes | |
| View All store analytics | `dashboard_view_all_analytics.yaml` | Yes | Taps the "View all store analytics" action on the Top Performers card and asserts the Analytics Hub loads |
| Customization — all cards | `dashboard_customize.yaml` | Partial | Opens the widget editor via the toolbar "Customize" action, asserts the core widgets (Performance, Top performers, etc.) render with a SAVE action, and closes without mutating state. Does NOT drag-to-reorder or toggle widgets on/off to keep the staging store's layout stable across runs |
| **Orders** | | | |
| List and pagination | `orders_list_and_search.yaml` | Yes | |
| Search | `orders_list_and_search.yaml` | Yes | |
| Create order (FAB, product, custom amount, customer, note) | `orders_create.yaml` | Yes | Adds a product, a fixed-amount custom amount with name, a customer (via "Add details manually" fallback), edits the customer, and adds a customer-facing note. Order is discarded at the end to keep staging clean |
| Order detail + add note | `orders_details_and_actions.yaml` | Partial | Details visible, adds an order note via `noteList_addNoteContainer` → `menu_add`. Does not tap "See receipt" or create shipping label |
| Mark order complete | `orders_mark_complete.yaml` | Yes | Filters to Processing, opens order, taps Mark Complete → Fulfill → Confirm, then taps Undo on the completion snackbar to leave the staging order in its original status |
| Collect cash-on-delivery payment | `orders_cash_payment.yaml` | Yes | Filters to Pending payment, opens order, taps Collect Payment → Cash → Change Due Calculator → Mark Order as Complete. No undo (cash payment has no revert), so one processing order is consumed per run |
| Refund order | `orders_refund.yaml` | Yes | Filters to Processing, selects all items, advances to the Refund Summary, fills a reason, taps Refund, and cancels the confirmation dialog so no real refund is issued against the staging store |
| Push notification for new order | - | No | Requires server trigger |
| Barcode scanner (add product / start order) | - | No | Requires camera |
| Shipping label creation | - | No | Complex external flow |
| **Products** | | | |
| List and pagination | `products_list_and_sort.yaml` | Yes | |
| Sort and search | `products_list_and_sort.yaml` | Yes | |
| Product detail (price, inventory, type, categories/shipping/description via fuzzy match) | `products_detail.yaml` | Partial | Variations, linked products, downloadable files, tags not explicitly asserted |
| Product detail — Variations | `products_variations_and_tags.yaml` | Yes | Opens the product filters (`btn_product_filter`), picks Product type → Variable, applies, asserts the filtered list is non-empty (fails with a clear seed-data message if it is), opens the first Variable product, asserts the `Variations` + `Variations attributes` rows, taps Variations to load `variationList`, then opens the first variation and asserts `cardsRecyclerView` on the variation detail screen. Backs out cleanly without mutating anything |
| Create product (full) | `products_create.yaml` | Yes | Creates a real product end-to-end: name ("Maestro Smoke Product"), price ($19.99), short description via the Aztec editor, and a category (ticks the first checkbox). Publishes, returns to the products list, searches for the name to confirm the product persisted. Mutates the staging store — clean up by bulk-deleting "Maestro Smoke" matches in wp-admin |
| Media upload (full) | `products_media_upload.yaml` | Yes | Exercises the real WP media library path: taps Add image → ProductImagesFragment → "WordPress media library" → taps the first `image_thumbnail` in the media recycler → taps `mnu_confirm_selection` ("Add N") → asserts the image renders in `productImage` on ProductImagesFragment → back to product detail → taps Save/Publish. Mutates the staging product (adds an image every run) |
| **Hub Menu** | | | |
| Settings | `hub_menu_settings.yaml` | Yes | |
| Payments — Pay in Person toggle, TTP, Order/Manage reader, Manuals | `hub_menu_payments.yaml` | Yes | UI only, no hardware |
| Coupons (list + create) | `hub_menu_coupons.yaml` | Yes | |
| Customers + Inbox | `hub_menu_customers_inbox.yaml` | Yes | |
| WC Admin + View Store + Change store | `hub_menu_admin_and_store.yaml` | Yes | |
| **Blaze** | | | |
| Campaign creation flow | `blaze_campaign.yaml` | Yes | Triggers flow only (payment not attempted) |
| **Google for Woo** | | | |
| Campaign webview loads | `google_for_woo.yaml` | Yes | |
| **POS (tablet only)** | | | |
| Add product(s) to cart | `pos_cash_payment.yaml` | Yes | Taps first/third product in grid |
| Pay with cash | `pos_cash_payment.yaml` | Yes | |
| Search products | `pos_search_and_coupons.yaml` | Partial | Asserts the active search hint matches the selected tab (`Search products and variations` on Products, `Search coupons` on Coupons). The search field still lacks a `testTag` so we can't type a query and assert filtered results — that's tracked separately |
| Use coupons | `pos_search_and_coupons.yaml` | Partial | Switches to the Coupons tab, waits for the list / empty state / error state to render, and optionally asserts the `Add coupon to cart` action is reachable. Does NOT actually add a coupon to the cart since the staging store's coupon seed data isn't guaranteed |
| Email receipt | `pos_cash_payment.yaml` | Partial | After a successful cash payment, taps `Email receipt` on the success screen and asserts the Email receipt screen loads with a `Send` action. Backs out without sending to avoid dispatching a real email on every run |
| Pay with card | - | No | Requires hardware (TTP not supported in POS per P2) |
| **Payments (hardware)** | | | |
| Card reader payment + print receipt | - | No | Requires physical card reader |
| Tap to Pay | - | No | Requires physical NFC + Apple/Google Pay setup |
| Refund IPP order | - | No | Requires prior IPP-paid order |
| **Other** | | | |
| Language switching | - | No | Requires device settings |
| Home-screen widget | - | No | Requires launcher interaction |
| Quick Actions (long-press app icon) | - | No | Requires launcher interaction |
| Watch app | - | No | iOS-only per P2 |
| Long-press push notification | - | No | iOS-only per P2 |

## Credentials Handling

Credentials are **never hardcoded** in the YAML files. Inside a flow
they are referenced as `${WOO_EMAIL}`, `${WOO_PASSWORD}`, etc.

Maestro CLI 2.x does NOT auto-import `MAESTRO_`-prefixed env vars (the
mobile.dev docs describing that behavior predate the rebrand). If you
run `maestro test …` directly with just the env exported, flows resolve
`${WOO_STORE_URL}` to the literal string `"undefined"`.

Use one of these instead:

1. **Wrapper scripts (recommended).** Both
   `.maestro/scripts/run-smoke-tests.sh` and
   `.buildkite/commands/run-maestro-tests.sh` collect every
   `MAESTRO_*` env var and pass it to maestro as `-e NAME=VALUE` with
   the prefix stripped. Export the vars (or `source .maestro/.env.local`)
   and run the script.

2. **CLI `-e` flags (per-run).**
   ```bash
   maestro test -e WOO_EMAIL="..." -e WOO_PASSWORD="..." flow.yaml
   ```

3. **CI secrets** (injected as `MAESTRO_*` env vars by the pipeline,
   then forwarded to maestro by the CI wrapper script above).

## Tips

- Use `maestro studio` to visually inspect the view hierarchy and find element selectors.
- Use `maestro test -c flow.yaml` for continuous mode during development.
- **Execution order follows the P2 post.** Within the login group, `login_successful` runs LAST so the app ends authenticated with the primary Woo account. Every non-login flow then reuses that session via `subflows/ensure_logged_in.yaml`. Only login-specific flows `clearState` and re-authenticate — repeated re-logins with the same account trigger WPCom security screens (magic-link, CAPTCHA) that block the rest of the suite.
- The local runner keeps a screen recording **only** for flows that fail. Passing flows have their recording and log deleted immediately after completion.
- Artifacts live outside the repo at `$HOME/woocommerce-maestro-output/<timestamp>/` by default. The HTML report embeds each failure's video inline, along with the first Maestro error line and a short troubleshooting hint.
- Screenshots are automatically saved at key checkpoints for visual verification.
- Flows use `optional: true` on interactions that may not be present on all store configurations.
