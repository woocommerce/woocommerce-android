package com.woocommerce.android

import android.annotation.SuppressLint
import android.content.Context
import android.preference.PreferenceManager
import com.woocommerce.android.tools.SelectedSite
import com.woocommerce.android.util.PreferenceUtils

// Guaranteed to hold a reference to the application context, which is safe
@SuppressLint("StaticFieldLeak")
object AppPrefs {
    private interface PrefKey

    private lateinit var context: Context

    /**
     * Application related preferences. When the user logs out, these preferences are erased.
     */
    private enum class DeletablePrefKey : PrefKey {
        SUPPORT_EMAIL,
        SUPPORT_NAME,
        IS_USING_V3_API,
        HAS_UNSEEN_NOTIFS,
        SELECTED_SHIPMENT_TRACKING_PROVIDER_NAME,
        SELECTED_SHIPMENT_TRACKING_PROVIDER_IS_CUSTOM
    }

    /**
     * These preferences won't be deleted when the user disconnects.
     * They should be used for device specific or user-independent preferences.
     */
    private enum class UndeletablePrefKey : PrefKey {
        // The last stored versionCode of the app
        LAST_APP_VERSION_CODE,
        // Whether or not automatic crash reporting is enabled
        ENABLE_CRASH_REPORTING,
        // Enable notifications for new orders
        NOTIFS_ORDERS_ENABLED,
        // Enable notifications for new reviews
        NOTIFS_REVIEWS_ENABLED,
        // Play cha-ching sound on new order notifications
        NOTIFS_ORDERS_CHA_CHING_ENABLED,
        // Number of times the "mark all notifications read" icon was tapped
        NUM_TIMES_MARK_ALL_NOTIFS_READ_SNACK_SHOWN
    }

    fun init(context: Context) {
        AppPrefs.context = context.applicationContext
    }

    fun getLastAppVersionCode(): Int {
        return getInt(UndeletablePrefKey.LAST_APP_VERSION_CODE)
    }

    fun setLastAppVersionCode(versionCode: Int) {
        setInt(UndeletablePrefKey.LAST_APP_VERSION_CODE, versionCode)
    }

    fun setSupportEmail(email: String?) {
        if (!email.isNullOrEmpty()) {
            setString(DeletablePrefKey.SUPPORT_EMAIL, email!!)
        } else {
            remove(DeletablePrefKey.SUPPORT_EMAIL)
        }
    }

    fun getSupportEmail() = getString(DeletablePrefKey.SUPPORT_EMAIL)

    fun hasSupportEmail() = !getSupportEmail().isEmpty()

    fun removeSupportEmail() {
        remove(DeletablePrefKey.SUPPORT_EMAIL)
    }

    fun setSupportName(name: String) {
        setString(DeletablePrefKey.SUPPORT_NAME, name)
    }

    fun getSupportName() = getString(DeletablePrefKey.SUPPORT_NAME)

    fun removeSupportName() {
        remove(DeletablePrefKey.SUPPORT_NAME)
    }

    fun isUsingV3Api() = getBoolean(DeletablePrefKey.IS_USING_V3_API, false)

    fun setIsUsingV3Api() = setBoolean(DeletablePrefKey.IS_USING_V3_API, true)

    fun isCrashReportingEnabled(): Boolean {
        // default to False for debug builds
        val default = !BuildConfig.DEBUG
        return getBoolean(UndeletablePrefKey.ENABLE_CRASH_REPORTING, default)
    }

    fun setCrashReportingEnabled(enabled: Boolean) {
        setBoolean(UndeletablePrefKey.ENABLE_CRASH_REPORTING, enabled)
    }

    fun isOrderNotificationsEnabled() = getBoolean(UndeletablePrefKey.NOTIFS_ORDERS_ENABLED, true)

    fun setOrderNotificationsEnabled(enabled: Boolean) {
        setBoolean(UndeletablePrefKey.NOTIFS_ORDERS_ENABLED, enabled)
    }

    fun isReviewNotificationsEnabled() = getBoolean(UndeletablePrefKey.NOTIFS_REVIEWS_ENABLED, true)

    fun setReviewNotificationsEnabled(enabled: Boolean) {
        setBoolean(UndeletablePrefKey.NOTIFS_REVIEWS_ENABLED, enabled)
    }

    fun isOrderNotificationsChaChingEnabled() = getBoolean(UndeletablePrefKey.NOTIFS_ORDERS_CHA_CHING_ENABLED, true)

    fun setOrderNotificationsChaChingEnabled(enabled: Boolean) {
        setBoolean(UndeletablePrefKey.NOTIFS_ORDERS_CHA_CHING_ENABLED, enabled)
    }

    fun getHasUnseenNotifs() = getBoolean(DeletablePrefKey.HAS_UNSEEN_NOTIFS, false)

    fun setHasUnseenNotifs(hasUnseen: Boolean) {
        setBoolean(DeletablePrefKey.HAS_UNSEEN_NOTIFS, hasUnseen)
    }

    fun getNumTimesMarkAllReadSnackShown(): Int =
            getInt(UndeletablePrefKey.NUM_TIMES_MARK_ALL_NOTIFS_READ_SNACK_SHOWN, 0)

    fun incNumTimesMarkAllReadSnackShown() {
        val numTimesShown = getNumTimesMarkAllReadSnackShown() + 1
        setInt(UndeletablePrefKey.NUM_TIMES_MARK_ALL_NOTIFS_READ_SNACK_SHOWN, numTimesShown)
    }

    fun getSelectedShipmentTrackingProviderName(): String =
            getString(DeletablePrefKey.SELECTED_SHIPMENT_TRACKING_PROVIDER_NAME)

    fun setSelectedShipmentTrackingProviderName(providerName: String) {
        setString(DeletablePrefKey.SELECTED_SHIPMENT_TRACKING_PROVIDER_NAME, providerName)
    }

    fun getIsSelectedShipmentTrackingProviderCustom(): Boolean =
            getBoolean(DeletablePrefKey.SELECTED_SHIPMENT_TRACKING_PROVIDER_IS_CUSTOM, false)

    fun setIsSelectedShipmentTrackingProviderNameCustom(isCustomProvider: Boolean) {
        setBoolean(DeletablePrefKey.SELECTED_SHIPMENT_TRACKING_PROVIDER_IS_CUSTOM, isCustomProvider)
    }

    /**
     * Remove all user-related preferences.
     */
    fun reset() {
        val editor = getPreferences().edit()
        DeletablePrefKey.values().forEach { editor.remove(it.name) }
        editor.remove(SelectedSite.SELECTED_SITE_LOCAL_ID)
        editor.apply()
    }

    private fun getInt(key: PrefKey, default: Int = 0) =
            PreferenceUtils.getInt(getPreferences(), key.toString(), default)

    private fun setInt(key: PrefKey, value: Int) =
            PreferenceUtils.setInt(getPreferences(), key.toString(), value)

    private fun getString(key: PrefKey, defaultValue: String = "") =
            PreferenceUtils.getString(getPreferences(), key.toString(), defaultValue)

    private fun setString(key: PrefKey, value: String) =
            PreferenceUtils.setString(getPreferences(), key.toString(), value)

    private fun getBoolean(key: PrefKey, default: Boolean) =
            PreferenceUtils.getBoolean(getPreferences(), key.toString(), default)

    private fun setBoolean(key: PrefKey, value: Boolean = false) =
            PreferenceUtils.setBoolean(getPreferences(), key.toString(), value)

    private fun getPreferences() = PreferenceManager.getDefaultSharedPreferences(context)

    private fun remove(key: PrefKey) {
        getPreferences().edit().remove(key.toString()).apply()
    }
}
