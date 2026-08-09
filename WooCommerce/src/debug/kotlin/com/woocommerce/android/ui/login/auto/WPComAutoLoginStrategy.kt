package com.woocommerce.android.ui.login.auto

import com.woocommerce.android.ui.login.AccountRepository
import com.woocommerce.android.ui.login.WPComLoginRepository
import com.woocommerce.android.ui.login.jetpack.JetpackActivationRepository
import kotlinx.coroutines.CancellationException
import org.wordpress.android.fluxc.model.SiteModel
import org.wordpress.android.fluxc.store.AccountStore
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
internal class WPComAutoLoginStrategy @Inject constructor(
    private val wpComLoginRepository: WPComLoginRepository,
    private val accountRepository: AccountRepository,
    private val accountStore: AccountStore,
    private val jetpackActivationRepository: JetpackActivationRepository
) {
    suspend fun login(
        request: AutoLoginRequest,
        reuseExistingSession: Boolean
    ): Result {
        require(request.connection == AutoLoginConnection.WPCOM)
        val sessionFailure = prepareSession(request, reuseExistingSession)
        return sessionFailure ?: fetchAndSelectSite(request.siteUrl)
    }

    private suspend fun prepareSession(
        request: AutoLoginRequest,
        reuseExistingSession: Boolean
    ): Result.Failure? {
        if (reuseExistingSession) {
            return Result.Failure(AutoLoginStatus.CONFLICT).takeUnless { hasWpComSession() }
        }
        if (accountStore.hasAccessToken() || accountStore.account.userId > 0L) {
            return Result.Failure(AutoLoginStatus.CONFLICT)
        }

        val loginResult = try {
            wpComLoginRepository.login(
                request.credentials.username,
                request.credentials.password
            )
        } catch (cancellationException: CancellationException) {
            throw cancellationException
        } catch (_: Exception) {
            return Result.Failure(AutoLoginStatus.AUTH_FAILED)
        }

        return when (loginResult) {
            WPComLoginRepository.LoginResult.Success -> fetchAccount()
            is WPComLoginRepository.LoginResult.TwoFactorRequired ->
                Result.Failure(AutoLoginStatus.AUTH_REQUIRES_2FA)

            is WPComLoginRepository.LoginResult.Error ->
                Result.Failure(AutoLoginStatus.AUTH_FAILED)
        }
    }

    private suspend fun fetchAccount(): Result.Failure? {
        val accountResult = try {
            accountRepository.fetchUserAccount()
        } catch (cancellationException: CancellationException) {
            throw cancellationException
        } catch (_: Exception) {
            return Result.Failure(AutoLoginStatus.AUTH_FAILED)
        }

        accountResult.exceptionOrNull()?.let { exception ->
            if (exception is CancellationException) throw exception
            return Result.Failure(AutoLoginStatus.AUTH_FAILED)
        }
        return Result.Failure(AutoLoginStatus.AUTH_FAILED).takeUnless { hasWpComSession() }
    }

    private suspend fun fetchAndSelectSite(siteUrl: String): Result {
        val siteResult = try {
            jetpackActivationRepository.fetchJetpackSite(siteUrl)
        } catch (cancellationException: CancellationException) {
            throw cancellationException
        } catch (_: Exception) {
            return Result.Failure(AutoLoginStatus.SITE_FAILED)
        }

        siteResult.exceptionOrNull()?.let { exception ->
            if (exception is CancellationException) throw exception
            return Result.Failure(AutoLoginStatus.SITE_FAILED)
        }
        return select(checkNotNull(siteResult.getOrNull()))
    }

    private fun select(target: SiteModel): Result =
        try {
            jetpackActivationRepository.setSelectedSiteAndCleanOldSites(target)
            Result.Success
        } catch (cancellationException: CancellationException) {
            throw cancellationException
        } catch (_: Exception) {
            Result.Failure(AutoLoginStatus.INTERNAL_ERROR)
        }

    private fun hasWpComSession(): Boolean =
        accountStore.hasAccessToken() && accountStore.account.userId > 0L

    sealed interface Result {
        data object Success : Result
        data class Failure(val status: AutoLoginStatus) : Result
    }
}
