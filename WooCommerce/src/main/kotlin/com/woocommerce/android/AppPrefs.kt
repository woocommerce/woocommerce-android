package com.woocommerce.android

import android.annotation.SuppressLint
import android.content.Context
import android.preference.PreferenceManager
import android.text.TextUtils

// Guaranteed to hold a reference to the application context, which is safe
@SuppressLint("StaticFieldLeak")
object AppPrefs {
    private interface PrefKey

    private lateinit var context: Context

    /**
     * Application related preferences. When the user logs out, these preferences are erased.
     */
    private enum class DeletablePrefKey : PrefKey

    /**
     * These preferences won't be deleted when the user disconnects.
     * They should be used for device specific or user-independent preferences.
     */
    private enum class UndeletablePrefKey : PrefKey {
        // The last stored versionCode of the app
        LAST_APP_VERSION_CODE
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

    /**
     * Remove all user-related preferences.
     */
    fun reset() {
        val editor = getPreferences().edit()
        DeletablePrefKey.values().forEach { editor.remove(it.name) }
        editor.apply()
    }

    private fun getInt(key: PrefKey, default: Int = 0): Int {
        return try {
            val value = getString(key)
            if (value.isEmpty()) {
                default
            } else Integer.parseInt(value)
        } catch (e: NumberFormatException) {
            default
        }
    }

    private fun setInt(key: PrefKey, value: Int) {
        setString(key, Integer.toString(value))
    }

    private fun getString(key: PrefKey, defaultValue: String = ""): String {
        return getPreferences().getString(key.toString(), defaultValue)
    }

    private fun setString(key: PrefKey, value: String) {
        val editor = getPreferences().edit()
        if (TextUtils.isEmpty(value)) {
            editor.remove(key.toString())
        } else {
            editor.putString(key.toString(), value)
        }
        editor.apply()
    }

    private fun getPreferences() = PreferenceManager.getDefaultSharedPreferences(context)
}
