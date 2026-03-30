# Maestro Smoke Tests for WooCommerce Android

Automated UI smoke tests using [Maestro](https://maestro.mobile.dev/) that cover the manual testing flows defined in the [Smoke Testing P2 post](https://woomobilep2.wordpress.com/flows-for-app-features-smoke-testing/).

## Quick Start

1. **Install Maestro CLI:**
   ```bash
   curl -fsSL "https://get.maestro.mobile.dev" | bash
   ```
   Requires Java 17+.

2. **Install the app** on an Android emulator or device:
   ```bash
   ./gradlew :WooCommerce:installWasabiDebug
   ```

3. **Set up credentials:**
   ```bash
   cp .maestro/env.example .maestro/.env
   # Edit .maestro/.env with your credentials
   # Primary creds (appstestadmin): https://mc.a8c.com/secret-store/?secret_id=8326
   ```

4. **Run tests:**
   ```bash
   ./scripts/run-maestro-local.sh .maestro/flows/login_successful.yaml   # single flow
   ./scripts/run-maestro-local.sh .maestro/flows/                        # all flows
   ```

## Running Tests

### All smoke tests
```bash
./scripts/run-maestro-local.sh .maestro/flows/
```

### Single flow
```bash
./scripts/run-maestro-local.sh .maestro/flows/login_successful.yaml
```

### By tag (category)
```bash
./scripts/run-maestro-local.sh --include-tags=smoke .maestro/flows/
./scripts/run-maestro-local.sh --include-tags=login .maestro/flows/
./scripts/run-maestro-local.sh --include-tags=orders .maestro/flows/
./scripts/run-maestro-local.sh --include-tags=products .maestro/flows/
./scripts/run-maestro-local.sh --include-tags=hub_menu .maestro/flows/
./scripts/run-maestro-local.sh --include-tags=pos .maestro/flows/
```

### Pass credentials inline (without .env file)
```bash
maestro test \
  -e WOO_EMAIL="user@example.com" \
  -e WOO_PASSWORD="password" \
  -e WOO_STORE_URL="https://store.wpcomstaging.com" \
  .maestro/flows/login_successful.yaml
```

### Generate reports
```bash
./scripts/run-maestro-local.sh --format junit --output report.xml .maestro/flows/
./scripts/run-maestro-local.sh --format html --output report.html .maestro/flows/
```

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
  .env                     # Your local credentials (gitignored, copy from env.example)
  README.md                # This file
  flows/                   # Top-level test flows (auto-executed by maestro test)
    login_successful.yaml
    login_not_wp_site.yaml
    login_wrong_credentials.yaml
    login_not_woo_store.yaml
    login_wrong_account.yaml
    dashboard_stats.yaml
    orders_list_and_search.yaml
    orders_create.yaml
    orders_details_and_actions.yaml
    products_list_and_sort.yaml
    products_detail.yaml
    products_create.yaml
    hub_menu_settings.yaml
    hub_menu_payments.yaml
    hub_menu_coupons.yaml
    hub_menu_customers_inbox.yaml
    hub_menu_admin_and_store.yaml
    blaze_campaign.yaml
    google_for_woo.yaml
    pos_cash_payment.yaml
  subflows/                # Reusable subflows (NOT auto-executed)
    login.yaml
    navigate_to_dashboard.yaml
    navigate_to_orders.yaml
    navigate_to_products.yaml
    navigate_to_more_menu.yaml
scripts/
  run-maestro-local.sh     # Local test runner (loads .env, validates credentials)
```

## Test Coverage vs P2 Smoke Testing Flows

### Login
| P2 Flow | Flow File | Status | Notes |
|---|---|---|---|
| Successful store login | `login_successful.yaml` | Yes | |
| Not a WP site | `login_not_wp_site.yaml` | Yes | Uses google.com |
| Wrong credentials | `login_wrong_credentials.yaml` | Yes | |
| Not a Woo store | `login_not_woo_store.yaml` | Yes | Uses `notawoostore.wordpress.com` |
| Wrong account for store | `login_wrong_account.yaml` | Yes | Uses mismatched store/account |
| Help section | - | No | Low effort to add |
| Passwordless login | - | No | Requires Mailosaur email inbox access |
| Social login (Apple/Google) | - | No | Requires system OAuth |
| Login with 2FA | - | No | Requires TOTP code generation |
| No Jetpack / Jetpack not connected | - | No | Requires Jurassic Ninja ephemeral site |

### Dashboard/Stats
| P2 Flow | Flow File | Status | Notes |
|---|---|---|---|
| Charts, analytics, date ranges | `dashboard_stats.yaml` | Yes | |
| View All store analytics | `dashboard_stats.yaml` | Partial | Checks date ranges, not full analytics screen |
| Customization - all cards | - | No | Feasible to add |

### Orders
| P2 Flow | Flow File | Status | Notes |
|---|---|---|---|
| List and pagination | `orders_list_and_search.yaml` | Yes | |
| Search | `orders_list_and_search.yaml` | Yes | |
| Create order (products, shipping, notes) | `orders_create.yaml` | Partial | Creates order, does not complete full product addition flow |
| Order detail, notes | `orders_details_and_actions.yaml` | Yes | |
| Collect Payment (cash, QR, share link) | - | No | Feasible — verify payment screens appear |
| Refund, Mark complete, See receipt | - | No | Feasible to extend `orders_details_and_actions.yaml` |
| Push notification for new order | - | No | Requires WooCommerce REST API trigger |
| Barcode scanner | - | No | Requires camera |
| Shipping label creation | - | No | Complex external carrier API flow |

### Products
| P2 Flow | Flow File | Status | Notes |
|---|---|---|---|
| List and pagination | `products_list_and_sort.yaml` | Yes | |
| Sort and search | `products_list_and_sort.yaml` | Yes | |
| Product detail (price, inventory, categories, type, shipping, description) | `products_detail.yaml` | Yes | |
| Create product | `products_create.yaml` | Yes | |
| Tags, Linked products, Downloadable files | - | No | Feasible to extend `products_detail.yaml` |
| Variations + Detail | - | No | Feasible — needs variable product on test store |
| Media upload | - | No | Requires device gallery interaction |

### Hub Menu
| P2 Flow | Flow File | Status | Notes |
|---|---|---|---|
| Settings | `hub_menu_settings.yaml` | Partial | Checks accessibility; does not test individual items |
| Payments (Pay in Person, Card Reader, Manuals) | `hub_menu_payments.yaml` | Yes | UI only, no hardware |
| Coupons (list + create) | `hub_menu_coupons.yaml` | Yes | |
| Customers + Inbox | `hub_menu_customers_inbox.yaml` | Yes | |
| WC Admin + View Store + Change store | `hub_menu_admin_and_store.yaml` | Yes | |
| Tap to Pay | - | No | Requires NFC hardware |

### Blaze / Google for Woo
| P2 Flow | Flow File | Status | Notes |
|---|---|---|---|
| Blaze campaign creation | `blaze_campaign.yaml` | Yes | Triggers webview flow |
| Google for Woo webview | `google_for_woo.yaml` | Yes | Verifies webview loads |

### POS (tablet)
| P2 Flow | Flow File | Status | Notes |
|---|---|---|---|
| Add products to cart + Pay with Cash | `pos_cash_payment.yaml` | Yes | Requires tablet |
| Search products, Use coupons | - | No | Feasible to extend `pos_cash_payment.yaml` |
| Pay with Card | - | No | Requires hardware |
| Email receipts | - | No | Requires email inbox verification |

### Payments (hardware)
| P2 Flow | Flow File | Status | Notes |
|---|---|---|---|
| Card reader (IPP) / TTP | - | No | Requires physical Bluetooth/NFC hardware |

### Other
| P2 Flow | Flow File | Status | Notes |
|---|---|---|---|
| Language switching | - | No | Feasible via adb locale change, but brittle |
| Widget on home screen | - | No | Maestro can't interact with launcher |
| Quick Actions (long press) | - | No | Brittle, device-dependent |
| Watch App | - | No | Maestro doesn't support Wear OS |

## Credentials Handling

Credentials are **never hardcoded** in the YAML files. They are passed via:

1. **`.maestro/.env` file** (recommended for local development):
   ```bash
   cp .maestro/env.example .maestro/.env
   # Fill in credentials, then use the wrapper script:
   ./scripts/run-maestro-local.sh .maestro/flows/
   ```

2. **Environment variables** with `MAESTRO_` prefix (auto-available in flows without prefix):
   ```bash
   export MAESTRO_WOO_EMAIL="email"
   export MAESTRO_WOO_PASSWORD="pass"
   export MAESTRO_WOO_STORE_URL="url"
   ```

3. **CLI `-e` flags** (per-run, without prefix):
   ```bash
   maestro test -e WOO_EMAIL="..." -e WOO_PASSWORD="..." -e WOO_STORE_URL="..." flow.yaml
   ```

4. **CI secrets** (injected as env vars in the Buildkite pipeline, passed explicitly via `-e` flags).

### Credential sources

| Variable Group | Secret Store | Used By |
|---------------|-------------|---------|
| Primary store (`WOO_EMAIL`, `WOO_PASSWORD`, `WOO_STORE_URL`) | [Secret Store 8326](https://mc.a8c.com/secret-store/?secret_id=8326) | Most flows |
| Not-a-Woo-store (`WOO_NOT_WOO_*`) | [Secret Store 8326](https://mc.a8c.com/secret-store/?secret_id=8326) | `login_not_woo_store.yaml` |
| Wrong account (`WOO_WRONG_ACCOUNT_*`) | [Secret Store 8326](https://mc.a8c.com/secret-store/?secret_id=8326) | `login_wrong_account.yaml` |
| Jetpack store (`WOO_JETPACK_*`) | [Secret Store 8326](https://mc.a8c.com/secret-store/?secret_id=8326) | `login_jetpack.yaml` (optional) |

See `env.example` for the full list of variables and their descriptions.

## Tips

- Use `maestro studio` to visually inspect the view hierarchy and find element selectors.
- Use `maestro test -c flow.yaml` for continuous mode during development.
- Each flow is independent and starts with a fresh login (clearState: true).
- Screenshots are automatically saved at key checkpoints for visual verification.
- Flows use `optional: true` on interactions that may not be present on all store configurations.
- If 2FA is triggered on the Jetpack store, [unlock the account here](https://mc.a8c.com/tools/reportcard/user/?id=209835171).
