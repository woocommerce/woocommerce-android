package com.woocommerce.android.ui.login

import com.woocommerce.android.AppPrefsWrapper
import com.woocommerce.android.tools.SelectedSite
import com.woocommerce.android.util.CoroutineDispatchers
import kotlinx.coroutines.withContext
import javax.inject.Inject

class LoginWithApplicationPasswordLink @Inject constructor(
    private val wpApiSiteRepository: WPApiSiteRepository,
    private val selectedSite: SelectedSite,
    private val appPrefs: AppPrefsWrapper,
    private val dispatchers: CoroutineDispatchers
) {
    suspend operator fun invoke(
        siteUrl: String,
        username: String,
        applicationPassword: String,
        uuid: String
    ): Result<Unit> {
        return withContext(dispatchers.io) {
            wpApiSiteRepository.fetchSite(siteUrl, null, null).fold(
                onSuccess = { siteModel ->
                    siteModel.apply { this.username = username }
                    wpApiSiteRepository.saveApplicationPassword(siteModel.id, username, applicationPassword, uuid)
                    wpApiSiteRepository.checkIfUserIsEligible(siteModel).fold(
                        onSuccess = { isEligible ->
                            if (isEligible) {
                                appPrefs.removeLoginSiteAddress()
                                selectedSite.set(siteModel)
                            }
                            Result.success(Unit)
                        },
                        onFailure = { exception -> Result.failure(exception) }
                    )
                },
                onFailure = { exception -> Result.failure(exception) }
            )
        }
    }
}
