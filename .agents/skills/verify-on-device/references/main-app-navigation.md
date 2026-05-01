# Main App Navigation Reference

Navigation map for the WooCommerce store management app (everything outside POS).
Uses Fragment-based navigation with XML nav graphs inside `MainActivity`.

For detailed screen identifiers, workflows, and tracking events per tab, see:
- [Login](main-app-login.md) -- authentication and store selection
- [Dashboard](main-app-dashboard.md) -- stats, top performers, onboarding
- [Orders](main-app-orders.md) -- order list, **order creation (adding products to orders)**, payment collection (cash/card/tap-to-pay), fulfillment, refunds, shipping labels
- [Products](main-app-products.md) -- **product catalog management**: creating, editing, deleting products. NOT for adding products to orders (that's Orders)
- [More Menu](main-app-more.md) -- Payments hub, Reviews, Coupons, Settings

## Feature Tree

```
MainActivity
|
+-- Dashboard (My Store) tab
|   +-- Stats Card (revenue, visitors, orders)
|   +-- Top Performers Card
|   +-- Date Range Selector
|   +-- -> Analytics Hub
|   |   +-- Analytics Settings
|   +-- -> Store Onboarding
|   +-- -> Inbox
|   +-- -> Blaze Campaigns
|
+-- Orders tab
|   +-- Order List (filterable by status)
|   |   +-- Order Filters
|   |   +-- Search Orders
|   |   +-- Create Order (FAB)
|   |   +-- Barcode Scanning
|   +-- -> Order Detail
|       +-- Order Status Change
|       +-- Add Order Note
|       +-- Order Fulfillment
|       +-- -> Issue Refund
|       |   +-- Refund Detail
|       +-- -> Shipping Labels (Print, Create, Refund)
|       +-- -> Edit Order
|       +-- -> Customer/Address Editing
|       +-- -> Receipt Preview
|       +-- -> Card Payment Flow
|
+-- Products tab
|   +-- Product List (filterable, sortable)
|   |   +-- Product Filters
|   |   +-- Search Products
|   |   +-- Add Product (FAB)
|   |   +-- Scan to Update Inventory
|   +-- -> Product Detail
|       +-- Product Images (Gallery)
|       +-- Product Pricing
|       +-- Product Inventory
|       +-- -> Variation List -> Variation Detail
|       +-- -> Product Attributes
|       +-- -> Product Categories
|       +-- -> Product Reviews
|       +-- -> Product Downloads
|       +-- -> Product Subscriptions
|       +-- -> Product Add-ons
|       +-- -> Blaze Campaign Creation
|
+-- Bookings tab (if extension active)
|   +-- Booking List (filterable)
|   +-- -> Booking Detail
|
+-- Point of Sale tab (if enabled)
|   +-- -> WooPosActivity (see pos-navigation.md)
|
+-- Menu (More) tab
    +-- Payments -> Payments Hub
    |   +-- Card Reader Setup
    |   +-- Tap to Pay
    |   +-- Scan to Pay
    +-- Reviews -> Review List -> Review Detail
    +-- Coupons -> Coupon List -> Coupon Detail
    |   +-- Create/Edit Coupon
    +-- Customers -> Customer List -> Customer Detail
    +-- Subscriptions
    +-- Blaze -> Campaign List -> Campaign Detail
    +-- Google for WooCommerce
    +-- Settings (opens AppSettingsActivity)
        +-- Privacy Settings
        +-- Account Settings
        +-- Notification Settings
        +-- Experimental Features (Beta)
        +-- Developer Options (debug only)
        +-- Plugins
        +-- Theme Picker
        +-- About
```

## Global Elements

| Element | Resource ID | Notes |
|---------|------------|-------|
| Bottom Navigation Bar | `bottom_nav` | Visible on top-level screens, hidden on detail screens |
| Toolbar | `toolbar` | Material toolbar, shows screen title |
| Navigation Host | `nav_host_fragment_main` | Container for all fragments |
| Offline Bar | `offline_bar` | Visible only when device is offline |

## Bottom Navigation Tabs

| Tab | Resource ID | Label | Target Screen |
|-----|------------|-------|---------------|
| My Store | `dashboard` | "My store" | Dashboard |
| Orders | `orders` | "Orders" | Orders List |
| Products | `products` | "Products" | Products List |
| Bookings | `bookings` | "Bookings" | Bookings List (only if extension active) |
| Point of Sale | `point_of_sale` | "Point of Sale" | POS (only if enabled) |
| Menu | `moreMenu` | "Menu" | More Menu |

To navigate between tabs, find the target tab by its `identifier`, compute center coordinates, and tap. The active tab has `selected: true` in the accessibility tree.
