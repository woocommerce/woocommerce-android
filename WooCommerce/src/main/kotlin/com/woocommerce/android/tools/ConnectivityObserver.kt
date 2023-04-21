package com.woocommerce.android.tools

import android.content.Context
import com.woocommerce.android.tools.ConnectivityObserver.Status.CONNECTED
import com.woocommerce.android.tools.ConnectivityObserver.Status.DISCONNECTED
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import org.wordpress.android.util.NetworkUtils
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class ConnectivityObserver @Inject constructor(@ApplicationContext context: Context) {

    private val state =
        MutableStateFlow(mapToStatus(NetworkUtils.isNetworkAvailable(context)))

    fun observe(): Flow<Status> = state

    suspend fun update(isConnected: Boolean) {
        state.emit(mapToStatus(isConnected))
    }

    private fun mapToStatus(isConnected: Boolean): Status {
        return if (isConnected) CONNECTED else DISCONNECTED
    }

    enum class Status {
        CONNECTED, DISCONNECTED
    }
}
