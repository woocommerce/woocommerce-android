package com.woocommerce.android.analytics

import android.annotation.SuppressLint
import android.content.Context
import android.preference.PreferenceManager
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
        LOGIN_PROLOGUE_JETPACK_BUTTON_TAPPED(siteless = true),
        LOGIN_PROLOGUE_JETPACK_CONFIGURATION_INSTRUCTIONS_LINK_TAPPED(siteless = true),
        LOGIN_EPILOGUE_STORES_SHOWN(siteless = true),
        LOGIN_EPILOGUE_STORE_PICKER_CONTINUE_TAPPED(siteless = true),

        // -- Dashboard
        DASHBOARD_PULLED_TO_REFRESH,
        DASHBOARD_SHARE_YOUR_STORE_BUTTON_TAPPED,
        DASHBOARD_UNFULFILLED_ORDERS_BUTTON_TAPPED,
        DASHBOARD_MAIN_STATS_DATE,
        DASHBOARD_MAIN_STATS_LOADED,
        DASHBOARD_TOP_PERFORMERS_DATE,
        DASHBOARD_TOP_PERFORMERS_LOADED,
        DASHBOARD_UNFULFILLED_ORDERS_LOADED,

        // -- Orders List
        ORDERS_LIST_FILTER,
        ORDERS_LIST_LOADED,
        ORDERS_LIST_SHARE_YOUR_STORE_BUTTON_TAPPED,
        ORDERS_LIST_PULLED_TO_REFRESH,
        ORDERS_LIST_MENU_FILTER_TAPPED,

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
        ORDER_DETAIL_ADD_NOTE_BUTTON_TAPPED,
        ORDER_DETAIL_CUSTOMER_INFO_SHOW_BILLING_TAPPED,
        ORDER_DETAIL_CUSTOMER_INFO_HIDE_BILLING_TAPPED,
        ORDER_DETAIL_CUSTOMER_INFO_EMAIL_MENU_EMAIL_TAPPED,
        ORDER_DETAIL_CUSTOMER_INFO_PHONE_MENU_PHONE_TAPPED,
        ORDER_DETAIL_CUSTOMER_INFO_PHONE_MENU_SMS_TAPPED,
        ORDER_DETAIL_FULFILL_ORDER_BUTTON_TAPPED,
        ORDER_DETAIL_PRODUCT_DETAIL_BUTTON_TAPPED,

        // -- Order Notes
        ADD_ORDER_NOTE_ADD_BUTTON_TAPPED,
        ADD_ORDER_NOTE_EMAIL_NOTE_TO_CUSTOMER_TOGGLED,
        ORDER_NOTE_ADD,
        ORDER_NOTE_ADD_FAILED,
        ORDER_NOTE_ADD_SUCCESS,

        // -- Order Fulfillment
        SNACK_ORDER_MARKED_COMPLETE_UNDO_BUTTON_TAPPED,
        ORDER_FULFILLMENT_MARK_ORDER_COMPLETE_BUTTON_TAPPED,

        // -- Top-level navigation
        MAIN_MENU_SETTINGS_TAPPED,
        MAIN_MENU_CONTACT_SUPPORT_TAPPED,
        MAIN_TAB_DASHBOARD_SELECTED,
        MAIN_TAB_DASHBOARD_RESELECTED,
        MAIN_TAB_ORDERS_SELECTED,
        MAIN_TAB_ORDERS_RESELECTED,
        MAIN_TAB_NOTIFICATIONS_SELECTED,
        MAIN_TAB_NOTIFICATIONS_RESELECTED,

        // -- Settings
        SETTING_CHANGE,
        SETTING_CHANGE_FAILED,
        SETTING_CHANGE_SUCCESS,
        SETTINGS_LOGOUT_BUTTON_TAPPED,
        SETTINGS_LOGOUT_CONFIRMATION_DIALOG_RESULT,
        SETTINGS_PRIVACY_SETTINGS_BUTTON_TAPPED,
        SETTINGS_ABOUT_WOOCOMMERCE_LINK_TAPPED,
        SETTINGS_ABOUT_OPEN_SOURCE_LICENSES_LINK_TAPPED,
        PRIVACY_SETTINGS_COLLECT_INFO_TOGGLED,
        PRIVACY_SETTINGS_PRIVACY_POLICY_LINK_TAPPED,
        PRIVACY_SETTINGS_SHARE_INFO_LINK_TAPPED,
        PRIVACY_SETTINGS_THIRD_PARTY_TRACKING_INFO_LINK_TAPPED,
        PRIVACY_SETTINGS_CRASH_REPORTING_TOGGLED,

        // -- Help & Support
        SUPPORT_HELP_CENTER_VIEWED,
        SUPPORT_IDENTITY_SET,
        SUPPORT_IDENTITY_FORM_VIEWED,
        SUPPORT_APPLICATION_LOG_VIEWED,
        SUPPORT_TICKETS_VIEWED,
        SUPPORT_FAQ_VIEWED,
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

        const val KEY_BLOG_ID = "blog_id"
        const val KEY_CONTEXT = "context"
        const val KEY_ERROR_CONTEXT = "error_context"
        const val KEY_ERROR_DESC = "error_description"
        const val KEY_ERROR_TYPE = "error_type"
        const val KEY_FROM = "from"
        const val KEY_HAS_UNFULFILLED_ORDERS = "has_unfulfilled_orders"
        const val KEY_ID = "id"
        const val KEY_IS_LOADING_MORE = "is_loading_more"
        const val KEY_IS_WPCOM_STORE = "is_wpcom_store"
        const val KEY_NAME = "name"
        const val KEY_NUMBER_OF_STORES = "number_of_stores"
        const val KEY_PARENT_ID = "parent_id"
        const val KEY_RANGE = "range"
        const val KEY_RESULT = "result"
        const val KEY_SELECTED_STORE_ID = "selected_store_id"
        const val KEY_STATE = "state"
        const val KEY_STATUS = "status"
        const val KEY_TO = "to"
        const val KEY_TYPE = "type"

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
        fun track(stat: Stat, errorContext: String, errorType: String, errorDescription: String?) {
            val props = HashMap<String, String>()
            props[KEY_ERROR_CONTEXT] = errorContext
            props[KEY_ERROR_TYPE] = errorType
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
            AnalyticsTracker.track(VIEW_SHOWN, mapOf(KEY_NAME to view::class.java.simpleName))
        }

        /**
         * A convenience method for tracking when a user clicks the "up" or "back" buttons.
         * @param view The active view when event was fired
         */
        fun trackBackPressed(view: Any) {
            AnalyticsTracker.track(BACK_PRESSED, mapOf(KEY_CONTEXT to view::class.java.simpleName))
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
