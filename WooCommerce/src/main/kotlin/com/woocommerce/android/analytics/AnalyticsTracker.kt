package com.woocommerce.android.analytics

import android.annotation.SuppressLint
import android.content.Context
import java.util.HashMap
import com.automattic.android.tracks.TracksClient
import android.preference.PreferenceManager
import com.woocommerce.android.util.WooLog
import com.woocommerce.android.util.WooLog.T
import java.util.UUID
import org.json.JSONObject
import org.wordpress.android.fluxc.model.SiteModel

class AnalyticsTracker private constructor(private val context: Context) {
    enum class Stat {
        APPLICATION_OPENED,
        APPLICATION_CLOSED,
        APPLICATION_INSTALLED,
        APPLICATION_UPGRADED,

        // -- Login
        SIGNED_IN,
        ACCOUNT_LOGOUT,
        LOGIN_ACCESSED,
        LOGIN_MAGIC_LINK_EXITED,
        LOGIN_MAGIC_LINK_FAILED,
        LOGIN_MAGIC_LINK_OPENED,
        LOGIN_MAGIC_LINK_REQUESTED,
        LOGIN_MAGIC_LINK_SUCCEEDED,
        LOGIN_FAILED,
        LOGIN_INSERTED_INVALID_URL,
        LOGIN_AUTOFILL_CREDENTIALS_FILLED,
        LOGIN_AUTOFILL_CREDENTIALS_UPDATED,
        LOGIN_EMAIL_FORM_VIEWED,
        LOGIN_MAGIC_LINK_OPEN_EMAIL_CLIENT_VIEWED,
        LOGIN_MAGIC_LINK_OPEN_EMAIL_CLIENT_CLICKED,
        LOGIN_MAGIC_LINK_REQUEST_FORM_VIEWED,
        LOGIN_PASSWORD_FORM_VIEWED,
        LOGIN_URL_FORM_VIEWED,
        LOGIN_URL_HELP_SCREEN_VIEWED,
        LOGIN_USERNAME_PASSWORD_FORM_VIEWED,
        LOGIN_TWO_FACTOR_FORM_VIEWED,
        LOGIN_FORGOT_PASSWORD_CLICKED,
        LOGIN_SOCIAL_BUTTON_CLICK,
        LOGIN_SOCIAL_BUTTON_FAILURE,
        LOGIN_SOCIAL_CONNECT_SUCCESS,
        LOGIN_SOCIAL_CONNECT_FAILURE,
        LOGIN_SOCIAL_SUCCESS,
        LOGIN_SOCIAL_FAILURE,
        LOGIN_SOCIAL_2FA_NEEDED,
        LOGIN_SOCIAL_ACCOUNTS_NEED_CONNECTING,
        LOGIN_SOCIAL_ERROR_UNKNOWN_USER,
        LOGIN_WPCOM_BACKGROUND_SERVICE_UPDATE,
        SIGNUP_EMAIL_BUTTON_TAPPED,
        SIGNUP_GOOGLE_BUTTON_TAPPED,
        SIGNUP_TERMS_OF_SERVICE_TAPPED,
        SIGNUP_CANCELED,
        SIGNUP_EMAIL_TO_LOGIN,
        SIGNUP_MAGIC_LINK_FAILED,
        SIGNUP_MAGIC_LINK_OPENED,
        SIGNUP_MAGIC_LINK_OPEN_EMAIL_CLIENT_CLICKED,
        SIGNUP_MAGIC_LINK_SENT,
        SIGNUP_MAGIC_LINK_SUCCEEDED,
        SIGNUP_SOCIAL_ACCOUNTS_NEED_CONNECTING,
        SIGNUP_SOCIAL_BUTTON_FAILURE,
        SIGNUP_SOCIAL_TO_LOGIN,
        ADDED_SELF_HOSTED_SITE,
        CREATED_ACCOUNT,
        LOGIN_PROLOGUE_OPENED_JETPACK_LINK,
        LOGIN_EPILOGUE_VIEWED,

        // -- Top-level navigation
        OPENED_DASHBOARD,
        OPENED_ORDER_LIST,
        OPENED_NOTIFICATIONS,
        RESELECTED_DASHBOARD,
        RESELECTED_ORDER_LIST,
        RESELECTED_NOTIFICATIONS,

        OPENED_ORDER_DETAIL,
        FULFILLED_ORDER,

        OPENED_SETTINGS
    }

    private var tracksClient = TracksClient.getClient(context)
    private var username: String? = null
    private var anonymousID: String? = null

    private fun clearAllData() {
        clearAnonID()
        username = null
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

        val propertiesJson = JSONObject(properties)
        tracksClient.track(EVENTS_PREFIX + eventName, propertiesJson, user, userType)

        if (propertiesJson.length() > 0) {
            WooLog.i(T.UTILS, "\uD83D\uDD35 Tracked: $eventName, Properties: $propertiesJson")
        } else {
            WooLog.i(T.UTILS, "\uD83D\uDD35 Tracked: $eventName")
        }
    }

    private fun flush() {
        tracksClient.flush()
    }

    private fun refreshMetadata(newUsername: String?) {
        if (!newUsername.isNullOrEmpty()) {
            username = newUsername
            if (getAnonID() != null) {
                tracksClient.trackAliasUser(username, getAnonID(), TracksClient.NosaraUserType.WPCOM)
                clearAnonID()
            }
        } else {
            username = null
            if (getAnonID() == null) {
                generateNewAnonID()
            }
        }
    }

    companion object {
        // Guaranteed to hold a reference to the application context, which is safe
        @SuppressLint("StaticFieldLeak")
        private var instance: AnalyticsTracker? = null
        private const val TRACKS_ANON_ID = "nosara_tracks_anon_id"
        private const val EVENTS_PREFIX = "woocommerceandroid_"

        private const val BLOG_ID_KEY = "blog_id"
        private const val IS_WPCOM_STORE = "is_wpcom_store"

        private const val PREFKEY_SEND_USAGE_STATS = "wp_pref_send_usage_stats"

        var sendUsageStats: Boolean = true
            set(value) {
                if (value != field) {
                    field = value
                    val prefs = PreferenceManager.getDefaultSharedPreferences(instance?.context)
                    prefs.edit().putBoolean(PREFKEY_SEND_USAGE_STATS, field).apply()
                }
            }

        fun init(context: Context) {
            instance = AnalyticsTracker(context.applicationContext)
            val prefs = PreferenceManager.getDefaultSharedPreferences(context)
            sendUsageStats = prefs.getBoolean(PREFKEY_SEND_USAGE_STATS, true)
        }

        fun track(stat: Stat) {
            track(stat, emptyMap<String, String>())
        }

        fun track(stat: Stat, properties: Map<String, *>) {
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
        fun track(stat: Stat, errorContext: String, errorType: String, errorDescription: String) {
            val props = HashMap<String, String>()
            props["error_context"] = errorContext
            props["error_type"] = errorType
            props["error_description"] = errorDescription
            track(stat, props)
        }

        fun trackWithSiteDetails(stat: Stat, site: SiteModel, properties: MutableMap<String, Any> = mutableMapOf()) {
            properties[BLOG_ID_KEY] = site.siteId
            properties[IS_WPCOM_STORE] = site.isWpComStore

            AnalyticsTracker.track(stat, properties)
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
