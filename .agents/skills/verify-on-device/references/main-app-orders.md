# Orders Tab (Order Creation, Payment Collection, Fulfillment)

**Scope:** This reference covers everything in the **Orders tab** — viewing orders, **creating orders (including adding products to orders)**, **collecting payment (cash, card, tap-to-pay, payment links)**, fulfillment, refunds, shipping labels, and receipts.

Fragment: `OrderListFragment` -- Tap `orders` bottom tab.
Logcat events are prefixed with `woocommerceandroid_`.

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

| Step | Action | Element | Logcat Event |
|------|--------|---------|-------------|
| 1 | Tap "Orders" tab | id: `orders` | `orders_list_loaded` |
| 2 | Tap an order | order row in `order_list_view` | `order_open` |
| 3 | Filter by status | tap chips in `order_filters_card` | |
| 4 | Search orders | tap search icon in toolbar | |
| 5 | Create new order | FAB: `createOrderButton` | `order_creation_success` |

**Tablet create order:** On tablets, the product selector and order summary are side-by-side. After selecting products, tap **"Recalculate"** in the totals section — totals show $0.00 until recalculated, and "Collect Payment" only appears after recalculation. On phones, the product selector is a separate screen.

### Order Status Change

| Step | Action | Element | Logcat Event |
|------|--------|---------|-------------|
| 1 | Tap order status card | id: `orderDetail_orderStatus` | |
| 2 | Select new status | status list dialog | `order_status_change_success` |

### Issue Refund Flow

| Step | Action | Element | Logcat Event |
|------|--------|---------|-------------|
| 1 | Tap "Issue Refund" on order detail | text: "Issue Refund" | |
| 2 | Select items to refund | item list with quantity selectors | `create_order_refund_next_button_tapped` |
| 3 | Tap "Next" | next button | |
| 4 | Review refund summary | refund amount, reason field, method | |
| 5 | Tap "Issue Refund" | refund button | `refund_create_success` |

### Add Order Note

| Step | Action | Element | Logcat Event |
|------|--------|---------|-------------|
| 1 | Tap "Add note" on order detail | add note button | `order_detail_add_note_button_tapped` |
| 2 | Enter note text | note text field | |
| 3 | Toggle "Email to customer" | email toggle switch | |
| 4 | Tap "Add" | add button | `order_note_add_success` |

### Shipment Tracking

| Step | Action | Element | Logcat Event |
|------|--------|---------|-------------|
| 1 | Tap "Add tracking" on order detail | add tracking button | |
| 2 | Select carrier/provider | provider list | `order_shipment_tracking_carrier_selected` |
| 3 | Enter tracking number | tracking number field | |
| 4 | Tap "Add" | add button | `order_tracking_add_success` |

### Order Fulfillment

| Step | Action | Element | Logcat Event |
|------|--------|---------|-------------|
| 1 | Tap "Fulfill" on order detail | fulfill button | `order_detail_fulfill_order_button_tapped` |
| 2 | Review fulfillment details | fulfillment screen | |
| 3 | Mark complete | complete button | `shipping_label_order_fulfill_succeeded` |

### Shipping Labels (Create)

| Step | Action | Element | Logcat Event |
|------|--------|---------|-------------|
| 1 | Tap "Create Shipping Label" on order detail | create label button | |
| 2 | Edit origin/destination address | address fields | |
| 3 | Select/create package | package selector | |
| 4 | Select carrier and rate | carrier rate list | |
| 5 | (International) Fill customs form | customs form fields | |
| 6 | Purchase label | purchase button | `shipping_label_purchase_flow` |

### Shipping Labels (Print/Refund)

| Step | Action | Element | Logcat Event |
|------|--------|---------|-------------|
| 1 | Tap existing shipping label | label row in shipping labels section | |
| 2 | Tap "Print" | print button | `shipping_label_print_requested` |
| 3 | Select paper size | paper size dialog | |
| 4 | (Or) Tap "Refund" | refund button | `shipping_label_refund_requested` |

### Collect Payment

Payment method selection screen offers multiple options depending on store setup.

| Step | Action | Element | Logcat Event |
|------|--------|---------|-------------|
| 1 | Tap "Collect Payment" on order detail | collect payment button | `payments_flow_order_collect_payment_tapped` |
| 2 | Select payment method | method selection screen | |
| 2a | **Card Reader (Bluetooth)** | card reader option | `payments_flow_collect` |
| 2b | **Tap to Pay (NFC)** | tap to pay option | `tap_to_pay_summary_shown` |
| 2c | **Cash** | cash option -> change due calculator | |
| 2d | **Share Payment Link** | share link option -> Android share sheet | |
| 2e | **Scan to Pay (QR)** | QR option -> displays QR code | |
| 3 | Payment completed | success screen | `payments_flow_completed` |

### Edit Order

| Step | Action | Element | Logcat Event |
|------|--------|---------|-------------|
| 1 | Tap edit icon in toolbar | menu: `menu_edit_order` | `order_edit_button_tapped` |
| 2 | Edit products, customer, shipping, fees, etc. | order creation/edit screen | |
| 3 | Save changes | save button | `order_detail_edit_flow_completed` |

### Bulk Status Update (from Orders List)

| Step | Action | Element | Logcat Event |
|------|--------|---------|-------------|
| 1 | Long-press to enter selection mode | order rows | `orders_list_bulk_update_selection_enabled` |
| 2 | Select orders | order checkboxes | |
| 3 | Tap bulk update | update button | `orders_list_bulk_update_requested` |
| 4 | Confirm status change | confirmation dialog | `orders_list_bulk_update_success` |

### Trash Order

| Step | Action | Element | Logcat Event |
|------|--------|---------|-------------|
| 1 | Tap trash button on order detail | id: `orderDetail_trash` | `order_detail_trash_tapped` |
| 2 | Confirm deletion | confirmation dialog | |

### Receipt

| Step | Action | Element | Logcat Event |
|------|--------|---------|-------------|
| 1 | Tap "See receipt" on order detail | receipt button | `receipt_view_tapped` |
| 2 | Print receipt | print button | `receipt_print_tapped` |
| 3 | Email receipt | email button | `receipt_email_tapped` |

### Customer Contact (from Order Detail)

| Action | Element | Logcat Event |
|--------|---------|-------------|
| Email customer | email icon in customer info card | `order_detail_customer_info_email_menu_email_tapped` |
| Call customer | phone icon in customer info card | `order_detail_customer_info_phone_menu_phone_tapped` |
| SMS customer | SMS option in phone menu | `order_detail_customer_info_phone_menu_sms_tapped` |
| WhatsApp customer | WhatsApp option in phone menu | `order_detail_customer_info_phone_menu_whatsapp_tapped` |
| Telegram customer | Telegram option in phone menu | `order_detail_customer_info_phone_menu_telegram_tapped` |
| Edit shipping address | tap shipping address section | |
| Edit billing address | tap billing address section | |
