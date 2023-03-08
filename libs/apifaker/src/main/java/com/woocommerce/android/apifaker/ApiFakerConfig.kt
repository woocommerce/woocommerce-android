package com.woocommerce.android.apifaker

import com.woocommerce.android.apifaker.db.EndpointDao
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.withContext
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class ApiFakerConfig @Inject constructor() {
    @Inject
    internal lateinit var endpointDao: EndpointDao

    private val _enabled: MutableStateFlow<Boolean> = MutableStateFlow(false)
    val enabled = _enabled.asStateFlow()
        .map {
            it && withContext(Dispatchers.IO) {
                endpointDao.endpointsCount() > 0
            }
        }

    internal fun setStatus(enabled: Boolean) {
        _enabled.value = enabled
    }
}
