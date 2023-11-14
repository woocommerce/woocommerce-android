package com.woocommerce.android.config

import kotlinx.coroutines.flow.Flow

interface RemoteConfigRepository {
    val fetchStatus: Flow<RemoteConfigFetchStatus>
    fun fetchRemoteConfig()
}

enum class RemoteConfigFetchStatus {
    Pending, Success, Failure
}
