# Products Tab (Product Catalog Management)

**Scope:** This reference covers the **Products tab** — managing the product catalog (creating, editing, deleting products). If the task involves **adding products to an order** or **creating orders with products**, see [Orders](main-app-orders.md) instead.

Fragment: `ProductListFragment` -- Tap `products` bottom tab.

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

| Step | Action | Element |
|------|--------|---------|
| 1 | Tap "Products" tab | id: `products` |
| 2 | Tap a product | product row in `productsRecycler` |
| 3 | Search products | tap search icon in toolbar |
| 4 | Filter products | tap filter in `products_sort_filter_card` |
| 5 | Sort products | tap sort in `products_sort_filter_card` |
| 6 | Scan barcode | tap barcode icon |

### Product Creation

| Step | Action | Element |
|------|--------|---------|
| 1 | Tap Add Product FAB | id: `addProductButton` |
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
| 2 | Tap "Update" / "Save" | toolbar save button |
| 3 | Duplicate product | toolbar menu -> "Duplicate" |
| 4 | Share product | toolbar menu -> "Share" |
| 5 | Delete product | toolbar menu -> "Trash" |

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
| 1 | Tap barcode scan icon on product list | barcode icon |
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
