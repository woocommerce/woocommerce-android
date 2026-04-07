# Products Tab (Product Catalog Management)

**Scope:** This reference covers the **Products tab** — managing the product catalog (creating, editing, deleting products). If the task involves **adding products to an order** or **creating orders with products**, see [Orders](main-app-orders.md) instead.

Fragment: `ProductListFragment` -- Tap `products` bottom tab.
Logcat events are prefixed with `woocommerceandroid_`.

## Screen Identifiers

**Products List**

| Key Element | Identifier | Notes |
|-------------|-----------|-------|
| **Primary** | `productsRecycler` | Products RecyclerView |
| Add product FAB | `addProductButton` | contentDescription: "Add product" |
| Sort/filter card | `products_sort_filter_card` | Filter/sort controls |
| Empty view | `empty_view` | Shown when no products match |

**Product Detail** -- Fragment: `ProductDetailFragment` -- tap any product row

| Key Element | Identifier | Notes |
|-------------|-----------|-------|
| **Primary** | `productDetail_root` | Root CoordinatorLayout |
| Toolbar | `productDetailToolbar` | Shows product name |
| Image gallery | `imageGallery` | Product image carousel |
| Product cards | `cardsRecyclerView` | Product detail cards (scrollable) |
| Add more container | `productDetail_addMoreContainer` | Bottom add-more section |

## Workflows

### Products List

| Step | Action | Element | Logcat Event |
|------|--------|---------|-------------|
| 1 | Tap "Products" tab | id: `products` | `product_list_loaded` |
| 2 | Tap a product | product row in `productsRecycler` | `product_list_product_tapped` |
| 3 | Search products | tap search icon in toolbar | `product_list_menu_search_tapped` |
| 4 | Filter products | tap filter in `products_sort_filter_card` | `product_list_view_filter_options_tapped` |
| 5 | Sort products | tap sort in `products_sort_filter_card` | `product_list_sorting_option_selected` |
| 6 | Scan barcode | tap barcode icon | `product_list_product_barcode_scanning_tapped` |

### Product Creation

| Step | Action | Element | Logcat Event |
|------|--------|---------|-------------|
| 1 | Tap Add Product FAB | id: `addProductButton` | `product_list_add_product_button_tapped` |
| 2 | Select product type | type selection bottom sheet | |
| 3 | (Optional) Use AI to generate | AI prompt screen | `product_creation_ai_generate_details_tapped` |
| 4 | Fill product details | product detail fields | |
| 5 | Tap "Publish" | publish button | `add_product_publish_tapped` |

### Product Detail Sub-Screens

All reachable by tapping the corresponding section on the Product Detail screen.

| Sub-Screen | How to Navigate | Logcat Event |
|------------|----------------|-------------|
| Images | tap image gallery or "Add image" | `product_detail_image_tapped` |
| Pricing | tap "Price" section | `product_detail_view_price_settings_tapped` |
| Inventory | tap "Inventory" section | `product_detail_view_inventory_settings_tapped` |
| Shipping | tap "Shipping" section | `product_detail_view_shipping_settings_tapped` |
| Variations | tap "Variations" section | `product_detail_view_product_variants_tapped` |
| Attributes | tap "Attributes" in variations | `product_variation_details_attributes_tapped` |
| Categories | tap "Categories" section | `product_detail_view_categories_tapped` |
| Tags | tap "Tags" section | `product_detail_view_tags_tapped` |
| Reviews | tap "Reviews" section | `product_detail_view_product_reviews_tapped` |
| Downloads | tap "Downloadable files" section | `product_detail_view_downloadable_files_tapped` |
| Linked products | tap "Linked products" section | `product_detail_view_linked_products_tapped` |
| Grouped products | tap "Grouped products" section | `product_detail_view_grouped_products_tapped` |
| Bundled products | tap "Bundled products" section | `product_detail_view_bundled_products_tapped` |
| Components | tap "Components" section (composite) | `product_details_view_components_tapped` |
| Subscriptions | tap "Subscription" section | `product_details_view_subscription_expiration_tapped` |
| Add-ons | tap "Add-ons" section | `product_addons_product_detail_view_product_addons_tapped` |
| Quantity rules | tap "Quantity rules" section | `product_detail_view_quantity_rules_tapped` |
| External link | tap "External link" section | `product_detail_view_external_product_link_tapped` |
| Custom fields | tap "Custom fields" button | `product_detail_custom_fields_tapped` |
| Description (rich editor) | tap "Description" section | `product_detail_view_product_description_tapped` |
| Short description | tap "Short description" section | `product_detail_view_short_description_tapped` |

### Product Update/Delete

| Step | Action | Element | Logcat Event |
|------|--------|---------|-------------|
| 1 | Edit fields | various product sections | |
| 2 | Tap "Update" / "Save" | toolbar save button | `product_detail_update_success` |
| 3 | Duplicate product | toolbar menu -> "Duplicate" | `product_detail_duplicate_button_tapped` |
| 4 | Share product | toolbar menu -> "Share" | `product_detail_share_button_tapped` |
| 5 | Delete product | toolbar menu -> "Trash" | `product_detail_product_deleted` |

### Variations (Variable Products)

| Step | Action | Element | Logcat Event |
|------|--------|---------|-------------|
| 1 | Tap "Variations" on product detail | variations section | `product_detail_view_product_variants_tapped` |
| 2 | Tap a variation | variation row | `product_variation_view_variation_detail_tapped` |
| 3 | Add new variation | add button | |
| 4 | Bulk update price | bulk update option | |

### Scan to Update Inventory

| Step | Action | Element | Logcat Event |
|------|--------|---------|-------------|
| 1 | Tap barcode scan icon on product list | barcode icon | |
| 2 | Scan product barcode | camera scanner | |
| 3 | Update inventory count | inventory bottom sheet | |

### AI Product Description

| Step | Action | Element | Logcat Event |
|------|--------|---------|-------------|
| 1 | Tap AI generate on description field | AI button | `product_creation_ai_generate_details_tapped` |
| 2 | Enter product prompt / details | prompt text field | |
| 3 | Generate and review | generated text | `product_creation_ai_generate_product_details_success` |

### Blaze Campaign (from Product)

| Step | Action | Element | Logcat Event |
|------|--------|---------|-------------|
| 1 | Tap "Promote with Blaze" on product detail | Blaze button | `blaze_entry_point_tapped` |
| 2 | Follow Blaze campaign creation flow | see More Menu > Blaze | |
