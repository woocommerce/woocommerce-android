package com.woocommerce.android.analytics

import android.annotation.SuppressLint
import android.content.Context
import androidx.preference.PreferenceManager
import com.automattic.android.tracks.TracksClient
import com.woocommerce.android.AppPrefs
import com.woocommerce.android.BuildConfig
import com.woocommerce.android.analytics.AnalyticsEvent.BACK_PRESSED
import com.woocommerce.android.analytics.AnalyticsEvent.VIEW_SHOWN
import com.woocommerce.android.tools.SelectedSite
import com.woocommerce.android.util.GetWooCorePluginCachedVersion
import com.woocommerce.android.util.PackageUtils
import com.woocommerce.android.util.WooLog
import com.woocommerce.android.util.WooLog.T
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.channels.Channel.Factory.BUFFERED
import kotlinx.coroutines.channels.consumeEach
import kotlinx.coroutines.launch
import org.json.JSONObject
import java.util.Locale
import java.util.UUID

class AnalyticsTracker private constructor(
    private val context: Context,
    private val selectedSite: SelectedSite,
    private val appPrefs: AppPrefs,
    private val getWooVersion: GetWooCorePluginCachedVersion,
    appCoroutineScope: CoroutineScope,
) {
    private var tracksClient: TracksClient? = TracksClient.getClient(context)
    private var username: String? = null
    private var anonymousID: String? = null

    private val analyticsEventsToTrack = Channel<Pair<IAnalyticsEvent, Map<String, *>>>(capacity = BUFFERED)

    init {
        appCoroutineScope.launch {
            analyticsEventsToTrack.consumeEach { (stat, properties) ->
                doTrack(stat, properties)
            }
        }.invokeOnCompletion {
            analyticsEventsToTrack.close()
        }
    }

    private fun clearAllData() {
        clearAnonID()
        username = null

        tracksClient?.clearUserProperties()
        tracksClient?.clearQueues()
    }

    private fun clearAnonID() {
        anonymousID = null
        val preferences = PreferenceManager.getDefaultSharedPreferences(context)
        if (preferences.contains(TRACKS_ANON_ID)) {
            val editor = preferences.edit()
            editor.remove(TRACKS_ANON_ID)
            editor.apply()
        }
    }

    private fun getAnonID(): String? {
        if (anonymousID == null) {
            val preferences = PreferenceManager.getDefaultSharedPreferences(context)
            anonymousID = preferences.getString(TRACKS_ANON_ID, null)
        }
        return anonymousID
    }

    private fun generateNewAnonID(): String {
        val uuid = UUID.randomUUID().toString()
        val preferences = PreferenceManager.getDefaultSharedPreferences(context)
        val editor = preferences.edit()
        editor.putString(TRACKS_ANON_ID, uuid)
        editor.apply()

        anonymousID = uuid
        return uuid
    }

    private fun track(stat: IAnalyticsEvent, properties: Map<String, *>) {
        analyticsEventsToTrack.trySend(Pair(stat, properties))
    }

    private fun doTrack(stat: IAnalyticsEvent, properties: Map<String, *>) {
        if (tracksClient == null) {
            return
        }

        val eventName = stat.name.lowercase(Locale.getDefault())

        val user = username ?: getAnonID() ?: generateNewAnonID()

        val userType = if (username != null) {
            TracksClient.NosaraUserType.WPCOM
        } else {
            TracksClient.NosaraUserType.ANON
        }

        val propertiesJson = JSONObject(properties.buildFinalProperties(stat.siteless))
        val eventPrefix = if (stat.isPosEvent) POS_EVENTS_PREFIX else EVENTS_PREFIX
        tracksClient?.track(eventPrefix + eventName, propertiesJson, user, userType)

        if (propertiesJson.length() > 0) {
            WooLog.i(T.UTILS, "\uD83D\uDD35 Tracked: $eventName, Properties: $propertiesJson")
        } else {
            WooLog.i(T.UTILS, "\uD83D\uDD35 Tracked: $eventName")
        }
    }

    private fun Map<String, *>.buildFinalProperties(siteLess: Boolean): MutableMap<String, Any?> {
        val finalProperties = this.toMutableMap()

        val selectedSiteModel = selectedSite.getOrNull()
        if (!siteLess) {
            selectedSiteModel?.let {
                if (!finalProperties.containsKey(KEY_BLOG_ID)) finalProperties[KEY_BLOG_ID] = it.siteId
                finalProperties[KEY_IS_WPCOM_STORE] = it.isWpComStore
                finalProperties[KEY_WAS_ECOMMERCE_TRIAL] = it.wasEcommerceTrial
                finalProperties[KEY_PLAN_PRODUCT_SLUG] = it.planProductSlug
                appPrefs.getWCStoreID(it.siteId)?.let { id -> finalProperties[KEY_STORE_ID] = id }
            }
        }
        finalProperties[IS_DEBUG] = BuildConfig.DEBUG
        selectedSiteModel?.url?.let { finalProperties[KEY_SITE_URL] = it }
        getWooVersion()?.let { finalProperties[KEY_CACHED_WOO_VERSION] = it }

        return finalProperties
    }

    private fun flush() {
        tracksClient?.flush()
    }

    private fun refreshMetadata(newUsername: String?) {
        if (tracksClient == null) {
            return
        }

        if (!newUsername.isNullOrEmpty()) {
            username = newUsername
            if (getAnonID() != null) {
                tracksClient?.trackAliasUser(username, getAnonID(), TracksClient.NosaraUserType.WPCOM)
                clearAnonID()
            }
        } else {
            username = null
            if (getAnonID() == null) {
                generateNewAnonID()
            }
        }
    }

    private fun storeUsagePref() {
        val prefs = PreferenceManager.getDefaultSharedPreferences(context)
        prefs.edit().putBoolean(PREFKEY_SEND_USAGE_STATS, sendUsageStats).apply()
    }

    companion object {
        // Guaranteed to hold a reference to the application context, which is safe
        @SuppressLint("StaticFieldLeak")
        private var instance: AnalyticsTracker? = null

        private const val TRACKS_ANON_ID = "nosara_tracks_anon_id"
        private const val EVENTS_PREFIX = "woocommerceandroid_"
        private const val POS_EVENTS_PREFIX = "woocommerceandroid_pos_"
        private const val KEY_SITE_URL = "site_url"

        const val IS_DEBUG = "is_debug"
        const val KEY_ALREADY_READ = "already_read"
        const val KEY_BLOG_ID = "blog_id"
        const val KEY_STORE_ID = "store_id"
        const val KEY_CONTEXT = "context"
        const val KEY_ERROR = "error"
        const val KEY_ERROR_CONTEXT = "error_context"
        const val KEY_ERROR_DESC = "error_description"
        const val KEY_ERROR_TYPE = "error_type"
        const val KEY_ERROR_CODE = "error_code"
        const val KEY_NETWORK_STATUS_CODE = "network_status_code"
        const val KEY_FROM = "from"
        const val KEY_HAS_UNFULFILLED_ORDERS = "has_unfulfilled_orders"
        const val KEY_ID = "id"
        const val KEY_ITEM_STOCK_MANAGED = "item_stock_managed"
        const val KEY_ORDER_ID = "order_id"
        const val KEY_PRODUCT_ID = "product_id"
        const val KEY_PRODUCT_COUNT = "product_count"
        const val KEY_HAS_LINKED_PRODUCTS = "has_linked_products"
        const val KEY_HAS_MIN_MAX_QUANTITY_RULES = "has_min_max_quantity_rules"
        const val KEY_IS_LOADING_MORE = "is_loading_more"
        const val KEY_IS_WPCOM_STORE = "is_wpcom_store"
        const val KEY_NAME = "name"
        const val KEY_NUMBER_OF_STORES = "number_of_stores"
        const val KEY_NUMBER_OF_NON_WOO_SITES = "number_of_non_woo_sites"
        const val KEY_PARENT_ID = "parent_id"
        const val KEY_RANGE = "range"
        const val KEY_RESULT = "result"
        const val KEY_SELECTED_STORE_ID = "selected_store_id"
        const val KEY_STATE = "state"
        const val KEY_HAS_CHANGED_DATA = "has_changed_data"
        const val KEY_STATUS = "status"
        const val KEY_PRODUCT = "product"
        const val KEY_CUSTOMER = "customer"
        const val KEY_TOTAL_DURATION = "total_duration"
        const val KEY_TOTAL_COMPLETED_ORDERS = "total_completed_orders"
        const val KEY_SEARCH = "search"
        const val KEY_SEARCH_FILTER = "filter"
        const val KEY_SEARCH_TYPE = "search_filter"
        const val VALUE_SEARCH_TYPE_ALL = "all"
        const val VALUE_SEARCH_TYPE_SKU = "sku"
        const val KEY_TO = "to"
        const val KEY_TYPE = "type"
        const val KEY_CARRIER = "carrier"
        const val KEY_OPTION = "option"
        const val KEY_URL = "url"
        const val KEY_HAS_CONNECTED_STORES = "has_connected_stores"
        const val KEY_LAST_KNOWN_VERSION_CODE = "last_known_version_code"
        const val KEY_REVIEW_ID = "review_id"
        const val KEY_NOTE_ID = "note_id"
        const val KEY_IMAGE_SOURCE = "source"
        const val KEY_FILTERS = "filters"
        const val KEY_FULFILL_ORDER = "fulfill_order"
        const val KEY_STEP = "step"
        const val KEY_ADDONS = "addons"
        const val KEY_SOFTWARE_UPDATE_TYPE = "software_update_type"
        const val KEY_SUBJECT = "subject"
        const val KEY_DATE_RANGE = "date_range"
        const val KEY_DATE = "date"
        const val KEY_GRANULARITY = "granularity"
        const val KEY_SOURCE = "source"
        const val KEY_CUSTOM_FIELDS_COUNT = "custom_fields_count"
        const val KEY_CUSTOM_FIELDS_SIZE = "custom_fields_size"
        const val KEY_WAITING_TIME = "waiting_time"
        const val KEY_IS_NON_ATOMIC = "is_non_atomic"
        const val KEY_CAUSE = "cause"
        const val KEY_SCENARIO = "scenario"
        const val KEY_REASON = "reason"
        const val KEY_TAP = "tap"
        const val KEY_FAILURE = "failure"
        const val KEY_SCANNING_SOURCE = "source"
        const val KEY_SCANNING_BARCODE_FORMAT = "barcode_format"
        const val KEY_PRODUCT_ADDED_VIA = "added_via"
        const val KEY_SCANNING_FAILURE_REASON = "reason"
        const val KEY_CATEGORY = "category"
        const val KEY_START_PAYMENT_FLOW = "start_payment_flow"
        const val KEY_HORIZONTAL_SIZE_CLASS = "horizontal_size_class"
        const val KEY_SUCCESS = "success"
        const val KEY_TIME_TAKEN = "time_taken"
        const val KEY_IS_EDITING = "is_editing"

        const val KEY_SORT_ORDER = "order"
        const val VALUE_DEVICE_TYPE_REGULAR = "regular"
        const val VALUE_DEVICE_TYPE_COMPACT = "compact"
        const val VALUE_SORT_NAME_ASC = "name,ascending"
        const val VALUE_SORT_NAME_DESC = "name,descending"
        const val VALUE_SORT_DATE_ASC = "date,ascending"
        const val VALUE_SORT_DATE_DESC = "date,descending"

        const val VALUE_API_SUCCESS = "success"
        const val VALUE_API_FAILED = "failed"
        const val VALUE_SHIPMENT_TRACK = "track"
        const val VALUE_SHIPMENT_COPY = "copy"
        const val VALUE_REVIEW = "review"
        const val VALUE_STARTED = "started"
        const val VALUE_PURCHASE_INITIATED = "purchase_initiated"
        const val VALUE_ORIGIN_ADDRESS_STARTED = "origin_address_started"
        const val VALUE_DESTINATION_ADDRESS_STARTED = "destination_address_started"
        const val VALUE_PACKAGES_STARTED = "packages_started"
        const val VALUE_CARRIER_RATES_STARTED = "carrier_rates_started"
        const val VALUE_CUSTOMS_STARTED = "customs_started"
        const val VALUE_PAYMENT_METHOD_STARTED = "payment_method_started"
        const val VALUE_ORIGIN_ADDRESS_COMPLETE = "origin_address_complete"
        const val VALUE_DESTINATION_ADDRESS_COMPLETE = "destination_address_complete"
        const val VALUE_PACKAGES_SELECTED = "packages_selected"
        const val VALUE_CARRIER_RATES_SELECTED = "carrier_rates_selected"
        const val VALUE_CUSTOMS_COMPLETE = "customs_complete"
        const val VALUE_PAYMENT_METHOD_SELECTED = "payment_method_selected"
        const val VALUE_PURCHASE_FAILED = "purchase_failed"
        const val VALUE_PURCHASE_SUCCEEDED = "purchase_succeeded"
        const val VALUE_PURCHASE_READY = "purchase_ready"
        const val VALUE_SEARCH_ALL = "all"
        const val VALUE_SEARCH_SKU = "sku"
        const val VALUE_SUBMIT = "submit"
        const val VALUE_DISMISS = "dismiss"
        const val VALUE_WP_COM = "wp_com"
        const val VALUE_NO_WP_COM = "no_wp_com"
        const val VALUE_PREVIOUS_PERIOD = "previous_period"

        const val KEY_FLOW = "flow"
        const val KEY_HAS_DIFFERENT_SHIPPING_DETAILS = "has_different_shipping_details"
        const val KEY_HAS_CUSTOMER_DETAILS = "has_customer_details"
        const val KEY_HAS_FEES = "has_fees"
        const val KEY_HAS_SHIPPING_METHOD = "has_shipping_method"
        const val KEY_CUSTOM_AMOUNTS_COUNT = "custom_amounts_Count"
        const val KEY_CUSTOM_AMOUNT_TAX_STATUS = "tax_status"
        const val KEY_EXPANDED = "expanded"
        const val KEY_SHIPPING_METHOD = "shipping_method"

        const val VALUE_CUSTOM_AMOUNT_TAX_STATUS_TAXABLE = "taxable"
        const val VALUE_CUSTOM_AMOUNT_TAX_STATUS_NONE = "none"
        const val VALUE_FLOW_CREATION = "creation"
        const val VALUE_FLOW_EDITING = "editing"
        const val VALUE_FLOW_LIST = "list"

        const val AUTO_TAX_RATE_ENABLED = "auto_tax_rate_enabled"

        const val ORDER_EDIT_CUSTOMER_NOTE = "customer_note"
        const val ORDER_EDIT_SHIPPING_ADDRESS = "shipping_address"
        const val ORDER_EDIT_BILLING_ADDRESS = "billing_address"

        const val KEY_ORDER_DISCOUNT_TYPE = "type"
        const val VALUE_ORDER_DISCOUNT_TYPE_FIXED = "fixed_amount"
        const val VALUE_ORDER_DISCOUNT_TYPE_PERCENTAGE = "percentage"

        const val KEY_HAS_MULTIPLE_SHIPPING_LINES = "has_multiple_shipping_lines"
        const val KEY_HAS_MULTIPLE_FEE_LINES = "has_multiple_fee_lines"

        const val JITM_ID = "jitm_id"
        const val JITM_FEATURE_CLASS = "feature_class"

        const val KEY_TIME_ELAPSED_SINCE_ADD_NEW_ORDER_IN_MILLIS = "milliseconds_since_order_add_new"
        const val KEY_TIME_ELAPSED_SINCE_CARD_COLLECT_PAYMENT_IN_MILLIS = "milliseconds_since_card_collect_payment_flow"

        const val KEY_COUPONS_COUNT = "coupons_count"
        const val KEY_USE_GIFT_CARD = "use_gift_card"
        const val KEY_IS_GIFT_CARD_REMOVED = "removed"
        const val KEY_SHIPPING_LINES_COUNT = "shipping_lines_count"

        const val KEY_WAS_ECOMMERCE_TRIAL = "was_ecommerce_trial"
        const val KEY_PLAN_PRODUCT_SLUG = "plan_product_slug"
        const val KEY_CACHED_WOO_VERSION = "cached_woo_core_version"

        const val KEY_PERIOD = "period"
        const val KEY_REPORT = "report"
        const val KEY_COMPARE = "compare"

        enum class OrderNoteType(val value: String) {
            CUSTOMER("customer"),
            PRIVATE("private"),
        }

        const val KEY_FEEDBACK_ACTION = "action"
        const val KEY_FEEDBACK_CONTEXT = "context"
        const val VALUE_FEEDBACK_GENERAL_CONTEXT = "general"
        const val VALUE_FEEDBACK_PRODUCT_M3_CONTEXT = "products_m3"
        const val VALUE_FEEDBACK_STORE_SETUP_CONTEXT = "store_setup"
        const val VALUE_FEEDBACK_SHOWN = "shown"
        const val VALUE_FEEDBACK_LIKED = "liked"
        const val VALUE_FEEDBACK_NOT_LIKED = "didnt_like"
        const val VALUE_FEEDBACK_LATER = "later"
        const val VALUE_FEEDBACK_DECLINED = "declined"
        const val VALUE_FEEDBACK_RATED = "rated"
        const val VALUE_FEEDBACK_COMPLETED = "completed"
        const val VALUE_FEEDBACK_OPENED = "opened"
        const val VALUE_FEEDBACK_CANCELED = "canceled"
        const val VALUE_FEEDBACK_DISMISSED = "dismissed"
        const val VALUE_FEEDBACK_GIVEN = "gave_feedback"
        const val VALUE_SHIPPING_LABELS_M4_FEEDBACK = "shipping_labels_m4"
        const val VALUE_PRODUCT_ADDONS_FEEDBACK = "product_addons"
        const val VALUE_COUPONS_FEEDBACK = "coupons"
        const val VALUE_ANALYTICS_HUB_FEEDBACK = "analytics_hub"
        const val VALUE_ORDER_SHIPPING_LINES_FEEDBACK = "order_shipping_lines"
        const val VALUE_STATE_ON = "on"
        const val VALUE_STATE_OFF = "off"

        const val VALUE_SIMPLE_PAYMENTS_FLOW = "simple_payment"
        const val VALUE_SIMPLE_PAYMENTS_FEEDBACK = "simple_payments"
        const val VALUE_TAP_TO_PAY_FEEDBACK = "tap_to_pay"
        const val VALUE_SIMPLE_PAYMENTS_COLLECT_CARD = "card"
        const val VALUE_SIMPLE_PAYMENTS_COLLECT_CASH = "cash"
        const val VALUE_SIMPLE_PAYMENTS_COLLECT_LINK = "payment_link"
        const val VALUE_SIMPLE_PAYMENTS_SOURCE_AMOUNT = "amount"
        const val VALUE_SIMPLE_PAYMENTS_SOURCE_SUMMARY = "summary"
        const val VALUE_SIMPLE_PAYMENTS_SOURCE_PAYMENT_METHOD = "payment_method"

        const val VALUE_TAP_TO_PAY_SOURCE_TRY_PAYMENT_PROMPT = "tap_to_pay_try_a_payment_prompt"
        const val VALUE_CARD_READER_TYPE_EXTERNAL = "external"
        const val VALUE_CARD_READER_TYPE_BUILT_IN = "built_in"

        const val VALUE_ORDER_PAYMENTS_FLOW = "order_payment"
        const val VALUE_ORDER_CREATION_PAYMENTS_FLOW = "creation"
        const val VALUE_SCAN_TO_PAY_PAYMENT_FLOW = "scan_to_pay"
        const val VALUE_TTP_TRY_PAYMENT_FLOW = "tap_to_pay_try_a_payment"
        const val VALUE_WOO_POS_PAYMENTS_FLOW = "woo_pos"

        const val KEY_JITM = "jitm"
        const val KEY_JITM_COUNT = "count"

        const val KEY_STORE_TIMEZONE = "store_timezone"
        const val KEY_LOCAL_TIMEZONE = "local_timezone"

        // -- Downloadable Files
        const val KEY_DOWNLOADABLE_FILE_ACTION = "action"

        // -- Connectivity Tool
        const val VALUE_INTERNET = "internet"
        const val VALUE_SITE = "site"
        const val VALUE_JETPACK_TUNNEL = "jetpack_tunnel"

        enum class DownloadableFileAction(val value: String) {
            ADDED("added"),
            UPDATED("updated"),
            DELETED("deleted")
        }

        // -- Linked Products
        const val KEY_LINKED_PRODUCTS_ACTION = "action"

        // -- Product Selector
        const val KEY_PRODUCT_SELECTOR_SOURCE = "source"
        const val KEY_PRODUCT_SELECTOR_FILTER_STATUS = "is_filter_active"

        enum class LinkedProductsAction(val value: String) {
            SHOWN("shown"),
            DONE("done")
        }

        // -- Connected Products
        const val KEY_CONNECTED_PRODUCTS_LIST_CONTEXT = "context"
        const val KEY_CONNECTED_PRODUCTS_LIST_ACTION = "action"

        enum class ConnectedProductsListContext(val value: String) {
            GROUPED_PRODUCTS("grouped_products"),
            UPSELLS("upsells"),
            CROSS_SELLS("cross_sells")
        }

        enum class ConnectedProductsListAction(val value: String) {
            ADD_TAPPED("add_tapped"),
            ADDED("added"),
            DELETE_TAPPED("delete_tapped")
        }

        const val IMAGE_SOURCE_CAMERA = "camera"
        const val IMAGE_SOURCE_DEVICE = "device"
        const val IMAGE_SOURCE_WPMEDIA = "wpmedia"

        const val KEY_REFUND_IS_FULL = "is_full"
        const val KEY_REFUND_TYPE = "method"
        const val KEY_REFUND_METHOD = "gateway"
        const val KEY_AMOUNT = "amount"
        const val KEY_AMOUNT_NORMALIZED = "amount_normalized"
        const val KEY_CURRENCY = "currency"

        const val KEY_PAYMENT_METHOD = "payment_method"
        const val KEY_PAYMENT_GATEWAY = "payment_gateway"
        const val KEY_PAYMENT_CARD_READER_TYPE = "card_reader_type"

        const val KEY_IS_JETPACK_CP_CONNECTED = "is_jetpack_cp_conntected"
        const val KEY_ACTIVE_JETPACK_CONNECTION_PLUGINS = "active_jetpack_connection_plugins"
        const val KEY_FETCH_SITES_DURATION = "duration"
        const val KEY_JETPACK_BENEFITS_BANNER_ACTION = "action"
        const val KEY_JETPACK_INSTALLATION_SOURCE = "source"
        const val KEY_JETPACK_INSTALLATION_STEP = "jetpack_install_step"

        const val PREFKEY_SEND_USAGE_STATS = "wc_pref_send_usage_stats"

        // -- Product details
        const val VALUE_SHARE_BUTTON_SOURCE_PRODUCT_FORM = "product_form"
        const val VALUE_SHARE_BUTTON_SOURCE_MORE_MENU = "more_menu"

        // -- Product Variations
        const val KEY_VARIATIONS_COUNT = "variations_count"

        // -- Feature Announcement / What's New
        const val KEY_ANNOUNCEMENT_VIEW_SOURCE = "source"
        const val VALUE_ANNOUNCEMENT_SOURCE_UPGRADE = "app_upgrade"
        const val VALUE_ANNOUNCEMENT_SOURCE_SETTINGS = "app_settings"

        // -- More Menu (aka Hub Menu) option values
        const val VALUE_MORE_MENU_VIEW_STORE = "view_store"
        const val VALUE_MORE_MENU_ADMIN_MENU = "admin_menu"
        const val VALUE_MORE_MENU_REVIEWS = "reviews"
        const val VALUE_MORE_MENU_INBOX = "inbox"
        const val VALUE_MORE_MENU_COUPONS = "coupons"
        const val VALUE_MORE_MENU_PAYMENTS = "payments"
        const val VALUE_MORE_MENU_UPGRADES = "upgrades"

        const val VALUE_MORE_MENU_PAYMENTS_BADGE_VISIBLE = "badge_visible"

        // -- Inbox note actions
        const val KEY_INBOX_NOTE_ACTION = "action"
        const val VALUE_INBOX_NOTE_ACTION_OPEN = "open"
        const val VALUE_INBOX_NOTE_ACTION_DISMISS = "dismiss"
        const val VALUE_INBOX_NOTE_ACTION_DISMISS_ALL = "dismiss_all"

        // -- Coupons
        const val KEY_COUPON_ACTION = "action"
        const val KEY_COUPON_ACTION_LOADED = "loaded"
        const val KEY_COUPON_ACTION_COPIED = "copied_code"
        const val KEY_COUPON_ACTION_SHARED = "shared_code"
        const val KEY_COUPON_ACTION_EDITED = "tapped_edit"
        const val KEY_COUPON_ACTION_DELETED = "tapped_delete"
        const val KEY_COUPON_DISCOUNT_TYPE_UPDATED = "discount_type_updated"
        const val KEY_COUPON_CODE_UPDATED = "coupon_code_updated"
        const val KEY_COUPON_AMOUNT_UPDATED = "amount_updated"
        const val KEY_COUPON_DESCRIPTION_UPDATED = "description_updated"
        const val KEY_COUPON_ALLOWED_PRODUCTS_OR_CATEGORIES_UPDATED = "allowed_products_or_categories_updated"
        const val KEY_COUPON_EXPIRY_DATE_UPDATED = "expiry_date_updated"
        const val KEY_COUPON_USAGE_RESTRICTIONS_UPDATED = "usage_restrictions_updated"
        const val KEY_COUPON_DISCOUNT_TYPE = "discount_type"
        const val VALUE_COUPON_DISCOUNT_TYPE_PERCENTAGE = "percent"
        const val VALUE_COUPON_DISCOUNT_TYPE_FIXED_CART = "fixed_cart"
        const val VALUE_COUPON_DISCOUNT_TYPE_FIXED_PRODUCT = "fixed_product"
        const val VALUE_COUPON_DISCOUNT_TYPE_CUSTOM = "custom"
        const val KEY_HAS_EXPIRY_DATE = "has_expiry_date"
        const val KEY_INCLUDES_FREE_SHIPPING = "includes_free_shipping"
        const val KEY_HAS_DESCRIPTION = "has_description"
        const val KEY_HAS_PRODUCT_OR_CATEGORY_RESTRICTIONS = "has_product_or_category_restrictions"
        const val KEY_HAS_USAGE_RESTRICTIONS = "has_usage_restrictions"

        // -- Onboarding
        const val VALUE_LOGIN_ONBOARDING_IS_FINAL_PAGE = "is_final_page"

        // -- Jetpack Installation
        const val VALUE_JETPACK_INSTALLATION_SOURCE_WEB = "web"

        // -- Jetpack Setup
        const val KEY_JETPACK_SETUP_IS_ALREADY_CONNECTED = "is_already_connected"
        const val KEY_JETPACK_SETUP_REQUIRES_CONNECTION_ONLY = "requires_connection_only"
        const val VALUE_JETPACK_SETUP_STEP_EMAIL_ADDRESS = "email_address"
        const val VALUE_JETPACK_SETUP_STEP_PASSWORD = "password"
        const val VALUE_JETPACK_SETUP_STEP_MAGIC_LINK = "magic_link"
        const val VALUE_JETPACK_INSTALLATION_STEP_BENEFITS = "benefits"
        const val VALUE_JETPACK_SETUP_STEP_VERIFICATION_CODE = "verification_code"
        const val VALUE_JETPACK_SETUP_TAP_GO_TO_STORE = "go_to_store"
        const val VALUE_JETPACK_SETUP_TAP_SUPPORT = "support"
        const val VALUE_JETPACK_SETUP_TAP_TRY_AGAIN = "try_again"

        // -- Login with WordPress.com account flow
        const val VALUE_LOGIN_WITH_WORDPRESS_COM = "wordpress_com"

        // -- Upsell banner
        const val KEY_BANNER_SOURCE = "source"
        const val KEY_BANNER_CAMPAIGN_NAME = "campaign_name"
        const val KEY_BANNER_REMIND_LATER = "remind_later"
        const val KEY_BANNER_LINKED_PRODUCTS_PROMO = "linked_products_promo"

        const val SOURCE_PRODUCT_DETAIL = "product_detail"

        // -- Cash on Delivery
        const val KEY_IS_ENABLED = "is_enabled"
        const val KEY_CASH_ON_DELIVERY_SOURCE = "source"

        // -- Help Center
        const val KEY_SOURCE_FLOW = "source_flow"
        const val KEY_SOURCE_STEP = "source_step"
        const val KEY_HELP_CONTENT_URL = "help_content_url"

        // Widgets
        const val KEY_WIDGETS = "widgets"

        // -- App links
        const val KEY_PATH = "path"

        const val VALUE_OTHER = "other"
        const val VALUE_STEP_WEB_CHECKOUT = "web_checkout"

        // -- Products bulk update
        const val KEY_PROPERTY = "property"
        const val VALUE_PRICE = "price"
        const val VALUE_STATUS = "status"
        const val VALUE_STOCK_STATUS = "stock_status"
        const val KEY_SELECTED_PRODUCTS_COUNT = "selected_products_count"

        // -- IPP Learn More Link
        const val IPP_LEARN_MORE_SOURCE = "source"

        // -- Domain change
        const val VALUE_STEP_DASHBOARD = "dashboard"
        const val VALUE_STEP_PICKER = "picker"
        const val VALUE_STEP_CONTACT_INFO = "contact_info"
        const val VALUE_STEP_PURCHASE_SUCCESS = "purchase_success"
        const val KEY_USE_DOMAIN_CREDIT = "use_domain_credit"

        // -- Free Trial
        const val VALUE_BANNER = "banner"
        const val VALUE_UPGRADES_SCREEN = "upgrades_screen"

        // -- Store Onboarding
        const val ONBOARDING_TASK_KEY = "task"
        const val KEY_HIDE_ONBOARDING_SOURCE = "source"
        const val KEY_ONBOARDING_PENDING_TASKS = "pending_tasks"
        const val KEY_HIDE_ONBOARDING_LIST_VALUE = "hide"
        const val VALUE_STORE_DETAILS = "store_details"
        const val VALUE_PRODUCTS = "products"
        const val VALUE_ADD_DOMAIN = "add_domain"
        const val VALUE_LAUNCH_SITE = "launch_site"
        const val VALUE_PAYMENTS = "payments"
        const val VALUE_WOO_PAYMENTS = "woocommerce-payments"
        const val VALUE_LOCAL_NAME_STORE = "store_name"

        // -- Product Selector
        const val VALUE_PRODUCT_SELECTOR = "product_selector"
        const val VALUE_VARIATION_SELECTOR = "variation_selector"

        // -- Product sharing with AI
        const val KEY_IS_RETRY = "is_retry"
        const val KEY_WITH_MESSAGE = "with_message"
        const val VALUE_PRODUCT_SHARING = "product_sharing"

        // -- AI product description
        const val VALUE_AZTEC_EDITOR = "aztec_editor"
        const val VALUE_PRODUCT_FORM = "product_form"
        const val VALUE_PRODUCT_DESCRIPTION = "product_description"
        const val KEY_IS_USEFUL = "is_useful"

        // -- AI Language detection
        const val KEY_DETECTED_LANGUAGE = "language"

        // -- AI thank-you note
        const val VALUE_ORDER_THANK_YOU_NOTE = "order_thank_you_note"

        // -- Blaze
        const val KEY_BLAZE_SOURCE = "source"
        const val KEY_BLAZE_DURATION = "duration"
        const val KEY_BLAZE_TOTAL_BUDGET = "total_budget"
        const val KEY_BLAZE_IS_AI_CONTENT = "is_ai_suggested_ad_content"
        const val KEY_BLAZE_ERROR = "blaze_creation_error"

        const val PRODUCT_TYPES = "product_types"
        const val HAS_ADDONS = "has_addons"
        const val KEY_HAS_BUNDLE_CONFIGURATION = "has_bundle_configuration"
        const val VALUE_PRODUCT_CARD = "product_card"
        const val KEY_CHANGED_FIELD = "changed_field"
        const val VALUE_CHANGED_FIELD_QUANTITY = "quantity"
        const val VALUE_CHANGED_FIELD_VARIATION = "variation"
        const val VALUE_CHANGED_FIELD_OPTIONAL = "optional"

        // -- AI product name
        const val KEY_HAS_INPUT_NAME = "has_input_name"
        const val VALUE_PRODUCT_CREATION = "product_creation"
        const val VALUE_PRODUCT_CREATION_AI = "product_creation_ai"

        // -- AI product creation
        const val KEY_TONE = "tone"
        const val KEY_IS_FIRST_ATTEMPT = "is_first_attempt"

        // -- AI product from package photo
        const val KEY_SCANNED_TEXT_COUNT = "scanned_text_count"
        const val KEY_SELECTED_TEXT_COUNT = "selected_text_count"
        const val VALUE_PRODUCT_CREATION_FROM_PACKAGE_PHOTO = "product_creation_from_package_photo"

        const val KEY_IS_AI_CONTENT = "is_ai_content"

        // -- Product subscriptions
        const val KEY_IS_ELIGIBLE_FOR_SUBSCRIPTIONS = "is_eligible_for_subscriptions"

        // -- Scan To Update Inventory
        const val SCAN_TO_UPDATE_INVENTORY = "scan_to_update_inventory"

        // Theme picker
        const val KEY_THEME_PICKER_SOURCE = "source"
        const val VALUE_THEME_PICKER_SOURCE_SETTINGS = "settings"
        const val KEY_THEME_PICKER_THEME = "theme"
        const val KEY_THEME_PICKER_LAYOUT_PREVIEW = "layout"
        const val KEY_THEME_PICKER_PAGE_PREVIEW = "page"

        // Analytics Hub Settings
        const val KEY_ENABLED_CARDS = "enabled_cards"
        const val KEY_DISABLED_CARDS = "disabled_cards"

        // Dynamic Dashboard
        const val KEY_NEW_CARD_AVAILABLE = "new_card_available"
        const val KEY_CARDS = "cards"
        const val KEY_SORTED_CARDS = "sorted_cards"

        var sendUsageStats: Boolean = true
            set(value) {
                if (value != field) {
                    field = value
                    instance?.storeUsagePref()
                    if (!field) {
                        instance?.clearAllData()
                    }
                }
            }

        fun init(
            context: Context,
            selectedSite: SelectedSite,
            appPrefs: AppPrefs,
            getWooVersion: GetWooCorePluginCachedVersion,
            appCoroutineScope: CoroutineScope,
        ) {
            instance = AnalyticsTracker(
                context.applicationContext,
                selectedSite,
                appPrefs,
                getWooVersion,
                appCoroutineScope
            )
            val prefs = PreferenceManager.getDefaultSharedPreferences(context)
            sendUsageStats = prefs.getBoolean(PREFKEY_SEND_USAGE_STATS, true)
        }

        fun track(stat: IAnalyticsEvent, properties: Map<String, *> = emptyMap<String, String>()) {
            if (instance == null && BuildConfig.DEBUG && !PackageUtils.isTesting()) {
                error("event $stat was tracked before AnalyticsTracker was initialized.")
            }
            if (sendUsageStats) {
                instance?.track(stat, properties)
            }
        }

        /**
         * A convenience method for logging an error event with some additional meta data.
         * @param stat The stat to track.
         * @param errorContext A string providing additional context (if any) about the error.
         * @param errorType The type of error.
         * @param errorDescription The error text or other description.
         */
        fun track(stat: IAnalyticsEvent, errorContext: String?, errorType: String?, errorDescription: String?) {
            track(stat, mapOf(), errorContext, errorType, errorDescription)
        }

        /**
         * A convenience method for logging an error event with some additional meta data.
         * @param stat The stat to track.
         * @param properties Map of additional properties
         * @param errorContext A string providing additional context (if any) about the error.
         * @param errorType The type of error.
         * @param errorDescription The error text or other description.
         */
        fun track(
            stat: IAnalyticsEvent,
            properties: Map<String, Any>,
            errorContext: String?,
            errorType: String?,
            errorDescription: String?
        ) {
            val mutableProperties = HashMap<String, Any>(properties)
            errorContext?.let {
                mutableProperties[KEY_ERROR_CONTEXT] = it
            }
            errorType?.let {
                mutableProperties[KEY_ERROR_TYPE] = it
            }
            errorDescription?.let {
                mutableProperties[KEY_ERROR_DESC] = it
            }
            track(stat, mutableProperties)
        }

        /**
         * A convenience method for tracking views shown during a session.
         * @param view The view to be tracked
         */
        fun trackViewShown(view: Any) {
            val name = if (view is String) {
                view
            } else {
                view::class.java.simpleName
            }
            track(VIEW_SHOWN, mapOf(KEY_NAME to name))
        }

        /**
         * A convenience method for tracking when a user clicks the "up" or "back" buttons.
         * @param view The active view when event was fired
         */
        fun trackBackPressed(view: Any) {
            track(BACK_PRESSED, mapOf(KEY_CONTEXT to view::class.java.simpleName))
        }

        fun flush() {
            instance?.flush()
        }

        fun clearAllData() {
            instance?.clearAllData()
        }

        fun refreshMetadata(username: String?) {
            instance?.refreshMetadata(username)
        }
    }
}
