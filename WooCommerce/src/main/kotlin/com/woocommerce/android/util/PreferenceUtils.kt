package com.woocommerce.android.util

import android.content.SharedPreferences

object PreferenceUtils {
    fun getInt(preferences: SharedPreferences, key: String, default: Int = 0): Int {
        return try {
            getString(preferences, key)?.let { value ->
                if (value.isEmpty()) {
                    default
                } else Integer.parseInt(value)
            } ?: default
        } catch (e: NumberFormatException) {
            default
        }
    }

    fun setInt(preferences: SharedPreferences, key: String, value: Int) {
        setString(preferences, key, value.toString())
    }

    fun getString(preferences: SharedPreferences, key: String, defaultValue: String = ""): String? {
        return preferences.getString(key, defaultValue)
    }

    fun setString(preferences: SharedPreferences, key: String, value: String?) {
        val editor = preferences.edit()
        if (value.isNullOrEmpty()) {
            editor.remove(key)
        } else {
            editor.putString(key, value)
        }
        editor.apply()
    }

    fun getBoolean(preferences: SharedPreferences, key: String, default: Boolean = false): Boolean {
        return preferences.getBoolean(key, default)
    }

    fun setBoolean(preferences: SharedPreferences, key: String, value: Boolean) {
        preferences.edit().putBoolean(key, value).apply()
    }
}
