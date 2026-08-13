package com.woocommerce.android.ui.login.auto

import com.woocommerce.android.tools.SelectedSite
import com.woocommerce.android.ui.login.WPApiSiteRepository
import kotlinx.coroutines.CancellationException
import org.wordpress.android.fluxc.model.SiteModel
import javax.inject.Inject

internal class ApplicationPasswordAutoLoginStrategy @Inject constructor(
    private val wpApiSiteRepository: WPApiSiteRepository,
    private val selectedSite: SelectedSite
) {
    suspend fun login(request: AutoLoginRequest): Result {
        require(request.connection == AutoLoginConnection.WP_API)
        val site = wpApiSiteRepository.fetchSite(request.siteUrl).getOrElse {
            return Result.Failure(AutoLoginStatus.SITE_FAILED)
        }
        if (site.origin != SiteModel.ORIGIN_WPAPI) {
            return Result.Failure(AutoLoginStatus.SITE_FAILED)
        }

        val persistedSite = persistCredentials(site, request)
            ?: return Result.Failure(AutoLoginStatus.INTERNAL_ERROR)
        return authenticateAndSelect(persistedSite)
    }

    private suspend fun persistCredentials(
        site: SiteModel,
        request: AutoLoginRequest
    ): SiteModel? {
        return try {
            wpApiSiteRepository.saveApplicationPassword(
                localSiteId = site.id,
                username = request.credentials.username,
                password = request.credentials.password
            )
            wpApiSiteRepository.getSiteByLocalId(site.id)
                ?.takeIf {
                    it.origin == SiteModel.ORIGIN_WPAPI &&
                        it.username == request.credentials.username
                }
        } catch (cancellationException: CancellationException) {
            throw cancellationException
        } catch (_: Exception) {
            null
        }
    }

    private suspend fun authenticateAndSelect(persistedSite: SiteModel): Result {
        val authenticatedUser = wpApiSiteRepository.checkIfUserIsEligible(persistedSite)
        return if (authenticatedUser.isFailure) {
            Result.Failure(AutoLoginStatus.AUTH_FAILED)
        } else {
            selectedSite.set(persistedSite)
            Result.Success
        }
    }

    sealed interface Result {
        data object Success : Result
        data class Failure(val status: AutoLoginStatus) : Result
    }
}
