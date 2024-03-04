package com.woocommerce.android.ui.orders.connectivitytool.useCases

import com.woocommerce.android.ui.orders.connectivitytool.OrderConnectivityToolViewModel.ConnectivityTestStatus
import com.woocommerce.android.util.BuildConfigWrapper
import javax.inject.Inject
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import org.wordpress.android.fluxc.store.WhatsNewStore

class WordPressConnectionTestUseCase @Inject constructor(
    private val whatsNewStore: WhatsNewStore,
    private val buildConfigWrapper: BuildConfigWrapper
) {
    operator fun invoke(): Flow<ConnectivityTestStatus> = flow {
        emit(ConnectivityTestStatus.InProgress)
        whatsNewStore.fetchRemoteAnnouncements(
            versionName = buildConfigWrapper.versionName,
            appId = WhatsNewStore.WhatsNewAppId.WOO_ANDROID
        ).fetchError?.let {
            emit(ConnectivityTestStatus.Failure)
        } ?: emit(ConnectivityTestStatus.Success)
    }
}
