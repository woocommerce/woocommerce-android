package com.woocommerce.android.ui.prefs.privacy.banner.domain

import com.woocommerce.android.AppPrefsWrapper
import com.woocommerce.android.ui.login.AccountRepository
import com.woocommerce.android.viewmodel.BaseUnitTest
import kotlinx.coroutines.ExperimentalCoroutinesApi
import org.assertj.core.api.Assertions.assertThat
import org.junit.Test
import org.mockito.kotlin.mock
import org.mockito.kotlin.whenever

@OptIn(ExperimentalCoroutinesApi::class)
class ShouldShowPrivacyBannerTest : BaseUnitTest() {

    private val appPrefs: AppPrefsWrapper = mock()
    private val isUserGdprCompliant: IsUsersCountryGdprCompliant = mock()
    private val accountRepository: AccountRepository = mock()

    private val sut = ShouldShowPrivacyBanner(appPrefs, isUserGdprCompliant, accountRepository)

    @Test
    fun `given user is not logged in, then do not show banner`() {
        // given
        whenever(accountRepository.isUserLoggedIn()).thenReturn(false)

        // then
        assertThat(sut()).isFalse
    }

    @Test
    fun `given user has saved privacy banner settings, then do not show banner`() {
        // given
        whenever(accountRepository.isUserLoggedIn()).thenReturn(true)
        whenever(appPrefs.hasSavedPrivacyBannerSettings()).thenReturn(true)

        // then
        assertThat(sut()).isFalse
    }

    @Test
    fun `given user country is not GDPR compliant, then do not show banner`() {
        // given
        whenever(accountRepository.isUserLoggedIn()).thenReturn(true)
        whenever(appPrefs.hasSavedPrivacyBannerSettings()).thenReturn(false)
        whenever(isUserGdprCompliant()).thenReturn(false)

        // then
        assertThat(sut()).isFalse
    }

    @Test
    fun `given user is logged in, country is GDPR compliant, and has not saved privacy banner settings, then show banner`() {
        // given
        whenever(accountRepository.isUserLoggedIn()).thenReturn(true)
        whenever(isUserGdprCompliant()).thenReturn(true)
        whenever(appPrefs.hasSavedPrivacyBannerSettings()).thenReturn(false)

        // then
        assertThat(sut()).isTrue
    }
}
