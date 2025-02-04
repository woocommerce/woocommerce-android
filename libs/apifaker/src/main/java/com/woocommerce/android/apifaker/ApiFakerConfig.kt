package com.woocommerce.android.apifaker

import android.content.Context
import android.content.SharedPreferences
import com.woocommerce.android.apifaker.db.EndpointDao
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import javax.inject.Inject
import javax.inject.Singleton

private const val PREF_FILE_NAME = "api_faker"
private const val PREFERENCE_KEY = "api_faker_enabled"

@Singleton
internal class ApiFakerConfig @Inject constructor(
    context: Context,
    endpointDao: EndpointDao
) {
    private val configScope = CoroutineScope(Dispatchers.Main)
    private val preferences = context.getSharedPreferences(PREF_FILE_NAME, Context.MODE_PRIVATE)

    private val prefFlow = preferences.prefFlow(PREFERENCE_KEY, false)

    val enabled = combine(
        prefFlow,
        endpointDao.observeEndpointsCount().map { it == 0 }
    ) { pref, isEmpty ->
        pref && !isEmpty
    }.stateIn(configScope, SharingStarted.Eagerly, false)

    fun setStatus(enabled: Boolean) {
        preferences.edit().putBoolean(PREFERENCE_KEY, enabled).apply()
    }

    private fun SharedPreferences.prefFlow(key: String, defaultValue: Boolean) = callbackFlow {
        val listener = SharedPreferences.OnSharedPreferenceChangeListener { _, listenerKey ->
            if (listenerKey == key) {
                trySend(getBoolean(key, defaultValue))
            }
        }
        registerOnSharedPreferenceChangeListener(listener)
        trySend(getBoolean(key, defaultValue))
        awaitClose { unregisterOnSharedPreferenceChangeListener(listener) }
    }
}
