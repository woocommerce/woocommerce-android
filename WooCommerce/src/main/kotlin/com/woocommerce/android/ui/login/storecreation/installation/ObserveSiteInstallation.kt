package com.woocommerce.android.ui.login.storecreation.installation

import com.woocommerce.android.extensions.isWooExpressSiteReadyToUse
import com.woocommerce.android.ui.login.storecreation.StoreCreationErrorType
import com.woocommerce.android.ui.login.storecreation.StoreCreationErrorType.STORE_LOADING_FAILED
import com.woocommerce.android.ui.login.storecreation.StoreCreationErrorType.STORE_NOT_READY
import com.woocommerce.android.ui.login.storecreation.StoreCreationRepository
import com.woocommerce.android.ui.login.storecreation.StoreCreationResult
import com.woocommerce.android.ui.login.storecreation.installation.InstallationConst.INITIAL_STORE_CREATION_DELAY
import com.woocommerce.android.ui.login.storecreation.installation.InstallationConst.SITE_CHECK_DEBOUNCE
import com.woocommerce.android.ui.login.storecreation.installation.InstallationConst.STORE_LOAD_RETRIES_LIMIT
import com.woocommerce.android.util.CoroutineDispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.withContext
import org.wordpress.android.fluxc.model.SiteModel
import org.wordpress.android.fluxc.store.SiteStore
import javax.inject.Inject

class ObserveSiteInstallation @Inject constructor(
    private val storeCreationRepository: StoreCreationRepository,
    private val siteStore: SiteStore,
    private val dispatchers: CoroutineDispatchers,
) {

    suspend operator fun invoke(
        siteId: Long,
        expectedName: String,
    ): Flow<InstallationState> {
        return flow {
            emit(InstallationState.InProgress)
            delay(INITIAL_STORE_CREATION_DELAY)

            repeat(STORE_LOAD_RETRIES_LIMIT) { retryIteration ->

                if (retryIteration == STORE_LOAD_RETRIES_LIMIT - 1) {
                    emit(InstallationState.Failure(STORE_NOT_READY))
                    return@flow
                }

                when (storeCreationRepository.fetchSite(siteId)) {
                    is StoreCreationResult.Success -> {
                        val site = withContext(dispatchers.io) {
                            siteStore.getSiteBySiteId(siteId)
                        }

                        if (site.isDesynced(expectedName)) {
                            emit(InstallationState.OutOfSync)
                        }

                        if (site.isWooExpressSiteReadyToUse) {
                            emit(InstallationState.Success)
                            return@flow
                        }
                    }

                    is StoreCreationResult.Failure -> {
                        emit(InstallationState.Failure(STORE_LOADING_FAILED))
                        return@flow
                    }
                }

                delay(SITE_CHECK_DEBOUNCE)
            }
        }
    }

    private fun SiteModel?.isDesynced(expectedName: String): Boolean =
        this?.isJetpackInstalled == true && this.isJetpackConnected &&
            (!this.isWpComStore || !this.hasWooCommerce || this.name != expectedName)

    sealed interface InstallationState {
        object Success : InstallationState
        data class Failure(val type: StoreCreationErrorType, val message: String? = null) :
            InstallationState

        object OutOfSync : InstallationState
        object InProgress : InstallationState
    }
}
