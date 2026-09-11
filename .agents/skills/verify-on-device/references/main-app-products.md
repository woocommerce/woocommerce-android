# Products Tab (Product Catalog Management)

**Scope:** This reference covers the **Products tab** — managing the product catalog (creating, editing, deleting products). If the task involves **adding products to an order** or **creating orders with products**, see [Orders](main-app-orders.md) instead.

Fragment: `ProductListFragment` -- Tap `products` bottom tab.

## Screen Identifiers

**Products List** -- Compose. Test tags are declared in `ProductListTestTags.kt` and surface in the accessibility tree as `resource-id` **verbatim**, with no `com.woocommerce.android.dev:id/` prefix.

| Key Element | Identifier | Notes |
|-------------|-----------|-------|
| **Primary** | `product_list_screen` | Compose root of the products list |
| Product list | `product_list` | LazyColumn holding the product rows |
| Product row | `product_list_row_<remoteId>` | One per product; `<remoteId>` is the remote product ID |
| Add product FAB | `product_list_add_action` | contentDescription: "Add products". Wrapped in `product_list_add_fab` |
| Control rail | `product_list_control_rail` | Horizontal row holding the sort and filter chips |
| Sort chip | `product_list_sort` | Label is the active sorting title |
| Filters chip | `product_list_filters` | `stateDescription` reports the active filter count |
| Search icon | `product_list_search_action` | Opens the search header |
| Search field | `product_list_search_field` | With `product_list_search_all` / `product_list_search_sku` tabs below it |
| Barcode icon | `product_list_barcode_action` | Only shown when barcode scanning is available |
| Empty view | `product_list_empty` | Shown when no products match |
| Selection header | `product_list_selection_header` | Replaces the top app bar in multi-select mode |

The empty view's "Add product" button carries the same `product_list_add_action` tag as the FAB. Only one of the two is on screen at a time, but match on the enclosing `product_list_empty` when you need to disambiguate.

**Product Detail** -- Fragment: `ProductDetailFragment` -- tap any product row

| Key Element | Identifier | Notes |
|-------------|-----------|-------|
| **Primary** | `productDetail_root` | Programmatic root ComposeView resource ID |
| Page | `productDetailPage` | Complete Compose hierarchy |
| Top app bar | `productDetailToolbar` | Compose semantics tag; shows product name |
| Top app bar overflow | `productDetailToolbarOverflow` | Compose semantics tag |
| Image gallery | `productDetailImageGallery` | Compose semantics tag |
| Product cards | `productDetailList` | Compose LazyColumn semantics tag |
| Add more footer | `productDetailFooter` | Fixed Compose footer semantics tag |

## Workflows

### Products List

| Step | Action | Element |
|------|--------|---------|
| 1 | Tap "Products" tab | id: `products` |
| 2 | Tap a product | product row in `product_list` -- `product_list_row_<remoteId>` |
| 3 | Search products | tap `product_list_search_action`, then type into `product_list_search_field` |
| 4 | Search by SKU | in the search header, tap the `product_list_search_sku` tab |
| 5 | Filter products | tap `product_list_filters` |
| 6 | Sort products | tap `product_list_sort` |
| 7 | Scan barcode | tap `product_list_barcode_action` |

### Product Creation

| Step | Action | Element |
|------|--------|---------|
| 1 | Tap Add Product FAB | `product_list_add_action` |
| 2 | Select product type | type selection bottom sheet |
| 3 | (Optional) Use AI to generate | AI prompt screen |
| 4 | Fill product details | product detail fields |
| 5 | Tap "Publish" | publish button |

### Product Detail Sub-Screens

All reachable by tapping the corresponding section on the Product Detail screen.

| Sub-Screen | How to Navigate |
|------------|----------------|
| Images | tap image gallery or "Add image" |
| Pricing | tap "Price" section |
| Inventory | tap "Inventory" section |
| Shipping | tap "Shipping" section |
| Variations | tap "Variations" section |
| Attributes | tap "Attributes" in variations |
| Categories | tap "Categories" section |
| Tags | tap "Tags" section |
| Reviews | tap "Reviews" section |
| Downloads | tap "Downloadable files" section |
| Linked products | tap "Linked products" section |
| Grouped products | tap "Grouped products" section |
| Bundled products | tap "Bundled products" section |
| Components | tap "Components" section (composite) |
| Subscriptions | tap "Subscription" section |
| Add-ons | tap "Add-ons" section |
| Quantity rules | tap "Quantity rules" section |
| External link | tap "External link" section |
| Custom fields | tap "Custom fields" button |
| Description (rich editor) | tap "Description" section |
| Short description | tap "Short description" section |

### Product Update/Delete

| Step | Action | Element |
|------|--------|---------|
| 1 | Edit fields | various product sections |
| 2 | Tap "Update" / "Save" | top app bar save button |
| 3 | Duplicate product | top app bar menu -> "Duplicate" |
| 4 | Share product | top app bar menu -> "Share" |
| 5 | Delete product | top app bar menu -> "Trash" |

### Variations (Variable Products)

| Step | Action | Element |
|------|--------|---------|
| 1 | Tap "Variations" on product detail | variations section |
| 2 | Tap a variation | variation row |
| 3 | Add new variation | add button |
| 4 | Bulk update price | bulk update option |

### Scan to Update Inventory

| Step | Action | Element |
|------|--------|---------|
| 1 | Tap barcode scan icon on product list | `product_list_barcode_action` |
| 2 | Scan product barcode | camera scanner |
| 3 | Update inventory count | inventory bottom sheet |

### AI Product Description

| Step | Action | Element |
|------|--------|---------|
| 1 | Tap AI generate on description field | AI button |
| 2 | Enter product prompt / details | prompt text field |
| 3 | Generate and review | generated text |

### Blaze Campaign (from Product)

| Step | Action | Element |
|------|--------|---------|
| 1 | Tap "Promote with Blaze" on product detail | Blaze button |
| 2 | Follow Blaze campaign creation flow | see More Menu > Blaze |
