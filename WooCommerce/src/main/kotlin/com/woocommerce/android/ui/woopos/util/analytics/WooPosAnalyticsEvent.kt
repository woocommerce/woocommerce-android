package com.woocommerce.android.ui.woopos.util.analytics

import com.woocommerce.android.analytics.AnalyticsTracker
import com.woocommerce.android.analytics.IAnalyticsEvent
import com.woocommerce.android.ui.woopos.home.cart.WooPosCartItemViewState
import com.woocommerce.android.ui.woopos.home.items.WooPosItemsViewModel
import com.woocommerce.android.ui.woopos.home.items.products.WooPosProductsDataSource.SyncStrategy
import com.woocommerce.android.ui.woopos.tab.WooPosLaunchability
import com.woocommerce.android.ui.woopos.util.ConnectionType
import com.woocommerce.android.ui.woopos.util.analytics.WooPosAnalyticsEventConstant.CartSource
import com.woocommerce.android.ui.woopos.util.analytics.WooPosAnalyticsEventConstant.ItemsHeaderType
import com.woocommerce.android.ui.woopos.util.analytics.WooPosAnalyticsEventConstant.ItemsListItemType
import com.woocommerce.android.ui.woopos.util.analytics.WooPosAnalyticsEventConstant.ItemsListProductType
import com.woocommerce.android.ui.woopos.util.analytics.WooPosAnalyticsEventConstant.ItemsListSource
import com.woocommerce.android.ui.woopos.util.analytics.WooPosAnalyticsEventConstant.ItemsListSourceType
import com.woocommerce.android.ui.woopos.util.analytics.WooPosAnalyticsEventConstant.RefundFlow
import com.woocommerce.android.ui.woopos.util.analytics.WooPosAnalyticsEventConstant.SyncErrorType
import com.woocommerce.android.ui.woopos.util.analytics.WooPosAnalyticsEventConstant.SyncSkipReason
import com.woocommerce.android.ui.woopos.util.analytics.WooPosAnalyticsEventConstant.SyncType
import kotlin.reflect.KClass

sealed class WooPosAnalyticsEvent : IAnalyticsEvent {
    override val siteless: Boolean = false
    override val isPosEvent: Boolean = true

    private val _properties: MutableMap<String, String> = mutableMapOf()
    val properties: Map<String, String> get() = _properties.toMap()

    fun addProperties(additionalProperties: Map<String, String>) {
        _properties.putAll(additionalProperties)
    }

    sealed class Error : WooPosAnalyticsEvent() {
        abstract val errorContext: KClass<out Any>
        abstract val errorType: String?
        abstract val errorDescription: String?

        data class OrderCreationError(
            override val errorContext: KClass<out Any>,
            override val errorType: String?,
            override val errorDescription: String?,
        ) : Error() {
            override val name: String = "order_creation_failed"
        }
    }

    sealed class Event : WooPosAnalyticsEvent() {
        data object BackToCartTapped : Event() {
            override val name: String = "back_to_cart_tapped"
        }

        data object BackToCheckoutFromCash : Event() {
            override val name: String = "back_to_checkout_from_cash"
        }

        data object CashCollectPaymentSuccess : Event() {
            override val name: String = "cash_collect_payment_success"
        }

        data object CheckoutCashPaymentTapped : Event() {
            override val name: String = "checkout_cash_payment_tapped"
        }

        data object CheckoutTapToPayPaymentTapped : Event() {
            override val name: String = "checkout_tap_to_pay_payment_tapped"
        }

        data object CashPaymentTapped : Event() {
            override val name: String = "cash_payment_tapped"
        }

        data object CashPaymentFailed : Event() {
            override val name: String = "cash_payment_failed"
        }

        data object CheckoutScanToPayPaymentTapped : Event() {
            override val name: String = "checkout_scan_to_pay_payment_tapped"
        }

        data object ScanToPayPaymentDetectedViaPolling : Event() {
            override val name: String = "scan_to_pay_payment_detected_via_polling"
        }

        data object ScanToPayCollectPaymentSuccess : Event() {
            override val name: String = "scan_to_pay_collect_payment_success"
        }

        data object ScanToPayPaymentFailed : Event() {
            override val name: String = "scan_to_pay_payment_failed"
        }

        data object BackToCheckoutFromScanToPay : Event() {
            override val name: String = "back_to_checkout_from_scan_to_pay"
        }

        data object CheckoutMarkAsPaidTapped : Event() {
            override val name: String = "checkout_mark_as_paid_tapped"
        }

        data object MarkAsPaidConfirmed : Event() {
            override val name: String = "mark_as_paid_confirmed"
        }

        data object MarkAsPaidSuccess : Event() {
            override val name: String = "mark_as_paid_success"
        }

        data object MarkAsPaidFailed : Event() {
            override val name: String = "mark_as_paid_failed"
        }

        data object BackToCheckoutFromMarkAsPaid : Event() {
            override val name: String = "back_to_checkout_from_mark_as_paid"
        }

        data class CheckoutTapped(val productsInCart: Int, val couponsInCart: Int) : Event() {
            override val name: String = "checkout_tapped"

            init {
                addProperties(
                    mapOf(
                        "products_in_cart" to productsInCart.toString(),
                        "coupons_in_cart" to couponsInCart.toString()
                    )
                )
            }
        }

        data object ClearCartTapped : Event() {
            override val name: String = "clear_cart_tapped"
        }

        data object CustomAmountEntryRowTapped : Event() {
            override val name: String = "custom_amount_entry_row_tapped"
        }

        data class CustomAmountSubmitted(
            val mode: Mode,
            val isTaxable: Boolean,
        ) : Event() {
            override val name: String = "custom_amount_submitted"

            init {
                addProperties(
                    mapOf(
                        "mode" to mode.value,
                        "is_taxable" to isTaxable.toString(),
                    )
                )
            }

            enum class Mode(val value: String) {
                ADD("add"),
                EDIT("edit"),
            }
        }

        data object CreateNewOrderTapped : Event() {
            override val name: String = "create_new_order_tapped"
        }

        data object EmailReceiptTapped : Event() {
            override val name: String = "receipt_email_tapped"
        }

        data object EmailReceiptSendTapped : Event() {
            override val name: String = "receipt_email_send_tapped"
        }

        data class EmailReceiptSendFailed(val eligibleForPOSReceipts: Boolean) : Event() {
            override val name: String = "receipt_email_failed"

            init {
                addProperties(
                    mapOf(
                        "eligible_for_pos_receipt" to eligibleForPOSReceipts.toString()
                    )
                )
            }
        }

        data class EmailReceiptSendSuccess(val eligibleForPOSReceipts: Boolean) : Event() {
            override val name: String = "receipt_email_success"

            init {
                addProperties(
                    mapOf(
                        "eligible_for_pos_receipt" to eligibleForPOSReceipts.toString()
                    )
                )
            }
        }

        data class TabVisibilityChecked(val isVisible: Result<Boolean>) : Event() {
            override val name: String = "tab_visibility_checked"

            init {
                val value: String = isVisible.fold(
                    onSuccess = { it.toString() },
                    onFailure = { "unknown" }
                )

                addProperties(mapOf("is_visible" to value))
            }
        }

        data object ExitTapped : Event() {
            override val name: String = "exit_menu_item_tapped"
        }

        data object ExitConfirmed : Event() {
            override val name: String = "exit_confirmed"
        }

        data object InteractionWithCustomerStarted : Event() {
            override val name: String = "interaction_with_customer_started"
        }

        data object GoToOrdersTapped : Event() {
            override val name: String = "orders_menu_item_tapped"
        }

        data object OrdersListPullToRefreshTriggered : Event() {
            override val name: String = "orders_list_pull_to_refresh"
        }

        data object OrdersListNextPageLoaded : Event() {
            override val name: String = "orders_list_next_page_loaded"
        }

        data object OrderDetailsEmailReceiptTapped : Event() {
            override val name: String = "order_details_email_receipt_tapped"
        }

        data class OrdersListRowTapped(
            val orderId: Long,
            val orderStatus: String,
            val listPosition: Int,
            val daysSinceCreated: Int
        ) : Event() {
            override val name: String = "orders_list_row_tapped"

            init {
                addProperties(
                    mapOf(
                        "order_id" to orderId.toString(),
                        "order_status" to orderStatus,
                        "list_position" to listPosition.toString(),
                        "days_since_created" to daysSinceCreated.toString()
                    )
                )
            }
        }

        data class OrderDetailsLoaded(
            val orderId: Long,
            val orderStatus: String,
            val daysSinceCreated: Int
        ) : Event() {
            override val name: String = "pos_order_details_loaded"

            init {
                addProperties(
                    mapOf(
                        "order_id" to orderId.toString(),
                        "order_status" to orderStatus,
                        "days_since_created" to daysSinceCreated.toString()
                    )
                )
            }
        }

        data class OrdersListFetched(val milimetersSinceRequestSent: Long) : Event() {
            override val name: String = "orders_list_fetched"

            init {
                addProperties(
                    mapOf(
                        "milliseconds_since_request_sent" to milimetersSinceRequestSent.toString()
                    )
                )
            }
        }

        data class OrdersListSearchResultsFetched(val millisecondsSinceRequestSent: Long) : Event() {
            override val name: String = "pos_orders_list_search_results_fetched"

            init {
                addProperties(
                    mapOf(
                        "milliseconds_since_request_sent" to millisecondsSinceRequestSent.toString()
                    )
                )
            }
        }

        data object OrdersListSearchButtonTapped : Event() {
            override val name: String = "pos_orders_list_search_button_tapped"
        }

        data class BarcodeScanned(
            val scanDurationMs: Long,
            val barcodeLength: Int,
            val scannerInfo: String?,
        ) : Event() {
            override val name: String = "barcode_scanned"

            init {
                addProperties(
                    mapOf(
                        "barcode_length" to barcodeLength.toString(),
                        "scan_duration_ms" to scanDurationMs.toString(),
                        "scanner_info" to (scannerInfo ?: "unknown")
                    )
                )
            }
        }

        data class BarcodeScanningFailed(
            val scanDurationMs: Long,
            val barcodeLength: Int,
            val scannerInfo: String?,
            val failReason: String,
        ) : Event() {
            override val name: String = "barcode_scanning_failed"

            init {
                addProperties(
                    mapOf(
                        "barcode_length" to barcodeLength.toString(),
                        "scan_duration_ms" to scanDurationMs.toString(),
                        "scanner_info" to (scannerInfo ?: "unknown"),
                        "fail_reason" to failReason
                    )
                )
            }
        }

        data object CouponsCreateTapped : Event() {
            override val name: String = "coupons_create_tapped"
        }

        data object CouponCreationInitiated : PaymentFlowTrackerEvent() {
            override val name: String = "coupon_creation_initiated"
        }

        data object CouponCreationSuccess : PaymentFlowTrackerEvent() {
            override val name: String = "coupon_creation_success"
        }

        data object CouponCreationFailed : PaymentFlowTrackerEvent() {
            override val name: String = "coupon_creation_failed"
        }

        data object CouponsLoadFailed : PaymentFlowTrackerEvent() {
            override val name: String = "coupons_load_failed"
        }

        @ExposedCopyVisibility
        data class ItemAddedToCart private constructor(
            val source: ItemsListSource?,
            val sourceType: ItemsListSourceType,
            val itemType: ItemsListItemType,
            val productType: ItemsListProductType?,
            val error: String? = null
        ) : Event() {
            override val name: String = "item_added_to_cart"

            constructor(
                source: ItemsListSource,
                sourceType: ItemsListSourceType,
                item: WooPosItemsViewModel.ItemClickedData
            ) : this(
                source,
                sourceType,
                itemType = when (item) {
                    is WooPosItemsViewModel.ItemClickedData.Product -> ItemsListItemType.PRODUCT
                    is WooPosItemsViewModel.ItemClickedData.Coupon -> ItemsListItemType.COUPON
                    is WooPosItemsViewModel.ItemClickedData.CustomAmount -> ItemsListItemType.CUSTOM_AMOUNT
                    is WooPosItemsViewModel.ItemClickedData.VariableProduct -> {
                        error("VariableProduct is not a valid item type")
                    }
                },
                productType = when (item) {
                    is WooPosItemsViewModel.ItemClickedData.Product.Simple -> ItemsListProductType.SIMPLE
                    is WooPosItemsViewModel.ItemClickedData.Product.Variation -> ItemsListProductType.VARIATION
                    is WooPosItemsViewModel.ItemClickedData.Coupon -> null
                    is WooPosItemsViewModel.ItemClickedData.CustomAmount -> null
                    is WooPosItemsViewModel.ItemClickedData.VariableProduct -> {
                        error("VariableProduct is not a valid item type")
                    }
                }
            )

            constructor(item: WooPosCartItemViewState) : this(
                source = null,
                sourceType = if (item is WooPosCartItemViewState.CustomAmount) {
                    ItemsListSourceType.CUSTOM_AMOUNT_FORM
                } else {
                    ItemsListSourceType.BARCODE_SCANNER
                },
                itemType = when (item) {
                    is WooPosCartItemViewState.Loading -> ItemsListItemType.LOADING
                    is WooPosCartItemViewState.Coupon -> ItemsListItemType.COUPON
                    is WooPosCartItemViewState.CustomAmount -> ItemsListItemType.CUSTOM_AMOUNT
                    is WooPosCartItemViewState.Product.Simple -> ItemsListItemType.PRODUCT
                    is WooPosCartItemViewState.Product.Variation -> ItemsListItemType.PRODUCT
                    is WooPosCartItemViewState.Error -> ItemsListItemType.ERROR
                },
                productType = when (item) {
                    is WooPosCartItemViewState.Coupon,
                    is WooPosCartItemViewState.CustomAmount,
                    is WooPosCartItemViewState.Error,
                    is WooPosCartItemViewState.Loading -> null

                    is WooPosCartItemViewState.Product.Simple -> ItemsListProductType.SIMPLE
                    is WooPosCartItemViewState.Product.Variation -> ItemsListProductType.VARIATION
                },
                error = if (item is WooPosCartItemViewState.Error) item.message else null
            )

            init {
                addProperties(
                    buildMap {
                        if (source != null) {
                            put(ItemsListSource.SOURCE, source.toString())
                        }
                        put(ItemsListSourceType.SOURCE_TYPE, sourceType.toString())
                        put(ItemsListItemType.ITEM_TYPE, itemType.toString())
                        if (productType != null) {
                            put(ItemsListProductType.PRODUCT_TYPE, productType.toString())
                        }
                        if (error != null) {
                            put("error", error)
                        }
                    }
                )
            }
        }

        @ExposedCopyVisibility
        data class ItemRemovedFromCart private constructor(
            val source: CartSource,
            val itemType: ItemsListItemType,
            val productType: ItemsListProductType?
        ) : Event() {
            constructor(
                source: CartSource,
                item: WooPosCartItemViewState
            ) : this(
                source,
                itemType = when (item) {
                    is WooPosCartItemViewState.Product -> ItemsListItemType.PRODUCT
                    is WooPosCartItemViewState.Coupon -> ItemsListItemType.COUPON
                    is WooPosCartItemViewState.CustomAmount -> ItemsListItemType.CUSTOM_AMOUNT
                    is WooPosCartItemViewState.Error -> ItemsListItemType.ERROR
                    is WooPosCartItemViewState.Loading -> ItemsListItemType.LOADING
                },
                productType = when (item) {
                    is WooPosCartItemViewState.Product.Simple -> ItemsListProductType.SIMPLE
                    is WooPosCartItemViewState.Product.Variation -> ItemsListProductType.VARIATION
                    is WooPosCartItemViewState.Coupon,
                    is WooPosCartItemViewState.CustomAmount,
                    is WooPosCartItemViewState.Error,
                    is WooPosCartItemViewState.Loading -> null
                }
            )

            override val name: String = "item_removed_from_cart"

            init {
                addProperties(
                    buildMap {
                        put(CartSource.CART_SOURCE, source.toString())
                        put(ItemsListItemType.ITEM_TYPE, itemType.toString())
                        if (productType != null) {
                            put(ItemsListProductType.PRODUCT_TYPE, productType.toString())
                        }
                    }
                )
            }
        }

        data class Loaded(val syncStrategy: SyncStrategy) : Event() {
            override val name: String = "loaded"

            init {
                addProperties(
                    mapOf(
                        "sync_strategy" to syncStrategy.toAnalyticsValue()
                    )
                )
            }
        }

        data class LocalCatalogSyncStarted(
            val syncType: SyncType,
            val connectionType: ConnectionType
        ) : Event() {
            override val name: String = "local_catalog_sync_started"

            init {
                addProperties(
                    mapOf(
                        SyncType.SYNC_TYPE to syncType.toString(),
                        "connection_type" to when (connectionType) {
                            ConnectionType.WIFI -> "wifi"
                            ConnectionType.CELLULAR -> "cellular"
                            ConnectionType.UNKNOWN -> "unknown"
                        }
                    )
                )
            }
        }

        data class LocalCatalogSyncCompleted(
            val syncType: SyncType,
            val productsSynced: Int,
            val variationsSynced: Int,
            val totalProducts: Int,
            val totalVariations: Int,
            val syncDurationMs: Long,
            val generationDurationMs: Long? = null,
            val pollAttempts: Int? = null
        ) : Event() {
            override val name: String = "local_catalog_sync_completed"

            init {
                val properties = mutableMapOf(
                    SyncType.SYNC_TYPE to syncType.toString(),
                    "products_synced" to productsSynced.toString(),
                    "variations_synced" to variationsSynced.toString(),
                    "total_products" to totalProducts.toString(),
                    "total_variations" to totalVariations.toString(),
                    "sync_duration_ms" to syncDurationMs.toString()
                )
                generationDurationMs?.let { properties["generation_duration_ms"] = it.toString() }
                pollAttempts?.let { properties["poll_attempts"] = it.toString() }
                addProperties(properties)
            }
        }

        data class LocalCatalogSyncFailed(
            val syncType: SyncType,
            val errorContext: String,
            val errorType: SyncErrorType,
            val errorDescription: String,
            val lastGenerationState: String? = null,
            val pollAttempts: Int? = null
        ) : Event() {
            override val name: String = "local_catalog_sync_failed"

            init {
                val properties = mutableMapOf(
                    SyncType.SYNC_TYPE to syncType.toString(),
                    "error_context" to errorContext,
                    SyncErrorType.ERROR_TYPE to errorType.toString(),
                    "error_description" to errorDescription
                )
                lastGenerationState?.let { properties["last_generation_state"] = it }
                pollAttempts?.let { properties["poll_attempts"] = it.toString() }
                addProperties(properties)
            }
        }

        data class LocalCatalogSyncSkipped(
            val syncType: SyncType,
            val skipReason: SyncSkipReason
        ) : Event() {
            override val name: String = "local_catalog_sync_skipped"

            init {
                val properties = mutableMapOf(
                    SyncType.SYNC_TYPE to syncType.toString(),
                    SyncSkipReason.SKIP_REASON to skipReason.toString()
                )
                addProperties(properties)
            }
        }

        data object OrderCreationSuccess : Event() {
            override val name: String = "order_creation_success"
        }

        data class PullToRefreshTriggered(
            val source: ItemsListSource,
            val sourceType: ItemsListSourceType
        ) : Event() {
            override val name: String = "items_pull_to_refresh"

            init {
                addProperties(
                    mapOf(
                        ItemsListSource.SOURCE to source.toString(),
                        ItemsListSourceType.SOURCE_TYPE to sourceType.toString()
                    )
                )
            }
        }

        data class ReaderReadyForCardPayment(
            val waitingTimeSeconds: Long?,
            val transport: String?,
        ) : Event() {
            override val name: String = "reader_ready_for_card_payment"

            init {
                addProperties(
                    buildMap {
                        waitingTimeSeconds?.let { put("waiting_time", it.toString()) }
                        transport?.let { put("transport", it) }
                    }
                )
            }
        }

        data object RemoteTapToPayExplainerShown : Event() {
            override val name: String = "remote_ttp_explainer_shown"
        }

        data object SimpleProductExplanationDialogShown : Event() {
            override val name: String = "simple_products_explanation_dialog_shown"
        }

        data object SettingsOpened : Event() {
            override val name: String = "settings_opened"
        }

        data object SettingsClosed : Event() {
            override val name: String = "settings_closed"
        }

        data object StoreDetailsTapped : Event() {
            override val name: String = "store_details_tapped"
        }

        data object LocalCatalogTapped : Event() {
            override val name: String = "local_catalog_tapped"
        }

        data object HardwareTapped : Event() {
            override val name: String = "hardware_tapped"
        }

        data object HelpTapped : Event() {
            override val name: String = "help_tapped"
        }

        data object GetSupportTapped : Event() {
            override val name: String = "get_support_tapped"
        }

        data object EditReceiptTapped : Event() {
            override val name: String = "edit_receipt_tapped"
        }

        data object ViewDocsTapped : Event() {
            override val name: String = "view_docs_tapped"
        }

        data object EmptyCartSetUpScannerTapped : Event() {
            override val name: String = "empty_cart_set_up_scanner_tapped"
        }

        data object BarcodeScannerSetupFlowShown : Event() {
            override val name: String = "barcode_scanner_setup_flow_shown"
        }

        data class BarcodeScannerSetupScannerSelected(val scanner: String) : Event() {
            override val name: String = "barcode_scanner_setup_scanner_selected"

            init {
                addProperties(
                    mapOf(
                        "scanner" to scanner
                    )
                )
            }
        }

        data class BarcodeScannerSetupNextTapped(val scanner: String, val step: String) : Event() {
            override val name: String = "barcode_scanner_setup_next_tapped"

            init {
                addProperties(
                    mapOf(
                        "scanner" to scanner,
                        "step" to step
                    )
                )
            }
        }

        data class BarcodeScannerSetupBackTapped(val scanner: String, val step: String) : Event() {
            override val name: String = "barcode_scanner_setup_back_tapped"

            init {
                addProperties(
                    mapOf(
                        "scanner" to scanner,
                        "step" to step
                    )
                )
            }
        }

        data class BarcodeScannerSetupOpenSystemSettingsTapped(val scanner: String) : Event() {
            override val name: String = "barcode_scanner_setup_open_system_settings_tapped"

            init {
                addProperties(
                    mapOf(
                        "scanner" to scanner
                    )
                )
            }
        }

        data class BarcodeScannerSetupTestScanSuccess(val scanner: String) : Event() {
            override val name: String = "barcode_scanner_setup_test_scan_success"

            init {
                addProperties(
                    mapOf(
                        "scanner" to scanner
                    )
                )
            }
        }

        data class BarcodeScannerSetupTestScanFailed(val scanner: String, val scanValue: String) : Event() {
            override val name: String = "barcode_scanner_setup_test_scan_failed"

            init {
                addProperties(
                    mapOf(
                        "scanner" to scanner,
                        "scan_value" to scanValue
                    )
                )
            }
        }

        data class BarcodeScannerSetupTestScanTimedOut(val scanner: String) : Event() {
            override val name: String = "barcode_scanner_setup_test_scan_timed_out"

            init {
                addProperties(
                    mapOf(
                        "scanner" to scanner
                    )
                )
            }
        }

        data class BarcodeScannerSetupDismissed(val scanner: String?, val step: String?) : Event() {
            override val name: String = "barcode_scanner_setup_dismissed"

            init {
                addProperties(
                    buildMap {
                        if (scanner != null) {
                            put("scanner", scanner)
                        }
                        if (step != null) {
                            put("step", step)
                        }
                    }
                )
            }
        }

        data class BarcodeScannerSetupRetryTapped(val scanner: String) : Event() {
            override val name: String = "barcode_scanner_setup_retry_tapped"

            init {
                addProperties(
                    mapOf(
                        "scanner" to scanner
                    )
                )
            }
        }

        data class BarcodeScannerSetupScannerConnected(val scanner: String, val scannerInfo: String) : Event() {
            override val name: String = "barcode_scanner_setup_scanner_connected"

            init {
                addProperties(
                    mapOf(
                        "scanner" to scanner,
                        "scanner_info" to scannerInfo
                    )
                )
            }
        }

        data class SearchButtonTapped(
            val source: ItemsListSource,
        ) : Event() {
            override val name: String = "search_button_tapped"

            init {
                addProperties(
                    mapOf(
                        ItemsListSource.SOURCE to source.toString(),
                    )
                )
            }
        }

        class ItemsHeaderTapped(type: ItemsHeaderType) : Event() {
            override val name: String = "items_header_tapped"

            init {
                addProperties(
                    mapOf(
                        ItemsHeaderType.HEADER_TYPE to type.toString()
                    )
                )
            }
        }

        class PreSearchRecentTermTapped(source: ItemsListSource) : Event() {
            override val name: String = "pre_search_recent_term_tapped"

            init {
                addProperties(
                    mapOf(
                        ItemsListSource.SOURCE to source.toString(),
                    )
                )
            }
        }

        data class ItemsNextPageLoaded(
            val source: ItemsListSource,
            val sourceType: ItemsListSourceType
        ) : Event() {
            override val name: String = "items_next_page_loaded"

            init {
                addProperties(
                    mapOf(
                        ItemsListSource.SOURCE to source.toString(),
                        ItemsListSourceType.SOURCE_TYPE to sourceType.toString()
                    )
                )
            }
        }

        data class SearchRemoteResultsFetched(
            val totalItemsCount: Int?,
            val millisecondsSinceRequestSent: Long,
            val source: ItemsListSource,
        ) : Event() {
            override val name: String = "search_remote_results_fetched"

            init {
                addProperties(
                    mapOf(
                        "milliseconds_since_request_sent" to millisecondsSinceRequestSent.toString(),
                        ItemsListSource.SOURCE to source.toString(),
                    )
                )
                if (totalItemsCount != null) {
                    addProperties(mapOf("total_items_count" to totalItemsCount.toString()))
                }
            }
        }

        data class IneligibleUIShown(val reason: WooPosLaunchability.NonLaunchabilityReason) : Event() {
            override val name: String = "ineligible_ui_shown"

            init {
                addProperties(
                    mapOf(
                        "reason" to reason.toAnalyticsReason()
                    )
                )
            }
        }

        data class IneligibleUIRetryTapped(val reason: WooPosLaunchability.NonLaunchabilityReason) : Event() {
            override val name: String = "ineligible_ui_retry_tapped"

            init {
                addProperties(
                    mapOf(
                        "reason" to reason.toAnalyticsReason()
                    )
                )
            }
        }

        data class IneligibleUILearnMoreTapped(val reason: WooPosLaunchability.NonLaunchabilityReason) : Event() {
            override val name: String = "ineligible_ui_learn_more_tapped"

            init {
                addProperties(
                    mapOf(
                        "reason" to reason.toAnalyticsReason()
                    )
                )
            }
        }

        data object LocalCatalogDownloadingScreenShown : Event() {
            override val name: String = "local_catalog_downloading_screen_shown"
        }

        data object LocalCatalogDownloadingScreenExitPosTapped : Event() {
            override val name: String = "local_catalog_downloading_screen_exit_pos_tapped"
        }

        data class LocalCatalogStaleWarningShown(val hoursSinceLastSync: Int) : Event() {
            override val name: String = "local_catalog_stale_warning_shown"

            init {
                addProperties(
                    mapOf(
                        "hours_since_last_sync" to hoursSinceLastSync.toString()
                    )
                )
            }
        }

        data object LocalCatalogStaleWarningDismissed : Event() {
            override val name: String = "local_catalog_stale_warning_dismissed"
        }

        data object WooCommerceVersionSunsetWarningShown : Event() {
            override val name: String = "woocommerce_version_sunset_warning_shown"
        }

        data object WooCommerceVersionSunsetWarningDismissed : Event() {
            override val name: String = "woocommerce_version_sunset_warning_dismissed"
        }

        data class LocalCatalogBlockedFellBackToRemote(val wooCommerceVersion: String?) : Event() {
            override val name: String = "local_catalog_blocked_fell_back_to_remote"

            init {
                addProperties(
                    mapOf(
                        "woocommerce_version" to wooCommerceVersion.orEmpty()
                    )
                )
            }
        }

        data object CatalogBlockedContinueWithBasicSyncTapped : Event() {
            override val name: String = "catalog_blocked_continue_with_basic_sync_tapped"
        }

        data object SplashScreenErrorShown : Event() {
            override val name: String = "splash_screen_error_shown"
        }

        data object SplashScreenRetryTapped : Event() {
            override val name: String = "splash_screen_retry_tapped"
        }

        data class CheckoutOutdatedItemDetectedScreenShown(
            val reason: String,
            val syncStrategy: SyncStrategy
        ) : Event() {
            override val name: String = "checkout_outdated_item_detected_screen_shown"

            init {
                addProperties(
                    mapOf(
                        "reason" to reason,
                        "sync_strategy" to syncStrategy.toAnalyticsValue()
                    )
                )
            }
        }

        data class CheckoutOutdatedItemDetectedEditOrderTapped(
            val reason: String,
            val syncStrategy: SyncStrategy
        ) : Event() {
            override val name: String = "checkout_outdated_item_detected_edit_order_tapped"

            init {
                addProperties(
                    mapOf(
                        "reason" to reason,
                        "sync_strategy" to syncStrategy.toAnalyticsValue()
                    )
                )
            }
        }

        data class CheckoutOutdatedItemDetectedRemoveTapped(
            val reason: String,
            val syncStrategy: SyncStrategy
        ) : Event() {
            override val name: String = "checkout_outdated_item_detected_remove_tapped"

            init {
                addProperties(
                    mapOf(
                        "reason" to reason,
                        "sync_strategy" to syncStrategy.toAnalyticsValue()
                    )
                )
            }
        }

        data object RefundFlowStarted : Event() {
            override val name: String = "refund_flow_started"
        }

        data class RefundConfirmTapped(
            val refundType: String,
            val hasReason: Boolean
        ) : Event() {
            override val name: String = "refund_confirm_tapped"

            init {
                addProperties(
                    mapOf(
                        "refund_type" to refundType,
                        "has_reason" to hasReason.toString()
                    )
                )
            }
        }

        data class RefundProcessingStarted(
            val refundFlow: RefundFlow
        ) : Event() {
            override val name: String = "refund_processing_started"

            init {
                addProperties(
                    mapOf(
                        RefundFlow.REFUND_FLOW to refundFlow.value
                    )
                )
            }
        }

        data class RefundProcessingSuccess(
            val refundFlow: RefundFlow
        ) : Event() {
            override val name: String = "refund_processing_success"

            init {
                addProperties(
                    mapOf(
                        RefundFlow.REFUND_FLOW to refundFlow.value
                    )
                )
            }
        }

        /**
         * [apiErrorCode] is the store's REST error code when it returned one. It separates
         * deterministic server rejections (`woocommerce_rest_*`) from transport failures, which the
         * message cannot do — it is localized to the store and varies by wording.
         */
        data class RefundProcessingFailed(
            val refundFlow: RefundFlow,
            val apiErrorCode: String? = null,
        ) : Event() {
            override val name: String = "refund_processing_failed"

            init {
                addProperties(
                    buildMap {
                        put(RefundFlow.REFUND_FLOW, refundFlow.value)
                        apiErrorCode?.let { put(AnalyticsTracker.KEY_API_ERROR_CODE, it) }
                    }
                )
            }
        }

        /**
         * Emitted when a preview probe finds the server-calculated refund route missing and the
         * store falls back to local calculation. [wooVersion] tells us whether the version gate is
         * behaving or the store is genuinely too old. Fired where the availability cache is marked
         * unavailable, so it counts stores rather than refunds.
         */
        data class RefundServerFlowUnavailable(val wooVersion: String) : Event() {
            override val name: String = "refund_server_flow_unavailable"

            init {
                addProperties(
                    mapOf(
                        "woocommerce_version" to wooVersion
                    )
                )
            }
        }

        data class RefundFlowAborted(val refundStep: String) : Event() {
            override val name: String = "refund_flow_aborted"

            init {
                addProperties(
                    mapOf(
                        "refund_step" to refundStep
                    )
                )
            }
        }

        data class RefundSelectAllTapped(val action: String) : Event() {
            override val name: String = "refund_select_all_tapped"

            init {
                addProperties(
                    mapOf(
                        "action" to action
                    )
                )
            }
        }

        data class SearchResultsFetched(
            val millisecondsSinceRequestSent: Long,
            val resultsCount: Int,
            val source: String,
            val searchMethod: String,
        ) : Event() {
            override val name: String = "search_results_fetched"

            init {
                addProperties(
                    mapOf(
                        "milliseconds_since_request_sent" to millisecondsSinceRequestSent.toString(),
                        "results_count" to resultsCount.toString(),
                        "source" to source,
                        "search_method" to searchMethod,
                    )
                )
            }
        }

        data class FtsIndexBuilt(
            val syncType: String,
            val indexDurationMs: Long,
            val productsIndexed: Int,
        ) : Event() {
            override val name: String = "fts_index_built"

            init {
                addProperties(
                    mapOf(
                        "sync_type" to syncType,
                        "index_duration_ms" to indexDurationMs.toString(),
                        "products_indexed" to productsIndexed.toString(),
                    )
                )
            }
        }

        data class SearchResultTapped(
            val resultPosition: Int,
            val resultType: String,
        ) : Event() {
            override val name: String = "pos_search_result_tapped"

            init {
                addProperties(
                    mapOf(
                        "result_position" to resultPosition.toString(),
                        "result_type" to resultType,
                    )
                )
            }
        }
    }

    sealed class PaymentFlowTrackerEvent : WooPosAnalyticsEvent() {
        data object CardPresentCollectInteracPaymentFailed : PaymentFlowTrackerEvent() {
            override val name: String = "card_present_collect_interac_payment_failed"
        }

        data object CardPresentCollectInteracPaymentSuccess : PaymentFlowTrackerEvent() {
            override val name: String = "card_present_collect_interac_payment_success"
        }

        data object CardPresentCollectInteracRefundCancelled : PaymentFlowTrackerEvent() {
            override val name: String = "card_present_collect_interac_refund_cancelled"
        }

        data object CardPresentCollectPaymentCancelled : PaymentFlowTrackerEvent() {
            override val name: String = "card_present_collect_payment_cancelled"
        }

        data object CardPresentCollectPaymentFailed : PaymentFlowTrackerEvent() {
            override val name: String = "card_present_collect_payment_failed"
        }

        data object CardPresentCollectPaymentSuccess : PaymentFlowTrackerEvent() {
            override val name: String = "card_present_collect_payment_success"
        }

        data object CardPresentConnectionLearnMoreTapped : PaymentFlowTrackerEvent() {
            override val name: String = "card_present_connection_learn_more_tapped"
        }

        data object CardPresentOnboardingCompleted : PaymentFlowTrackerEvent() {
            override val name: String = "card_present_onboarding_completed"
        }

        data object CardPresentOnboardingCtaFailed : PaymentFlowTrackerEvent() {
            override val name: String = "card_present_onboarding_cta_failed"
        }

        data object CardPresentOnboardingCtaTapped : PaymentFlowTrackerEvent() {
            override val name: String = "card_present_onboarding_cta_tapped"
        }

        data object CardPresentOnboardingLearnMoreTapped : PaymentFlowTrackerEvent() {
            override val name: String = "card_present_onboarding_learn_more_tapped"
        }

        data object CardPresentOnboardingNotCompleted : PaymentFlowTrackerEvent() {
            override val name: String = "card_present_onboarding_not_completed"
        }

        data object CardPresentOnboardingStepSkipped : PaymentFlowTrackerEvent() {
            override val name: String = "card_present_onboarding_step_skipped"
        }

        data object CardPresentPaymentFailedContactSupportTapped : PaymentFlowTrackerEvent() {
            override val name: String = "card_present_payment_failed_contact_support_tapped"
        }

        data object CardPresentPaymentGatewaySelected : PaymentFlowTrackerEvent() {
            override val name: String = "card_present_payment_gateway_selected"
        }

        data object CardPresentSelectReaderTypeBluetoothTapped : PaymentFlowTrackerEvent() {
            override val name: String = "card_present_select_reader_type_bluetooth_tapped"
        }

        data object CardPresentSelectReaderTypeBuiltInTapped : PaymentFlowTrackerEvent() {
            override val name: String = "card_present_select_reader_type_built_in_tapped"
        }

        data object CardPresentTapToPayNotAvailable : PaymentFlowTrackerEvent() {
            override val name: String = "card_present_tap_to_pay_not_available"
        }

        data object CardPresentTapToPayPaymentFailedEnableNfcTapped : PaymentFlowTrackerEvent() {
            override val name: String = "card_present_tap_to_pay_payment_failed_enable_nfc_tapped"
        }

        data object CardReaderAutomaticDisconnect : PaymentFlowTrackerEvent() {
            override val name: String = "card_reader_automatic_disconnect"
        }

        data object CardReaderAutoConnectionStarted : PaymentFlowTrackerEvent() {
            override val name: String = "card_reader_auto_connection_started"
        }

        data object CardReaderConnectionFailed : PaymentFlowTrackerEvent() {
            override val name: String = "card_reader_connection_failed"
        }

        data object CardReaderConnectionSuccess : PaymentFlowTrackerEvent() {
            override val name: String = "card_reader_connection_success"
        }

        data object CardReaderConnectionTapped : PaymentFlowTrackerEvent() {
            override val name: String = "card_reader_connection_tapped"
        }

        data object CardReaderDisconnectTapped : PaymentFlowTrackerEvent() {
            override val name: String = "card_reader_disconnect_tapped"
        }

        data object CardReaderDiscoveryFailed : PaymentFlowTrackerEvent() {
            override val name: String = "card_reader_discovery_failed"
        }

        data object CardReaderDiscoveryReaderDiscovered : PaymentFlowTrackerEvent() {
            override val name: String = "card_reader_discovery_reader_discovered"
        }

        data object CardReaderDiscoveryTapped : PaymentFlowTrackerEvent() {
            override val name: String = "card_reader_discovery_tapped"
        }

        data object CardReaderLocationFailure : PaymentFlowTrackerEvent() {
            override val name: String = "card_reader_location_failure"
        }

        data object CardReaderLocationMissingTapped : PaymentFlowTrackerEvent() {
            override val name: String = "card_reader_location_missing_tapped"
        }

        data object CardReaderLocationPermissionPreAlertShown : PaymentFlowTrackerEvent() {
            override val name: String = "card_reader_location_permission_pre_alert_shown"
        }

        data object CardReaderLocationPermissionRequiredShown : PaymentFlowTrackerEvent() {
            override val name: String = "card_reader_location_permission_required_shown"
        }

        data object CardReaderLocationSuccess : PaymentFlowTrackerEvent() {
            override val name: String = "card_reader_location_success"
        }

        data object CardReaderSoftwareUpdateAlertInstallClicked : PaymentFlowTrackerEvent() {
            override val name: String = "card_reader_software_update_alert_install_clicked"
        }

        data object CardReaderSoftwareUpdateAlertShown : PaymentFlowTrackerEvent() {
            override val name: String = "card_reader_software_update_alert_shown"
        }

        data object CardReaderSoftwareUpdateFailed : PaymentFlowTrackerEvent() {
            override val name: String = "card_reader_software_update_failed"
        }

        data object CardReaderSoftwareUpdateStarted : PaymentFlowTrackerEvent() {
            override val name: String = "card_reader_software_update_started"
        }

        data object CardReaderSoftwareUpdateSuccess : PaymentFlowTrackerEvent() {
            override val name: String = "card_reader_software_update_success"
        }

        data object DisableCashOnDeliveryFailed : PaymentFlowTrackerEvent() {
            override val name: String = "disable_cash_on_delivery_failed"
        }

        data object DisableCashOnDeliverySuccess : PaymentFlowTrackerEvent() {
            override val name: String = "disable_cash_on_delivery_success"
        }

        data object EnableCashOnDeliveryFailed : PaymentFlowTrackerEvent() {
            override val name: String = "enable_cash_on_delivery_failed"
        }

        data object EnableCashOnDeliverySuccess : PaymentFlowTrackerEvent() {
            override val name: String = "enable_cash_on_delivery_success"
        }

        data object InPersonPaymentsLearnMoreTapped : PaymentFlowTrackerEvent() {
            override val name: String = "in_person_payments_learn_more_tapped"
        }

        data object ManageCardReadersAutomaticDisconnectBuiltInReader : PaymentFlowTrackerEvent() {
            override val name: String = "manage_card_readers_automatic_disconnect_built_in_reader"
        }

        data object PaymentsFlowOrderCollectPaymentTapped : PaymentFlowTrackerEvent() {
            override val name: String = "payments_flow_order_collect_payment_tapped"
        }

        data object PaymentsHubCashOnDeliveryToggled : PaymentFlowTrackerEvent() {
            override val name: String = "payments_hub_cash_on_delivery_toggled"
        }

        data object PaymentsHubCashOnDeliveryToggledLearnMoreTapped : PaymentFlowTrackerEvent() {
            override val name: String = "payments_hub_cash_on_delivery_toggled_learn_more_tapped"
        }

        data object PaymentsOnboardingDismissed : PaymentFlowTrackerEvent() {
            override val name: String = "payments_onboarding_dismissed"
        }

        data object PaymentsOnboardingShown : PaymentFlowTrackerEvent() {
            override val name: String = "payments_onboarding_shown"
        }

        data object ReceiptEmailFailed : PaymentFlowTrackerEvent() {
            override val name: String = "receipt_email_failed"
        }

        data object ReceiptEmailTapped : PaymentFlowTrackerEvent() {
            override val name: String = "receipt_email_tapped"
        }

        data object ReceiptPrintCanceled : PaymentFlowTrackerEvent() {
            override val name: String = "receipt_print_canceled"
        }

        data object ReceiptPrintFailed : PaymentFlowTrackerEvent() {
            override val name: String = "receipt_print_failed"
        }

        data object ReceiptPrintSuccess : PaymentFlowTrackerEvent() {
            override val name: String = "receipt_print_success"
        }

        data object ReceiptPrintTapped : PaymentFlowTrackerEvent() {
            override val name: String = "receipt_print_tapped"
        }

        data object ReceiptUrlFetchingFails : PaymentFlowTrackerEvent() {
            override val name: String = "receipt_url_fetching_fails"
        }

        data object ReceiptViewTapped : PaymentFlowTrackerEvent() {
            override val name: String = "receipt_view_tapped"
        }

        data object PaymentsFlowFailed : PaymentFlowTrackerEvent() {
            override val name: String = "payments_flow_failed"
        }

        data object PaymentsFlowCanceled : PaymentFlowTrackerEvent() {
            override val name: String = "payments_flow_canceled"
        }

        data object PaymentsFlowCollect : PaymentFlowTrackerEvent() {
            override val name: String = "payments_flow_collect"
        }

        data object PaymentsFlowCompleted : PaymentFlowTrackerEvent() {
            override val name: String = "payments_flow_completed"
        }
    }
}

internal fun IAnalyticsEvent.addProperties(additionalProperties: Map<String, String>) {
    when (this) {
        is WooPosAnalyticsEvent -> addProperties(additionalProperties)
        else -> error("Cannot add properties to non-WooPosAnalytics event")
    }
}

internal fun WooPosLaunchability.NonLaunchabilityReason.toAnalyticsReason(): String {
    return when (this) {
        WooPosLaunchability.NonLaunchabilityReason.UnsupportedWooCommerceVersion -> "wc_plugin_version"
        WooPosLaunchability.NonLaunchabilityReason.SiteSettingsUnavailable,
        WooPosLaunchability.NonLaunchabilityReason.UnknownNoPositiveCache,
        WooPosLaunchability.NonLaunchabilityReason.NoSiteSelected -> "other"
    }
}

internal fun SyncStrategy.toAnalyticsValue(): String {
    return when (this) {
        SyncStrategy.REMOTE -> "remote"
        SyncStrategy.LOCAL_CATALOG_FILE -> "local_catalog_file"
    }
}
