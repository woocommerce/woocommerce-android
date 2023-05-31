package com.woocommerce.android.ui.prefs.privacy.banner.domain

import com.woocommerce.android.util.TelephonyManagerProvider
import com.woocommerce.android.util.locale.LocaleProvider
import com.woocommerce.android.viewmodel.BaseUnitTest
import kotlinx.coroutines.ExperimentalCoroutinesApi
import org.assertj.core.api.Assertions.assertThat
import org.junit.Test
import org.mockito.kotlin.doReturn
import org.mockito.kotlin.mock
import org.mockito.kotlin.stub
import org.wordpress.android.fluxc.model.AccountModel
import org.wordpress.android.fluxc.store.AccountStore
import java.util.Locale

@OptIn(ExperimentalCoroutinesApi::class)
class IsUsersCountryGdprCompliantTest : BaseUnitTest() {

    private val accountStore: AccountStore = mock()
    private val telephonyManagerProvider: TelephonyManagerProvider = mock()
    private val localeProvider: LocaleProvider = mock()

    private val sut: IsUsersCountryGdprCompliant = IsUsersCountryGdprCompliant(
        accountStore = accountStore,
        telephonyManagerProvider = telephonyManagerProvider,
        localeProvider = localeProvider,
    )

    @Test
    fun `given user has WPCOM account, when ip country code in EU, then show the banner`() {
        // given
        accountStore.stub {
            on { hasAccessToken() } doReturn true
            on { account } doReturn AccountModel().apply { userIpCountryCode = "DE" }
        }

        // then
        assertThat(sut()).isTrue
    }

    @Test
    fun `given user has WPCOM account, when ip country code outside EU, then do not show the banner`() {
        // given
        accountStore.stub {
            on { hasAccessToken() } doReturn true
            on { account } doReturn AccountModel().apply { userIpCountryCode = "US" }
        }

        // then
        assertThat(sut()).isFalse
    }

    @Test
    fun `given user has not WPCOM account, when network carrier country code in EU, then show the banner`() {
        // given
        accountStore.stub {
            on { hasAccessToken() } doReturn false
        }
        telephonyManagerProvider.stub {
            on { getCountryCode() } doReturn "DE"
        }

        // then
        assertThat(sut()).isTrue
    }

    @Test
    fun `given user has not WPCOM account, when network carrier country code in EU and network carrier returns country code lowercase, then show the banner`() {
        // given
        accountStore.stub {
            on { hasAccessToken() } doReturn false
        }
        telephonyManagerProvider.stub {
            on { getCountryCode() } doReturn "de"
        }

        // then
        assertThat(sut()).isTrue
    }

    @Test
    fun `given user has not WPCOM account, when network carrier country code outside EU, then do not show the banner`() {
        // given
        accountStore.stub {
            on { hasAccessToken() } doReturn false
        }
        telephonyManagerProvider.stub {
            on { getCountryCode() } doReturn "US"
        }

        // then
        assertThat(sut()).isFalse
    }

    @Test
    fun `given user has not WPCOM account and no network carrier data, when locale country code in EU, then show the banner`() {
        // given
        accountStore.stub {
            on { hasAccessToken() } doReturn false
        }
        telephonyManagerProvider.stub {
            on { getCountryCode() } doReturn ""
        }
        localeProvider.stub {
            on { provideLocale() } doReturn Locale.GERMANY
        }

        // then
        assertThat(sut()).isTrue
    }

    @Test
    fun `given user has not WPCOM account and no network carrier data, when locale country code outside EU, then do not show the banner`() {
        // given
        accountStore.stub {
            on { hasAccessToken() } doReturn false
        }
        telephonyManagerProvider.stub {
            on { getCountryCode() } doReturn ""
        }
        localeProvider.stub {
            on { provideLocale() } doReturn Locale.US
        }

        // then
        assertThat(sut()).isFalse
    }

    @Test
    fun `given user has WPCOM account but it returns empty ip code, when requesting if user is gdpr compliant, fall back to phone carrier check`() {
        // given
        accountStore.stub {
            on { hasAccessToken() } doReturn true
            on { account } doReturn AccountModel().apply { userIpCountryCode = null }
        }
        telephonyManagerProvider.stub {
            on { getCountryCode() } doReturn "pl"
        }

        // then
        assertThat(sut()).isTrue
    }
}
