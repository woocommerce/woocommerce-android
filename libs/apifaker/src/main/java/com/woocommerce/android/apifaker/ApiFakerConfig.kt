package com.woocommerce.android.apifaker

import android.content.Context
import com.woocommerce.android.apifaker.db.EndpointDao
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import javax.inject.Inject
import javax.inject.Singleton

private const val PREF_FILE_NAME = "api_faker"
private const val PREFERENCE_KEY = "api_faker_enabled"

@Singleton
internal class ApiFakerConfig @Inject constructor(
    context: Context,
    private val endpointDao: EndpointDao
) {
    private val preferences = context.getSharedPreferences(PREF_FILE_NAME, Context.MODE_PRIVATE)

    private val _enabled: MutableStateFlow<Boolean> = MutableStateFlow(preferences.getBoolean(PREFERENCE_KEY, false))
    val enabled = _enabled.asStateFlow()

    suspend fun setStatus(enabled: Boolean) {
        val status = enabled && !endpointDao.isEmpty()
        _enabled.value = status
        preferences.edit().putBoolean(PREFERENCE_KEY, status).apply()
    }
}
