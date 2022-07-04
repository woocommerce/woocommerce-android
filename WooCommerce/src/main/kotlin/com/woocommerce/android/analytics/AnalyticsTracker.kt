package com.woocommerce.android.analytics

import android.annotation.SuppressLint
import android.content.Context
import androidx.preference.PreferenceManager
import com.automattic.android.tracks.TracksClient
import com.woocommerce.android.BuildConfig
import com.woocommerce.android.analytics.AnalyticsEvent.BACK_PRESSED
import com.woocommerce.android.analytics.AnalyticsEvent.VIEW_SHOWN
import com.woocommerce.android.util.WooLog
import com.woocommerce.android.util.WooLog.T
import org.json.JSONObject
import org.wordpress.android.fluxc.model.SiteModel
import java.util.UUID

class AnalyticsTracker private constructor(private val context: Context) {
    private var tracksClient: TracksClient? = TracksClient.getClient(context)
    private var username: String? = null
    private var anonymousID: String? = null

    private var site: SiteModel? = null

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

    private fun track(stat: AnalyticsEvent, properties: Map<String, *>) {
        if (tracksClient == null) {
            return
        }

        val eventName = stat.name.toLowerCase()

        val user = username ?: getAnonID() ?: generateNewAnonID()

        val userType = if (username != null) {
            TracksClient.NosaraUserType.WPCOM
        } else {
            TracksClient.NosaraUserType.ANON
        }

        val finalProperties = properties.toMutableMap()

        if (!stat.siteless) {
            site?.let {
                finalProperties[KEY_BLOG_ID] = it.siteId
                finalProperties[KEY_IS_WPCOM_STORE] = it.isWpComStore
            }
        }
        finalProperties[IS_DEBUG] = BuildConfig.DEBUG

        val propertiesJson = JSONObject(finalProperties)
        tracksClient?.track(EVENTS_PREFIX + eventName, propertiesJson, user, userType)

        if (propertiesJson.length() > 0) {
            WooLog.i(T.UTILS, "\uD83D\uDD35 Tracked: $eventName, Properties: $propertiesJson")
        } else {
            WooLog.i(T.UTILS, "\uD83D\uDD35 Tracked: $eventName")
        }
    }

    private fun flush() {
        tracksClient?.flush()
    }

    private fun refreshMetadata(newUsername: String?, site: SiteModel? = null) {
        if (tracksClient == null) {
            return
        }

        this.site = site

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

    private fun refreshSiteMetadata(site: SiteModel) {
        refreshMetadata(username, site)
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

        const val IS_DEBUG = "is_debug"
        const val KEY_ALREADY_READ = "already_read"
        const val KEY_BLOG_ID = "blog_id"
        const val KEY_CONTEXT = "context"
        const val KEY_ERROR_CONTEXT = "error_context"
        const val KEY_ERROR_DESC = "error_description"
        const val KEY_ERROR_TYPE = "error_type"
        const val KEY_FROM = "from"
        const val KEY_HAS_UNFULFILLED_ORDERS = "has_unfulfilled_orders"
        const val KEY_ID = "id"
        const val KEY_ORDER_ID = "order_id"
        const val KEY_PRODUCT_ID = "product_id"
        const val KEY_PRODUCT_COUNT = "product_count"
        const val KEY_IS_LOADING_MORE = "is_loading_more"
        const val KEY_IS_WPCOM_STORE = "is_wpcom_store"
        const val KEY_NAME = "name"
        const val KEY_NUMBER_OF_STORES = "number_of_stores"
        const val KEY_PARENT_ID = "parent_id"
        const val KEY_RANGE = "range"
        const val KEY_RESULT = "result"
        const val KEY_SELECTED_STORE_ID = "selected_store_id"
        const val KEY_STATE = "state"
        const val KEY_HAS_CHANGED_DATA = "has_changed_data"
        const val KEY_STATUS = "status"
        const val KEY_TOTAL_DURATION = "total_duration"
        const val KEY_SEARCH = "search"
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
        const val KEY_SOURCE = "source"

        const val KEY_SORT_ORDER = "order"
        const val VALUE_SORT_NAME_ASC = "name,ascending"
        const val VALUE_SORT_NAME_DESC = "name,descending"
        const val VALUE_SORT_DATE_ASC = "date,ascending"
        const val VALUE_SORT_DATE_DESC = "date,descending"

        const val VALUE_API_SUCCESS = "success"
        const val VALUE_API_FAILED = "failed"
        const val VALUE_SHIPMENT_TRACK = "track"
        const val VALUE_SHIPMENT_COPY = "copy"
        const val VALUE_ORDER = "order"
        const val VALUE_REVIEW = "review"
        const val VALUE_ORDER_DETAIL = "order_detail"
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

        const val KEY_FLOW = "flow"
        const val KEY_HAS_DIFFERENT_SHIPPING_DETAILS = "has_different_shipping_details"
        const val KEY_HAS_CUSTOMER_DETAILS = "has_customer_details"
        const val KEY_HAS_FEES = "has_fees"
        const val KEY_HAS_SHIPPING_METHOD = "has_shipping_method"
        const val VALUE_FLOW_CREATION = "creation"
        const val VALUE_FLOW_EDITING = "editing"

        const val ORDER_EDIT_CUSTOMER_NOTE = "customer_note"
        const val ORDER_EDIT_SHIPPING_ADDRESS = "shipping_address"
        const val ORDER_EDIT_BILLING_ADDRESS = "billing_address"

        const val KEY_FEEDBACK_ACTION = "action"
        const val KEY_FEEDBACK_CONTEXT = "context"
        const val VALUE_FEEDBACK_GENERAL_CONTEXT = "general"
        const val VALUE_FEEDBACK_PRODUCT_M3_CONTEXT = "products_m3"
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
        const val VALUE_PRODUCTS_VARIATIONS_FEEDBACK = "products_variations"
        const val VALUE_SHIPPING_LABELS_M4_FEEDBACK = "shipping_labels_m4"
        const val VALUE_PRODUCT_ADDONS_FEEDBACK = "product_addons"
        const val VALUE_COUPONS_FEEDBACK = "coupons"
        const val VALUE_STATE_ON = "on"
        const val VALUE_STATE_OFF = "off"

        const val VALUE_SIMPLE_PAYMENTS_FEEDBACK = "simple_payments"
        const val VALUE_SIMPLE_PAYMENTS_COLLECT_CARD = "card"
        const val VALUE_SIMPLE_PAYMENTS_COLLECT_CASH = "cash"
        const val VALUE_SIMPLE_PAYMENTS_COLLECT_LINK = "payment_link"
        const val VALUE_SIMPLE_PAYMENTS_SOURCE_AMOUNT = "amount"
        const val VALUE_SIMPLE_PAYMENTS_SOURCE_SUMMARY = "summary"
        const val VALUE_SIMPLE_PAYMENTS_SOURCE_PAYMENT_METHOD = "payment_method"

        // -- Downloadable Files
        const val KEY_DOWNLOADABLE_FILE_ACTION = "action"

        enum class DownloadableFileAction(val value: String) {
            ADDED("added"),
            UPDATED("updated"),
            DELETED("deleted")
        }

        // -- Linked Products
        const val KEY_LINKED_PRODUCTS_ACTION = "action"

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
            DONE_TAPPED("done_tapped"),
            DELETE_TAPPED("delete_tapped")
        }

        const val IMAGE_SOURCE_CAMERA = "camera"
        const val IMAGE_SOURCE_DEVICE = "device"
        const val IMAGE_SOURCE_WPMEDIA = "wpmedia"

        const val KEY_REFUND_IS_FULL = "is_full"
        const val KEY_REFUND_TYPE = "method"
        const val KEY_REFUND_METHOD = "gateway"
        const val KEY_AMOUNT = "amount"

        const val KEY_PAYMENT_METHOD = "payment_method"

        const val KEY_IS_JETPACK_CP_CONNECTED = "is_jetpack_cp_conntected"
        const val KEY_ACTIVE_JETPACK_CONNECTION_PLUGINS = "active_jetpack_connection_plugins"
        const val KEY_FETCH_SITES_DURATION = "duration"
        const val KEY_JETPACK_BENEFITS_BANNER_ACTION = "action"
        const val KEY_JETPACK_INSTALLATION_SOURCE = "source"

        private const val PREFKEY_SEND_USAGE_STATS = "wc_pref_send_usage_stats"

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
        const val KEY_COUPON_ACTION_DELETED = "tapped_delete"

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

        fun init(context: Context) {
            instance = AnalyticsTracker(context.applicationContext)
            val prefs = PreferenceManager.getDefaultSharedPreferences(context)
            sendUsageStats = prefs.getBoolean(PREFKEY_SEND_USAGE_STATS, true)
        }

        fun track(stat: AnalyticsEvent, properties: Map<String, *> = emptyMap<String, String>()) {
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
        fun track(stat: AnalyticsEvent, errorContext: String?, errorType: String?, errorDescription: String?) {
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
            stat: AnalyticsEvent,
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

        fun refreshMetadata(username: String?, site: SiteModel? = null) {
            instance?.refreshMetadata(username, site)
        }

        fun refreshSiteMetadata(site: SiteModel) {
            instance?.refreshSiteMetadata(site)
        }
    }
}
