package com.woocommerce.android.analytics

import android.annotation.SuppressLint
import android.content.Context
import androidx.preference.PreferenceManager
import com.automattic.android.tracks.TracksClient
import com.woocommerce.android.analytics.AnalyticsTracker.Stat.BACK_PRESSED
import com.woocommerce.android.analytics.AnalyticsTracker.Stat.VIEW_SHOWN
import com.woocommerce.android.util.WooLog
import com.woocommerce.android.util.WooLog.T
import org.json.JSONObject
import org.wordpress.android.fluxc.model.SiteModel
import java.util.HashMap
import java.util.UUID

class AnalyticsTracker private constructor(private val context: Context) {
    // region Track Event Enums
    enum class Stat(val siteless: Boolean = false) {
        // -- General
        APPLICATION_OPENED(siteless = true),
        APPLICATION_CLOSED(siteless = true),
        APPLICATION_INSTALLED(siteless = true),
        APPLICATION_UPGRADED(siteless = true),
        APPLICATION_VERSION_CHECK_FAILED(siteless = true),
        BACK_PRESSED(siteless = true),
        VIEW_SHOWN(siteless = true),

        // -- Login
        SIGNED_IN(siteless = true),
        ACCOUNT_LOGOUT(siteless = true),
        LOGIN_ACCESSED(siteless = true),
        LOGIN_MAGIC_LINK_EXITED(siteless = true),
        LOGIN_MAGIC_LINK_FAILED(siteless = true),
        LOGIN_MAGIC_LINK_OPENED(siteless = true),
        LOGIN_MAGIC_LINK_REQUESTED(siteless = true),
        LOGIN_MAGIC_LINK_SUCCEEDED(siteless = true),
        LOGIN_FAILED(siteless = true),
        LOGIN_INSERTED_INVALID_URL(siteless = true),
        LOGIN_AUTOFILL_CREDENTIALS_FILLED(siteless = true),
        LOGIN_AUTOFILL_CREDENTIALS_UPDATED(siteless = true),
        LOGIN_EMAIL_FORM_VIEWED(siteless = true),
        LOGIN_BY_EMAIL_HELP_FINDING_CONNECTED_EMAIL_LINK_TAPPED(siteless = true),
        LOGIN_MAGIC_LINK_OPEN_EMAIL_CLIENT_VIEWED(siteless = true),
        LOGIN_MAGIC_LINK_OPEN_EMAIL_CLIENT_CLICKED(siteless = true),
        LOGIN_MAGIC_LINK_REQUEST_FORM_VIEWED(siteless = true),
        LOGIN_PASSWORD_FORM_VIEWED(siteless = true),
        LOGIN_URL_FORM_VIEWED(siteless = true),
        LOGIN_URL_HELP_SCREEN_VIEWED(siteless = true),
        LOGIN_USERNAME_PASSWORD_FORM_VIEWED(siteless = true),
        LOGIN_TWO_FACTOR_FORM_VIEWED(siteless = true),
        LOGIN_FORGOT_PASSWORD_CLICKED(siteless = true),
        LOGIN_SOCIAL_BUTTON_CLICK(siteless = true),
        LOGIN_SOCIAL_BUTTON_FAILURE(siteless = true),
        LOGIN_SOCIAL_CONNECT_SUCCESS(siteless = true),
        LOGIN_SOCIAL_CONNECT_FAILURE(siteless = true),
        LOGIN_SOCIAL_SUCCESS(siteless = true),
        LOGIN_SOCIAL_FAILURE(siteless = true),
        LOGIN_SOCIAL_2FA_NEEDED(siteless = true),
        LOGIN_SOCIAL_ACCOUNTS_NEED_CONNECTING(siteless = true),
        LOGIN_SOCIAL_ERROR_UNKNOWN_USER(siteless = true),
        LOGIN_WPCOM_BACKGROUND_SERVICE_UPDATE(siteless = true),
        SIGNUP_EMAIL_BUTTON_TAPPED(siteless = true),
        SIGNUP_GOOGLE_BUTTON_TAPPED(siteless = true),
        SIGNUP_TERMS_OF_SERVICE_TAPPED(siteless = true),
        SIGNUP_CANCELED(siteless = true),
        SIGNUP_EMAIL_TO_LOGIN(siteless = true),
        SIGNUP_MAGIC_LINK_FAILED(siteless = true),
        SIGNUP_MAGIC_LINK_OPENED(siteless = true),
        SIGNUP_MAGIC_LINK_OPEN_EMAIL_CLIENT_CLICKED(siteless = true),
        SIGNUP_MAGIC_LINK_SENT(siteless = true),
        SIGNUP_MAGIC_LINK_SUCCEEDED(siteless = true),
        SIGNUP_SOCIAL_ACCOUNTS_NEED_CONNECTING(siteless = true),
        SIGNUP_SOCIAL_BUTTON_FAILURE(siteless = true),
        SIGNUP_SOCIAL_TO_LOGIN(siteless = true),
        ADDED_SELF_HOSTED_SITE(siteless = true),
        CREATED_ACCOUNT(siteless = true),
        LOGIN_PROLOGUE_JETPACK_LOGIN_BUTTON_TAPPED(siteless = true),
        LOGIN_PROLOGUE_JETPACK_CONFIGURATION_INSTRUCTIONS_LINK_TAPPED(siteless = true),
        LOGIN_JETPACK_REQUIRED_SCREEN_VIEWED(siteless = true),
        LOGIN_JETPACK_REQUIRED_VIEW_INSTRUCTIONS_BUTTON_TAPPED(siteless = true),
        LOGIN_JETPACK_REQUIRED_WHAT_IS_JETPACK_LINK_TAPPED(siteless = true),
        LOGIN_JETPACK_REQUIRED_MENU_HELP_TAPPED(siteless = true),
        LOGIN_JETPACK_REQUIRED_SIGN_IN_LINK_TAPPED(siteless = true),
        LOGIN_WHAT_IS_JETPACK_HELP_SCREEN_VIEWED(siteless = true),
        LOGIN_WHAT_IS_JETPACK_HELP_SCREEN_LEARN_MORE_BUTTON_TAPPED(siteless = true),
        LOGIN_WHAT_IS_JETPACK_HELP_SCREEN_OK_BUTTON_TAPPED(siteless = true),
        LOGIN_CONNECTED_SITE_INFO_REQUESTED(siteless = true),
        LOGIN_CONNECTED_SITE_INFO_FAILED(siteless = true),
        LOGIN_CONNECTED_SITE_INFO_SUCCEEDED(siteless = true),
        LOGIN_FIND_CONNECTED_EMAIL_HELP_SCREEN_VIEWED(siteless = true),
        LOGIN_FIND_CONNECTED_EMAIL_HELP_SCREEN_NEED_MORE_HELP_LINK_TAPPED(siteless = true),
        LOGIN_FIND_CONNECTED_EMAIL_HELP_SCREEN_OK_BUTTON_TAPPED(siteless = true),
        LOGIN_NO_JETPACK_SCREEN_VIEWED(siteless = true),
        LOGIN_NO_JETPACK_VIEW_INSTRUCTIONS_BUTTON_TAPPED(siteless = true),
        LOGIN_NO_JETPACK_LOGOUT_LINK_TAPPED(siteless = true),
        LOGIN_NO_JETPACK_TRY_AGAIN_TAPPED(siteless = true),
        LOGIN_NO_JETPACK_MENU_HELP_TAPPED(siteless = true),
        LOGIN_DISCOVERY_ERROR_SCREEN_VIEWED(siteless = true),
        LOGIN_DISCOVERY_ERROR_TROUBLESHOOT_BUTTON_TAPPED(siteless = true),
        LOGIN_DISCOVERY_ERROR_TRY_AGAIN_TAPPED(siteless = true),
        LOGIN_DISCOVERY_ERROR_SIGN_IN_WORDPRESS_BUTTON_TAPPED(siteless = true),
        LOGIN_DISCOVERY_ERROR_MENU_HELP_TAPPED(siteless = true),
        LOGIN_MAGIC_LINK_INTERCEPT_SCREEN_VIEWED(siteless = true),
        LOGIN_MAGIC_LINK_INTERCEPT_RETRY_TAPPED(siteless = true),
        LOGIN_MAGIC_LINK_UPDATE_TOKEN_FAILED(siteless = true),
        LOGIN_MAGIC_LINK_FETCH_ACCOUNT_FAILED(siteless = true),
        LOGIN_MAGIC_LINK_FETCH_ACCOUNT_SETTINGS_FAILED(siteless = true),
        LOGIN_MAGIC_LINK_FETCH_SITES_FAILED(siteless = true),
        LOGIN_MAGIC_LINK_FETCH_ACCOUNT_SUCCESS(siteless = true),
        LOGIN_MAGIC_LINK_FETCH_ACCOUNT_SETTINGS_SUCCESS(siteless = true),
        LOGIN_MAGIC_LINK_FETCH_SITES_SUCCESS(siteless = true),

        // -- Site Picker
        SITE_PICKER_STORES_SHOWN(siteless = true),
        SITE_PICKER_CONTINUE_TAPPED(siteless = true),
        SITE_PICKER_HELP_BUTTON_TAPPED(siteless = true),
        SITE_PICKER_AUTO_LOGIN_SUBMITTED(siteless = true),
        SITE_PICKER_AUTO_LOGIN_ERROR_NOT_CONNECTED_TO_USER(siteless = true),
        SITE_PICKER_AUTO_LOGIN_ERROR_NOT_WOO_STORE(siteless = true),
        SITE_PICKER_AUTO_LOGIN_ERROR_NOT_CONNECTED_JETPACK(siteless = true),
        SITE_PICKER_TRY_ANOTHER_ACCOUNT_BUTTON_TAPPED(siteless = true),
        SITE_PICKER_TRY_ANOTHER_STORE_BUTTON_TAPPED(siteless = true),
        SITE_PICKER_VIEW_CONNECTED_STORES_BUTTON_TAPPED(siteless = true),
        SITE_PICKER_HELP_FINDING_CONNECTED_EMAIL_LINK_TAPPED(siteless = true),
        SITE_PICKER_NOT_WOO_STORE_REFRESH_APP_LINK_TAPPED(siteless = true),
        SITE_PICKER_NOT_CONNECTED_JETPACK_REFRESH_APP_LINK_TAPPED(siteless = true),

        // -- Dashboard
        DASHBOARD_PULLED_TO_REFRESH,
        DASHBOARD_SHARE_YOUR_STORE_BUTTON_TAPPED,
        DASHBOARD_UNFULFILLED_ORDERS_BUTTON_TAPPED,
        DASHBOARD_MAIN_STATS_DATE,
        DASHBOARD_MAIN_STATS_LOADED,
        DASHBOARD_TOP_PERFORMERS_DATE,
        DASHBOARD_TOP_PERFORMERS_LOADED,
        DASHBOARD_NEW_STATS_REVERTED_BANNER_DISMISS_TAPPED,
        DASHBOARD_NEW_STATS_REVERTED_BANNER_LEARN_MORE_TAPPED,
        DASHBOARD_NEW_STATS_AVAILABILITY_BANNER_CANCEL_TAPPED,
        DASHBOARD_NEW_STATS_AVAILABILITY_BANNER_TRY_TAPPED,

        // -- Orders List
        ORDERS_LIST_FILTER,
        ORDERS_LIST_LOADED,
        ORDERS_LIST_SHARE_YOUR_STORE_BUTTON_TAPPED,
        ORDERS_LIST_PULLED_TO_REFRESH,
        ORDERS_LIST_MENU_FILTER_TAPPED,
        ORDERS_LIST_MENU_SEARCH_TAPPED,

        // -- Order filter by status dialog
        FILTER_ORDERS_BY_STATUS_DIALOG_APPLY_FILTER_BUTTON_TAPPED,
        FILTER_ORDERS_BY_STATUS_DIALOG_OPTION_SELECTED,

        // -- Order Detail
        ORDER_OPEN,
        ORDER_NOTES_LOADED,
        ORDER_CONTACT_ACTION,
        ORDER_CONTACT_ACTION_FAILED,
        ORDER_STATUS_CHANGE,
        ORDER_STATUS_CHANGE_FAILED,
        ORDER_STATUS_CHANGE_SUCCESS,
        ORDER_STATUS_CHANGE_UNDO,
        ORDER_DETAIL_PULLED_TO_REFRESH,
        ORDER_DETAIL_ADD_NOTE_BUTTON_TAPPED,
        ORDER_DETAIL_CUSTOMER_INFO_SHOW_BILLING_TAPPED,
        ORDER_DETAIL_CUSTOMER_INFO_HIDE_BILLING_TAPPED,
        ORDER_DETAIL_CUSTOMER_INFO_EMAIL_MENU_EMAIL_TAPPED,
        ORDER_DETAIL_CUSTOMER_INFO_PHONE_MENU_PHONE_TAPPED,
        ORDER_DETAIL_CUSTOMER_INFO_PHONE_MENU_SMS_TAPPED,
        ORDER_DETAIL_FULFILL_ORDER_BUTTON_TAPPED,
        ORDER_DETAIL_ORDER_STATUS_EDIT_BUTTON_TAPPED,
        ORDER_DETAIL_PRODUCT_TAPPED,
        ORDER_DETAIL_PRODUCT_DETAIL_BUTTON_TAPPED,
        ORDER_DETAIL_TRACK_PACKAGE_BUTTON_TAPPED,
        ORDER_TRACKING_LOADED,
        ORDER_DETAIL_TRACKING_DELETE_BUTTON_TAPPED,
        ORDER_DETAIL_TRACKING_ADD_TRACKING_BUTTON_TAPPED,
        ORDER_DETAIL_ISSUE_REFUND_BUTTON_TAPPED,
        ORDER_DETAIL_VIEW_REFUND_DETAILS_BUTTON_TAPPED,

        // -- Refunds
        CREATE_ORDER_REFUND_NEXT_BUTTON_TAPPED,
        CREATE_ORDER_REFUND_TAB_CHANGED,
        CREATE_ORDER_REFUND_SELECT_ALL_ITEMS_BUTTON_TAPPED,
        CREATE_ORDER_REFUND_ITEM_QUANTITY_DIALOG_OPENED,
        CREATE_ORDER_REFUND_PRODUCT_AMOUNT_DIALOG_OPENED,
        CREATE_ORDER_REFUND_SUMMARY_REFUND_BUTTON_TAPPED,
        CREATE_ORDER_REFUND_SUMMARY_UNDO_BUTTON_TAPPED,
        REFUND_CREATE,
        REFUND_CREATE_SUCCESS,
        REFUND_CREATE_FAILED,

        // -- Order Notes
        ADD_ORDER_NOTE_ADD_BUTTON_TAPPED,
        ADD_ORDER_NOTE_EMAIL_NOTE_TO_CUSTOMER_TOGGLED,
        ORDER_NOTE_ADD,
        ORDER_NOTE_ADD_FAILED,
        ORDER_NOTE_ADD_SUCCESS,

        // -- Order Fulfillment
        SNACK_ORDER_MARKED_COMPLETE_UNDO_BUTTON_TAPPED,
        ORDER_FULFILLMENT_MARK_ORDER_COMPLETE_BUTTON_TAPPED,
        ORDER_FULFILLMENT_TRACKING_ADD_TRACKING_BUTTON_TAPPED,
        ORDER_FULFILLMENT_TRACKING_DELETE_BUTTON_TAPPED,

        // -- Order Shipment Tracking
        ORDER_SHIPMENT_TRACKING_CARRIER_SELECTED,
        ORDER_TRACKING_ADD,
        ORDER_TRACKING_ADD_FAILED,
        ORDER_TRACKING_ADD_SUCCESS,
        ORDER_SHIPMENT_TRACKING_ADD_BUTTON_TAPPED,
        ORDER_SHIPMENT_TRACKING_CUSTOM_PROVIDER_SELECTED,
        ORDER_TRACKING_DELETE,
        ORDER_TRACKING_DELETE_SUCCESS,
        ORDER_TRACKING_DELETE_FAILED,
        ORDER_TRACKING_PROVIDERS_LOADED,

        // -- Top-level navigation
        MAIN_MENU_SETTINGS_TAPPED,
        MAIN_MENU_CONTACT_SUPPORT_TAPPED,
        MAIN_TAB_DASHBOARD_SELECTED,
        MAIN_TAB_DASHBOARD_RESELECTED,
        MAIN_TAB_ORDERS_SELECTED,
        MAIN_TAB_ORDERS_RESELECTED,
        MAIN_TAB_PRODUCTS_SELECTED,
        MAIN_TAB_PRODUCTS_RESELECTED,
        MAIN_TAB_NOTIFICATIONS_SELECTED,
        MAIN_TAB_NOTIFICATIONS_RESELECTED,

        // -- Settings
        SETTING_CHANGE,
        SETTING_CHANGE_FAILED,
        SETTING_CHANGE_SUCCESS,
        SETTINGS_SELECTED_SITE_TAPPED,
        SETTINGS_LOGOUT_BUTTON_TAPPED,
        SETTINGS_LOGOUT_CONFIRMATION_DIALOG_RESULT,
        SETTINGS_BETA_FEATURES_BUTTON_TAPPED,
        SETTINGS_PRIVACY_SETTINGS_BUTTON_TAPPED,
        SETTINGS_FEATURE_REQUEST_BUTTON_TAPPED,
        SETTINGS_ABOUT_WOOCOMMERCE_LINK_TAPPED,
        SETTINGS_ABOUT_OPEN_SOURCE_LICENSES_LINK_TAPPED,
        SETTINGS_NOTIFICATIONS_OPEN_CHANNEL_SETTINGS_BUTTON_TAPPED,
        SETTINGS_WE_ARE_HIRING_BUTTON_TAPPED,
        SETTINGS_BETA_FEATURES_NEW_STATS_UI_TOGGLED,
        SETTINGS_BETA_FEATURES_PRODUCTS_TOGGLED,
        SETTINGS_IMAGE_OPTIMIZATION_TOGGLED,
        PRIVACY_SETTINGS_COLLECT_INFO_TOGGLED,
        PRIVACY_SETTINGS_PRIVACY_POLICY_LINK_TAPPED,
        PRIVACY_SETTINGS_SHARE_INFO_LINK_TAPPED,
        PRIVACY_SETTINGS_THIRD_PARTY_TRACKING_INFO_LINK_TAPPED,
        PRIVACY_SETTINGS_CRASH_REPORTING_TOGGLED,

        // -- Product list
        PRODUCT_LIST_LOADED,
        PRODUCT_LIST_LOAD_ERROR,
        PRODUCT_LIST_PRODUCT_TAPPED,
        PRODUCT_LIST_PULLED_TO_REFRESH,
        PRODUCT_LIST_SEARCHED,
        PRODUCT_LIST_MENU_SEARCH_TAPPED,
        PRODUCT_LIST_VIEW_FILTER_OPTIONS_TAPPED,
        PRODUCT_LIST_VIEW_SORTING_OPTIONS_TAPPED,
        PRODUCT_LIST_SORTING_OPTION_SELECTED,

        // -- Product detail
        PRODUCT_DETAIL_LOADED,
        PRODUCT_DETAIL_IMAGE_TAPPED,
        PRODUCT_DETAIL_SHARE_BUTTON_TAPPED,
        PRODUCT_DETAIL_UPDATE_BUTTON_TAPPED,
        PRODUCT_DETAIL_VIEW_EXTERNAL_TAPPED,
        PRODUCT_DETAIL_VIEW_AFFILIATE_TAPPED,
        PRODUCT_DETAIL_VIEW_PRODUCT_VARIANTS_TAPPED,
        PRODUCT_DETAIL_VIEW_PRODUCT_DESCRIPTION_TAPPED,
        PRODUCT_DETAIL_VIEW_PRICE_SETTINGS_TAPPED,
        PRODUCT_DETAIL_VIEW_INVENTORY_SETTINGS_TAPPED,
        PRODUCT_DETAIL_VIEW_SHIPPING_SETTINGS_TAPPED,
        PRODUCT_DETAIL_VIEW_SHORT_DESCRIPTION_TAPPED,
        PRODUCT_DETAIL_VIEW_CATEGORIES_TAPPED,
        PRODUCT_PRICE_SETTINGS_DONE_BUTTON_TAPPED,
        PRODUCT_INVENTORY_SETTINGS_DONE_BUTTON_TAPPED,
        PRODUCT_SHIPPING_SETTINGS_DONE_BUTTON_TAPPED,
        PRODUCT_IMAGE_SETTINGS_DONE_BUTTON_TAPPED,
        PRODUCT_CATEGORY_SETTINGS_DONE_BUTTON_TAPPED,
        PRODUCT_DETAIL_UPDATE_SUCCESS,
        PRODUCT_DETAIL_UPDATE_ERROR,

        // -- Product Categories
        PRODUCT_CATEGORIES_LOADED,
        PRODUCT_CATEGORIES_LOAD_FAILED,
        PRODUCT_CATEGORIES_PULLED_TO_REFRESH,

        // -- Product settings
        PRODUCT_SETTINGS_DONE_BUTTON_TAPPED,
        PRODUCT_DETAIL_ADD_IMAGE_TAPPED,
        PRODUCT_IMAGE_SETTINGS_ADD_IMAGES_BUTTON_TAPPED,
        PRODUCT_IMAGE_SETTINGS_ADD_IMAGES_SOURCE_TAPPED,
        PRODUCT_IMAGE_SETTINGS_DELETE_IMAGE_BUTTON_TAPPED,
        PRODUCT_SETTINGS_STATUS_TAPPED,
        PRODUCT_SETTINGS_CATALOG_VISIBILITY_TAPPED,
        PRODUCT_SETTINGS_SLUG_TAPPED,
        PRODUCT_SETTINGS_PURCHASE_NOTE_TAPPED,
        PRODUCT_SETTINGS_VISIBILITY_TAPPED,
        PRODUCT_SETTINGS_MENU_ORDER_TAPPED,

        // -- Product filters
        PRODUCT_FILTER_LIST_SHOW_PRODUCTS_BUTTON_TAPPED,
        PRODUCT_FILTER_LIST_CLEAR_MENU_BUTTON_TAPPED,

        // -- Aztec editor
        AZTEC_EDITOR_DONE_BUTTON_TAPPED,

        // -- Product variants
        PRODUCT_VARIANTS_PULLED_TO_REFRESH,
        PRODUCT_VARIANTS_LOADED,
        PRODUCT_VARIANTS_LOAD_ERROR,

        // -- Product images
        PRODUCT_IMAGE_ADDED,
        PRODUCT_IMAGE_REMOVED,

        // -- Help & Support
        SUPPORT_HELP_CENTER_VIEWED(siteless = true),
        SUPPORT_IDENTITY_SET(siteless = true),
        SUPPORT_IDENTITY_FORM_VIEWED(siteless = true),
        SUPPORT_APPLICATION_LOG_VIEWED(siteless = true),
        SUPPORT_TICKETS_VIEWED(siteless = true),
        SUPPORT_FAQ_VIEWED(siteless = true),

        // -- Push notifications
        PUSH_NOTIFICATION_RECEIVED,
        PUSH_NOTIFICATION_TAPPED,

        // -- Notifications List
        NOTIFICATION_OPEN,
        NOTIFICATIONS_LOADED,
        NOTIFICATIONS_LOAD_FAILED,

        // -- Product Review List
        REVIEWS_LOADED,
        REVIEWS_LOAD_FAILED,
        REVIEWS_PRODUCTS_LOADED,
        REVIEWS_PRODUCTS_LOAD_FAILED,
        REVIEWS_MARK_ALL_READ,
        REVIEWS_MARK_ALL_READ_SUCCESS,
        REVIEWS_MARK_ALL_READ_FAILED,
        REVIEWS_LIST_PULLED_TO_REFRESH,
        REVIEWS_LIST_MENU_MARK_READ_BUTTON_TAPPED,
        REVIEWS_LIST_SHARE_YOUR_STORE_BUTTON_TAPPED,

        // -- Product Review Detail
        REVIEW_LOADED,
        REVIEW_LOAD_FAILED,
        REVIEW_PRODUCT_LOADED,
        REVIEW_PRODUCT_LOAD_FAILED,
        REVIEW_MARK_READ,
        REVIEW_MARK_READ_SUCCESS,
        REVIEW_MARK_READ_FAILED,
        REVIEW_ACTION,
        REVIEW_ACTION_FAILED,
        REVIEW_ACTION_SUCCESS,
        REVIEW_ACTION_UNDO,
        SNACK_REVIEW_ACTION_APPLIED_UNDO_BUTTON_TAPPED,
        REVIEW_DETAIL_APPROVE_BUTTON_TAPPED,
        REVIEW_DETAIL_OPEN_EXTERNAL_BUTTON_TAPPED,
        REVIEW_DETAIL_SPAM_BUTTON_TAPPED,
        REVIEW_DETAIL_TRASH_BUTTON_TAPPED,

        // -- Errors
        JETPACK_TUNNEL_TIMEOUT,

        // -- Order status changes
        SET_ORDER_STATUS_DIALOG_APPLY_BUTTON_TAPPED,

        // -- Application permissions
        APP_PERMISSION_GRANTED,
        APP_PERMISSION_DENIED,

        // -- Other
        UNFULFILLED_ORDERS_LOADED,
        TOP_EARNER_PRODUCT_TAPPED
    }
    // endregion

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

    private fun track(stat: Stat, properties: Map<String, *>) {
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
        const val KEY_SEARCH = "search"
        const val KEY_TO = "to"
        const val KEY_TYPE = "type"
        const val KEY_CARRIER = "carrier"
        const val KEY_OPTION = "option"
        const val KEY_SOURCE = "source"
        const val KEY_URL = "url"
        const val KEY_HAS_CONNECTED_STORES = "has_connected_stores"
        const val KEY_LAST_KNOWN_VERSION_CODE = "last_known_version_code"
        const val KEY_REVIEW_ID = "review_id"
        const val KEY_NOTE_ID = "note_id"
        const val KEY_IMAGE_SOURCE = "source"
        const val KEY_FILTERS = "filters"

        const val KEY_SORT_ORDER = "order"
        const val VALUE_SORT_NAME_ASC = "name,ascending"
        const val VALUE_SORT_NAME_DESC = "name,descending"
        const val VALUE_SORT_DATE_ASC = "date,ascending"
        const val VALUE_SORT_DATE_DESC = "date,descending"

        const val VALUE_ORDER = "order"
        const val VALUE_REVIEW = "review"
        const val VALUE_ORDER_DETAIL = "order_detail"
        const val VALUE_ORDER_FULFILL = "order_fulfill"
        const val IMAGE_SOURCE_CAMERA = "camera"
        const val IMAGE_SOURCE_DEVICE = "device"
        const val IMAGE_SOURCE_WPMEDIA = "wpmedia"

        const val KEY_REFUND_IS_FULL = "is_full"
        const val KEY_REFUND_TYPE = "method"
        const val KEY_REFUND_METHOD = "gateway"
        const val KEY_REFUND_AMOUNT = "amount"

        private const val PREFKEY_SEND_USAGE_STATS = "wc_pref_send_usage_stats"

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

        fun track(stat: Stat, properties: Map<String, *> = emptyMap<String, String>()) {
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
        fun track(stat: Stat, errorContext: String?, errorType: String?, errorDescription: String?) {
            val props = HashMap<String, String>()
            errorContext?.let {
                props[KEY_ERROR_CONTEXT] = it
            }
            errorType?.let {
                props[KEY_ERROR_TYPE] = it
            }
            errorDescription?.let {
                props[KEY_ERROR_DESC] = it
            }
            track(stat, props)
        }

        /**
         * A convenience method for tracking views shown during a session.
         * @param view The view to be tracked
         */
        fun trackViewShown(view: Any) {
            track(VIEW_SHOWN, mapOf(KEY_NAME to view::class.java.simpleName))
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
