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
```

## Test Coverage vs P2 Smoke Testing Flows

| P2 Category | Flow File | Automated? | Notes |
|---|---|---|---|
| **Login** | | | |
| Successful store login | `login_successful.yaml` | Yes | |
| Not a WP site | `login_not_wp_site.yaml` | Yes | Uses google.com |
| Wrong credentials | `login_wrong_credentials.yaml` | Yes | |
| Not a Woo store | - | No | Requires separate test account |
| Passwordless login | - | No | Requires email inbox access |
| Social login (Apple/Google) | - | No | Requires external auth |
| Login with 2FA | - | No | Requires authenticator app |
| No Jetpack / Jetpack not connected | - | No | Requires Jurassic Ninja setup |
| **Dashboard/Stats** | | | |
| Charts, analytics, date ranges | `dashboard_stats.yaml` | Yes | |
| **Orders** | | | |
| List and pagination | `orders_list_and_search.yaml` | Yes | |
| Search | `orders_list_and_search.yaml` | Yes | |
| Filters | `orders_list_and_search.yaml` | Yes | |
| Create order (products, shipping, notes) | `orders_create.yaml` | Yes | |
| Order detail, notes, payment options | `orders_details_and_actions.yaml` | Yes | |
| Push notification for new order | - | No | Requires server trigger |
| Barcode scanner | - | No | Requires camera |
| Shipping label creation | - | No | Complex external flow |
| **Products** | | | |
| List and pagination | `products_list_and_sort.yaml` | Yes | |
| Sort and search | `products_list_and_sort.yaml` | Yes | |
| Filters | `products_list_and_sort.yaml` | Yes | |
| Product detail (all properties) | `products_detail.yaml` | Yes | |
| Create product | `products_create.yaml` | Yes | |
| Media upload | - | No | Requires device gallery |
| **Hub Menu** | | | |
| Settings (theme, beta features) | `hub_menu_settings.yaml` | Yes | |
| Payments (card reader, TTP) | `hub_menu_payments.yaml` | Yes | UI only, no hardware |
| Coupons (list + create) | `hub_menu_coupons.yaml` | Yes | |
| Customers + Inbox | `hub_menu_customers_inbox.yaml` | Yes | |
| WC Admin + View Store + Change store | `hub_menu_admin_and_store.yaml` | Yes | |
| **Blaze** | | | |
| Campaign creation flow | `blaze_campaign.yaml` | Yes | Triggers flow only |
| **Google for Woo** | | | |
| Campaign webview | `google_for_woo.yaml` | Yes | Verifies webview loads |
| **POS** | | | |
| Cash payment (tablet) | `pos_cash_payment.yaml` | Yes | Requires tablet |
| Card reader payment | - | No | Requires hardware |
| **Payments (hardware)** | | | |
| Card reader / TTP | - | No | Requires physical hardware |
| **Other** | | | |
| Language switching | - | No | Requires device settings |
| Widget | - | No | Requires home screen |
| Quick Actions | - | No | Requires app icon long press |
| Watch app | - | No | Requires Wear OS device |

## Credentials Handling

Credentials are **never hardcoded** in the YAML files. They are passed via:

1. **Environment variables** with `MAESTRO_` prefix (auto-available):
   ```bash
   export MAESTRO_WOO_EMAIL="email"
   export MAESTRO_WOO_PASSWORD="pass"
   export MAESTRO_WOO_STORE_URL="url"
   ```

2. **CLI `-e` flags** (per-run):
   ```bash
   maestro test -e WOO_EMAIL="..." -e WOO_PASSWORD="..." flow.yaml
   ```

3. **CI secrets** (injected as env vars in the pipeline).

## Tips

- Use `maestro studio` to visually inspect the view hierarchy and find element selectors.
- Use `maestro test -c flow.yaml` for continuous mode during development.
- Each flow is independent and starts with a fresh login (clearState: true).
- Screenshots are automatically saved at key checkpoints for visual verification.
- Flows use `optional: true` on interactions that may not be present on all store configurations.
