package com.woocommerce.android.ui.login.auto

import com.woocommerce.android.ui.login.AccountRepository
import com.woocommerce.android.ui.login.WPComLoginRepository
import com.woocommerce.android.ui.login.jetpack.JetpackActivationRepository
import com.woocommerce.android.viewmodel.BaseUnitTest
import kotlinx.coroutines.ExperimentalCoroutinesApi
import org.assertj.core.api.Assertions.assertThat
import org.junit.Before
import org.junit.Test
import org.mockito.kotlin.any
import org.mockito.kotlin.doReturn
import org.mockito.kotlin.doSuspendableAnswer
import org.mockito.kotlin.inOrder
import org.mockito.kotlin.mock
import org.mockito.kotlin.never
import org.mockito.kotlin.verify
import org.mockito.kotlin.verifyNoInteractions
import org.mockito.kotlin.whenever
import org.wordpress.android.fluxc.model.AccountModel
import org.wordpress.android.fluxc.model.SiteModel
import org.wordpress.android.fluxc.store.AccountStore
import org.wordpress.android.fluxc.store.AccountStore.AuthenticationError
import org.wordpress.android.fluxc.store.AccountStore.AuthenticationErrorType.INVALID_TOKEN

@Suppress("FunctionNaming")
@OptIn(ExperimentalCoroutinesApi::class)
class WPComAutoLoginStrategyTest : BaseUnitTest() {
    private val loginRepository: WPComLoginRepository = mock()
    private val accountRepository: AccountRepository = mock()
    private val accountStore: AccountStore = mock()
    private val jetpackRepository: JetpackActivationRepository = mock()
    private val account = AccountModel()
    private val targetSite = SiteModel()
    private var hasToken = false
    private val strategy = WPComAutoLoginStrategy(
        loginRepository,
        accountRepository,
        accountStore,
        jetpackRepository
    )

    @Before
    fun setUp() = testBlocking {
        whenever(accountStore.account).thenReturn(account)
        whenever(accountStore.hasAccessToken()).thenAnswer { hasToken }
        whenever(loginRepository.login(USERNAME, PASSWORD)).doSuspendableAnswer {
            hasToken = true
            WPComLoginRepository.LoginResult.Success
        }
        whenever(accountRepository.fetchUserAccount()).doSuspendableAnswer {
            account.userId = ACCOUNT_ID
            Result.success(Unit)
        }
        whenever(jetpackRepository.fetchJetpackSite(SITE_URL)).thenReturn(Result.success(targetSite))
    }

    @Test
    fun `when fresh login succeeds, then production repositories run in order before selection`() = testBlocking {
        val result = strategy.login(request(), reuseExistingSession = false)

        assertThat(result).isEqualTo(WPComAutoLoginStrategy.Result.Success)
        inOrder(loginRepository, accountRepository, jetpackRepository).run {
            verify(loginRepository).login(USERNAME, PASSWORD)
            verify(accountRepository).fetchUserAccount()
            verify(jetpackRepository).fetchJetpackSite(SITE_URL)
            verify(jetpackRepository).setSelectedSiteAndCleanOldSites(targetSite)
        }
    }

    @Test
    fun `when login requires two factor, then explicit status is returned without selection`() = testBlocking {
        doReturn(
            WPComLoginRepository.LoginResult.TwoFactorRequired("user-id", "nonce", emptyList())
        ).whenever(loginRepository).login(USERNAME, PASSWORD)

        assertFailureWithoutSelection(AutoLoginStatus.AUTH_REQUIRES_2FA)
        verifyNoInteractions(accountRepository)
    }

    @Test
    fun `when authentication fails, then auth failure is returned without selection`() = testBlocking {
        doReturn(
            WPComLoginRepository.LoginResult.Error(AuthenticationError(INVALID_TOKEN, "fixed error"))
        ).whenever(loginRepository).login(USERNAME, PASSWORD)

        assertFailureWithoutSelection(AutoLoginStatus.AUTH_FAILED)
        verifyNoInteractions(accountRepository)
    }

    @Test
    fun `when account fetch fails, then auth failure is returned without selection`() = testBlocking {
        doReturn(Result.failure<Unit>(IllegalStateException("fixed error")))
            .whenever(accountRepository)
            .fetchUserAccount()

        assertFailureWithoutSelection(AutoLoginStatus.AUTH_FAILED)
        verify(jetpackRepository, never()).fetchJetpackSite(any())
    }

    @Test
    fun `when site fetch fails, then site failure is returned without selection`() = testBlocking {
        doReturn(Result.failure<SiteModel>(IllegalStateException("fixed error")))
            .whenever(jetpackRepository)
            .fetchJetpackSite(SITE_URL)

        assertFailureWithoutSelection(AutoLoginStatus.SITE_FAILED)
    }

    @Test
    fun `given a coherent session, when reused, then authentication is skipped and site is selected`() = testBlocking {
        hasToken = true
        account.userId = ACCOUNT_ID

        val result = strategy.login(request(), reuseExistingSession = true)

        assertThat(result).isEqualTo(WPComAutoLoginStrategy.Result.Success)
        verify(loginRepository, never()).login(any(), any())
        verify(accountRepository, never()).fetchUserAccount()
        verify(jetpackRepository).setSelectedSiteAndCleanOldSites(targetSite)
    }

    private suspend fun assertFailureWithoutSelection(expectedStatus: AutoLoginStatus) {
        assertThat(strategy.login(request(), reuseExistingSession = false))
            .isEqualTo(WPComAutoLoginStrategy.Result.Failure(expectedStatus))
        verify(jetpackRepository, never()).setSelectedSiteAndCleanOldSites(any())
    }

    private fun request() = AutoLoginRequest(
        connection = AutoLoginConnection.WPCOM,
        siteUrl = SITE_URL,
        credentials = AutoLoginCredentials(USERNAME, PASSWORD)
    )

    private companion object {
        const val SITE_URL = "https://store.example/shop"
        const val USERNAME = "merchant"
        const val PASSWORD = "application-password"
        const val ACCOUNT_ID = 42L
    }
}
