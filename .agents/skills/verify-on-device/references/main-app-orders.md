# Orders Tab (Order Creation, Payment Collection, Fulfillment)

**Scope:** This reference covers everything in the **Orders tab** — viewing orders, **creating orders (including adding products to orders)**, **collecting payment (cash, card, tap-to-pay, payment links)**, fulfillment, refunds, shipping labels, and receipts.

Fragment: `OrderListFragment` -- Tap `orders` bottom tab.

## Screen Identifiers

**Orders List**

| Key Element | Identifier | Notes |
|-------------|-----------|-------|
| **Primary** | `order_list_view` | Order list RecyclerView |
| Filters card | `order_filters_card` | Status filter chips |
| Create order FAB | `createOrderButton` | contentDescription: "Create order" |

**Order Detail** -- Fragment: `OrderDetailFragment` -- tap any order row

| Key Element | Identifier | Notes |
|-------------|-----------|-------|
| **Primary** | `orderDetail_container` | Main content container |
| Order status | `orderDetail_orderStatus` | Status card at top |
| Product list | `orderDetail_productList` | Products in the order |
| Payment info | `orderDetail_paymentInfo` | Payment details card |
| Customer info | `orderDetail_customerInfo` | Customer details card |
| Refunds info | `orderDetail_refundsInfo` | Visible if refunds exist |
| Notes list | `orderDetail_noteList` | Order notes section |
| Shipment tracking | `orderDetail_shipmentList` | Shipment tracking section |
| Shipping labels | `orderDetail_shippingLabelList` | Shipping labels section |

## Workflows

### Orders List

| Step | Action | Element |
|------|--------|---------|
| 1 | Tap "Orders" tab | id: `orders` |
| 2 | Tap an order | order row in `order_list_view` |
| 3 | Filter by status | tap chips in `order_filters_card` |
| 4 | Search orders | tap search icon in toolbar |
| 5 | Create new order | FAB: `createOrderButton` |

**Tablet create order:** On tablets, the product selector and order summary are side-by-side. After selecting products, tap **"Recalculate"** in the totals section — totals show $0.00 until recalculated, and "Collect Payment" only appears after recalculation. On phones, the product selector is a separate screen.

### Order Status Change

| Step | Action | Element |
|------|--------|---------|
| 1 | Tap order status card | id: `orderDetail_orderStatus` |
| 2 | Select new status | status list dialog |

### Issue Refund Flow

| Step | Action | Element |
|------|--------|---------|
| 1 | Tap "Issue Refund" on order detail | text: "Issue Refund" |
| 2 | Select items to refund | item list with quantity selectors |
| 3 | Tap "Next" | next button |
| 4 | Review refund summary | refund amount, reason field, method |
| 5 | Tap "Issue Refund" | refund button |

### Add Order Note

| Step | Action | Element |
|------|--------|---------|
| 1 | Tap "Add note" on order detail | add note button |
| 2 | Enter note text | note text field |
| 3 | Toggle "Email to customer" | email toggle switch |
| 4 | Tap "Add" | add button |

### Shipment Tracking

| Step | Action | Element |
|------|--------|---------|
| 1 | Tap "Add tracking" on order detail | add tracking button |
| 2 | Select carrier/provider | provider list |
| 3 | Enter tracking number | tracking number field |
| 4 | Tap "Add" | add button |

### Order Fulfillment

| Step | Action | Element |
|------|--------|---------|
| 1 | Tap "Fulfill" on order detail | fulfill button |
| 2 | Review fulfillment details | fulfillment screen |
| 3 | Mark complete | complete button |

### Shipping Labels (Create)

| Step | Action | Element |
|------|--------|---------|
| 1 | Tap "Create Shipping Label" on order detail | create label button |
| 2 | Edit origin/destination address | address fields |
| 3 | Select/create package | package selector |
| 4 | Select carrier and rate | carrier rate list |
| 5 | (International) Fill customs form | customs form fields |
| 6 | Purchase label | purchase button |

### Shipping Labels (Print/Refund)

| Step | Action | Element |
|------|--------|---------|
| 1 | Tap existing shipping label | label row in shipping labels section |
| 2 | Tap "Print" | print button |
| 3 | Select paper size | paper size dialog |
| 4 | (Or) Tap "Refund" | refund button |

### Collect Payment

Payment method selection screen offers multiple options depending on store setup.

| Step | Action | Element |
|------|--------|---------|
| 1 | Tap "Collect Payment" on order detail | collect payment button |
| 2 | Select payment method | method selection screen |
| 2a | **Card Reader (Bluetooth)** | card reader option |
| 2b | **Tap to Pay (NFC)** | tap to pay option |
| 2c | **Cash** | cash option -> change due calculator |
| 2d | **Share Payment Link** | share link option -> Android share sheet |
| 2e | **Scan to Pay (QR)** | QR option -> displays QR code |
| 3 | Payment completed | success screen |

### Edit Order

| Step | Action | Element |
|------|--------|---------|
| 1 | Tap edit icon in toolbar | menu: `menu_edit_order` |
| 2 | Edit products, customer, shipping, fees, etc. | order creation/edit screen |
| 3 | Save changes | save button |

### Bulk Status Update (from Orders List)

| Step | Action | Element |
|------|--------|---------|
| 1 | Long-press to enter selection mode | order rows |
| 2 | Select orders | order checkboxes |
| 3 | Tap bulk update | update button |
| 4 | Confirm status change | confirmation dialog |

### Trash Order

| Step | Action | Element |
|------|--------|---------|
| 1 | Tap trash button on order detail | id: `orderDetail_trash` |
| 2 | Confirm deletion | confirmation dialog |

### Receipt

| Step | Action | Element |
|------|--------|---------|
| 1 | Tap "See receipt" on order detail | receipt button |
| 2 | Print receipt | print button |
| 3 | Email receipt | email button |

### Customer Contact (from Order Detail)

| Action | Element |
|--------|---------|
| Email customer | email icon in customer info card |
| Call customer | phone icon in customer info card |
| SMS customer | SMS option in phone menu |
| WhatsApp customer | WhatsApp option in phone menu |
| Telegram customer | Telegram option in phone menu |
| Edit shipping address | tap shipping address section |
| Edit billing address | tap billing address section |
