package com.woocommerce.android.apifaker

import com.woocommerce.android.apifaker.db.EndpointDao
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
internal class ApiFakerConfig @Inject constructor() {
    @Inject
    internal lateinit var endpointDao: EndpointDao

    private val _enabled: MutableStateFlow<Boolean> = MutableStateFlow(false)
    val enabled = _enabled.asStateFlow()

    internal suspend fun setStatus(enabled: Boolean) {
        _enabled.value = enabled && endpointDao.endpointsCount() > 0
    }
}
