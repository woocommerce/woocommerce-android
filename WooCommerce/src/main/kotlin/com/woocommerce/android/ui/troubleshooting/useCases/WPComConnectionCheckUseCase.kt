package com.woocommerce.android.ui.troubleshooting.useCases

import com.woocommerce.android.ui.troubleshooting.ConnectivityCheckStatus
import com.woocommerce.android.ui.troubleshooting.ConnectivityCheckStatus.Failure
import com.woocommerce.android.ui.troubleshooting.ConnectivityCheckStatus.InProgress
import com.woocommerce.android.ui.troubleshooting.ConnectivityCheckStatus.Success
import com.woocommerce.android.util.BuildConfigWrapper
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import org.wordpress.android.fluxc.store.WhatsNewStore
import javax.inject.Inject

class WPComConnectionCheckUseCase @Inject constructor(
    private val whatsNewStore: WhatsNewStore,
    private val buildConfigWrapper: BuildConfigWrapper
) {
    operator fun invoke(): Flow<ConnectivityCheckStatus> = flow {
        emit(InProgress)
        val startTime = System.currentTimeMillis()
        val result = whatsNewStore.fetchRemoteAnnouncements(
            versionName = buildConfigWrapper.versionName
        )
        val durationMs = System.currentTimeMillis() - startTime
        if (result.fetchError != null) {
            emit(Failure(durationMs = durationMs))
        } else {
            emit(Success(durationMs = durationMs))
        }
    }

    companion object {
        const val OPERATION_NAME = "Connecting to WordPress.com Servers"
    }
}
