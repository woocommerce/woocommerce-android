package com.woocommerce.android.ui.login.storecreation.installation

import com.woocommerce.android.ui.common.PluginRepository
import com.woocommerce.android.ui.login.storecreation.StoreCreationErrorType
import com.woocommerce.android.ui.login.storecreation.StoreCreationErrorType.STORE_NOT_READY
import com.woocommerce.android.ui.login.storecreation.StoreCreationRepository
import com.woocommerce.android.ui.login.storecreation.StoreCreationResult.Failure
import com.woocommerce.android.ui.login.storecreation.StoreCreationResult.Success
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
    private val pluginRepository: PluginRepository
) {
    private companion object {
        const val JETPACK_PLUGIN_SLUG = "jetpack"
        const val WOOCOMMERCE_PLUGIN_SLUG = "woocommerce"
    }

    suspend operator fun invoke(
        siteId: Long,
        expectedName: String,
    ): Flow<InstallationState> {
        return flow {
            emit(InstallationState.InProgress)
            delay(InstallationConst.INITIAL_STORE_CREATION_DELAY)

            var newSiteModel = getNewSiteModel(siteId)
            if (newSiteModel == null) {
                emit(InstallationState.Failure(STORE_NOT_READY))
                return@flow
            }

            repeat(STORE_LOAD_RETRIES_LIMIT) { retryIteration ->

                if (retryIteration == STORE_LOAD_RETRIES_LIMIT - 1) {
                    emit(InstallationState.Failure(STORE_NOT_READY))
                    return@flow
                }

                if (newSiteModel?.isJetpackConnected == false) {
                    newSiteModel = getNewSiteModel(siteId)
                } else {
                    val installedPlugins = pluginRepository.fetchSitePlugins(newSiteModel!!)
                        .getOrNull() ?: emptyList()

                    val jetpackActive = installedPlugins
                        .firstOrNull { it.slug == JETPACK_PLUGIN_SLUG }
                        ?.isActive ?: false
                    val wooActive = installedPlugins
                        .firstOrNull { it.slug == WOOCOMMERCE_PLUGIN_SLUG }
                        ?.isActive ?: false

                    if (jetpackActive && wooActive) {
                        if (newSiteModel.isDesynced(expectedName)) {
                            emit(InstallationState.OutOfSync)
                        }
                        emit(InstallationState.Success)
                        return@flow
                    }
                }

                delay(SITE_CHECK_DEBOUNCE)
            }
        }
    }

    private suspend fun getNewSiteModel(siteId: Long) = when (storeCreationRepository.fetchSite(siteId)) {
        is Failure -> null
        is Success -> withContext(dispatchers.io) {
            siteStore.getSiteBySiteId(siteId)
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
