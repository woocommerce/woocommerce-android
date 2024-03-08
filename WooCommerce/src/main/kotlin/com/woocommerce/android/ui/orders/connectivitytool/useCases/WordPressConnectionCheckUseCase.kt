package com.woocommerce.android.ui.orders.connectivitytool.useCases

import com.woocommerce.android.ui.orders.connectivitytool.ConnectivityCheckStatus
import com.woocommerce.android.ui.orders.connectivitytool.ConnectivityCheckStatus.Failure
import com.woocommerce.android.ui.orders.connectivitytool.ConnectivityCheckStatus.InProgress
import com.woocommerce.android.ui.orders.connectivitytool.ConnectivityCheckStatus.Success
import com.woocommerce.android.util.BuildConfigWrapper
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import org.wordpress.android.fluxc.store.WhatsNewStore
import javax.inject.Inject

class WordPressConnectionCheckUseCase @Inject constructor(
    private val whatsNewStore: WhatsNewStore,
    private val buildConfigWrapper: BuildConfigWrapper
) {
    operator fun invoke(): Flow<ConnectivityCheckStatus> = flow {
        emit(InProgress)
        whatsNewStore.fetchRemoteAnnouncements(
            versionName = buildConfigWrapper.versionName,
            appId = WhatsNewStore.WhatsNewAppId.WOO_ANDROID
        ).fetchError?.let {
            emit(Failure())
        } ?: emit(Success)
    }
}
