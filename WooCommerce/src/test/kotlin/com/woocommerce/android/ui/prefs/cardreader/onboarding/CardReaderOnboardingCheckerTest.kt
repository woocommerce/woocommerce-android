package com.woocommerce.android.ui.prefs.cardreader.onboarding

import com.woocommerce.android.AppPrefsWrapper
import org.mockito.kotlin.*
import com.woocommerce.android.tools.NetworkStatus
import com.woocommerce.android.tools.SelectedSite
import com.woocommerce.android.viewmodel.BaseUnitTest
import kotlinx.coroutines.ExperimentalCoroutinesApi
import org.assertj.core.api.Assertions.assertThat
import org.junit.Before
import org.junit.Test
import org.mockito.ArgumentMatchers.anyInt
import org.mockito.ArgumentMatchers.anyLong
import org.wordpress.android.fluxc.model.SiteModel
import org.wordpress.android.fluxc.model.pay.WCPaymentAccountResult
import org.wordpress.android.fluxc.network.rest.wpcom.wc.WooResult
import org.wordpress.android.fluxc.persistence.WCPluginSqlUtils
import org.wordpress.android.fluxc.store.WCPayStore
import org.wordpress.android.fluxc.store.WooCommerceStore

@ExperimentalCoroutinesApi
class CardReaderOnboardingCheckerTest : BaseUnitTest() {
    private lateinit var checker: CardReaderOnboardingChecker

    private val selectedSite: SelectedSite = mock()
    private val wooStore: WooCommerceStore = mock()
    private val wcPayStore: WCPayStore = mock()
    private val networkStatus: NetworkStatus = mock()
    private val appPrefsWrapper: AppPrefsWrapper = mock()

    private val site = SiteModel()

    @Before
    fun setUp() = testBlocking {
        checker = CardReaderOnboardingChecker(
            selectedSite,
            appPrefsWrapper,
            wooStore,
            wcPayStore,
            coroutinesTestRule.testDispatchers,
            networkStatus
        )
        whenever(networkStatus.isConnected()).thenReturn(true)
        whenever(selectedSite.get()).thenReturn(site)
        whenever(wooStore.getStoreCountryCode(site)).thenReturn("US")
        whenever(wcPayStore.loadAccount(site)).thenReturn(buildPaymentAccountResult())
        whenever(wooStore.fetchSitePlugins(site)).thenReturn(WooResult(listOf()))
        whenever(wooStore.getSitePlugin(site, WooCommerceStore.WooPlugin.WOO_PAYMENTS))
            .thenReturn(buildWCPayPluginInfo())
    }

    @Test
    fun `when not connected to network, then NO_CONNECTION returned`() = testBlocking {
        whenever(networkStatus.isConnected()).thenReturn(false)

        val result = checker.getOnboardingState()

        assertThat(result).isInstanceOf(CardReaderOnboardingState.NoConnectionError::class.java)
    }

    @Test
    fun `when connected to network, then NO_CONNECTION not returned`() = testBlocking {
        whenever(networkStatus.isConnected()).thenReturn(true)

        val result = checker.getOnboardingState()

        assertThat(result).isNotInstanceOf(CardReaderOnboardingState.NoConnectionError::class.java)
    }

    @Test
    fun `when store country not supported, then STORE_COUNTRY_NOT_SUPPORTED returned`() = testBlocking {
        whenever(wooStore.getStoreCountryCode(site)).thenReturn("unsupported country abc")

        val result = checker.getOnboardingState()

        assertThat(result).isInstanceOf(CardReaderOnboardingState.StoreCountryNotSupported::class.java)
    }

    @Test
    fun `when store country supported, then STORE_COUNTRY_NOT_SUPPORTED not returned`() = testBlocking {
        whenever(wooStore.getStoreCountryCode(site)).thenReturn("US")

        val result = checker.getOnboardingState()

        assertThat(result).isNotInstanceOf(CardReaderOnboardingState.StoreCountryNotSupported::class.java)
    }

    @Test
    fun `given country in lower case, when store country supported, then STORE_COUNTRY_NOT_SUPPORTED not returned`() =
        testBlocking {
            whenever(wooStore.getStoreCountryCode(site)).thenReturn("us")

            val result = checker.getOnboardingState()

            assertThat(result).isNotInstanceOf(CardReaderOnboardingState.StoreCountryNotSupported::class.java)
        }

    @Test
    fun `when country code is not found, then STORE_COUNTRY_NOT_SUPPORTED returned`() =
        testBlocking {
            whenever(wooStore.getStoreCountryCode(site)).thenReturn(null)

            val result = checker.getOnboardingState()

            assertThat(result).isInstanceOf(CardReaderOnboardingState.StoreCountryNotSupported::class.java)
        }

    @Test
    fun `given country not supported, then stripe account loading does not even start`() =
        testBlocking {
            whenever(wooStore.getStoreCountryCode(site)).thenReturn("unsupported country abc")

            checker.getOnboardingState()

            verify(wcPayStore, never()).loadAccount(anyOrNull())
        }

    @Test
    fun `when account country not supported, then STRIPE_COUNTRY_NOT_SUPPORTED returned`() = testBlocking {
        whenever(wcPayStore.loadAccount(site)).thenReturn(
            buildPaymentAccountResult(
                countryCode = "unsupported country abc"
            )
        )

        val result = checker.getOnboardingState()

        assertThat(result).isInstanceOf(CardReaderOnboardingState.StripeAccountCountryNotSupported::class.java)
    }

    @Test
    fun `when account country supported, then ACCOUNT_COUNTRY_NOT_SUPPORTED not returned`() = testBlocking {
        whenever(wcPayStore.loadAccount(site)).thenReturn(
            buildPaymentAccountResult(
                countryCode = "US"
            )
        )

        val result = checker.getOnboardingState()

        assertThat(result).isNotInstanceOf(CardReaderOnboardingState.StripeAccountCountryNotSupported::class.java)
    }

    @Test
    fun `given country in lower case, when country supported, then ACCOUNT_COUNTRY_NOT_SUPPORTED not returned`() =
        testBlocking {
            whenever(wcPayStore.loadAccount(site)).thenReturn(
                buildPaymentAccountResult(
                    countryCode = "us"
                )
            )

            val result = checker.getOnboardingState()

            assertThat(result).isNotInstanceOf(CardReaderOnboardingState.StripeAccountCountryNotSupported::class.java)
        }

    @Test
    fun `when woocommerce payments plugin not installed, then WCPAY_NOT_INSTALLED returned`() =
        testBlocking {
            whenever(wooStore.fetchSitePlugins(site)).thenReturn(WooResult(listOf()))
            whenever(wooStore.getSitePlugin(site, WooCommerceStore.WooPlugin.WOO_PAYMENTS)).thenReturn(null)

            val result = checker.getOnboardingState()

            assertThat(result).isEqualTo(CardReaderOnboardingState.WcpayNotInstalled)
        }

    @Test
    fun `when woocommerce payments plugin outdated, then WCPAY_UNSUPPORTED_VERSION returned`() =
        testBlocking {
            whenever(wooStore.fetchSitePlugins(site)).thenReturn(WooResult(listOf()))
            whenever(wooStore.getSitePlugin(site, WooCommerceStore.WooPlugin.WOO_PAYMENTS))
                .thenReturn(buildWCPayPluginInfo(version = "2.8.1"))

            val result = checker.getOnboardingState()

            assertThat(result).isEqualTo(CardReaderOnboardingState.WcpayUnsupportedVersion)
        }

    @Test
    fun `when woocommerce payments plugin not active, then WCPAY_NOT_ACTIVATED returned`() =
        testBlocking {
            whenever(wooStore.fetchSitePlugins(site)).thenReturn(WooResult(listOf()))
            whenever(wooStore.getSitePlugin(site, WooCommerceStore.WooPlugin.WOO_PAYMENTS))
                .thenReturn(buildWCPayPluginInfo(isActive = false))

            val result = checker.getOnboardingState()

            assertThat(result).isEqualTo(CardReaderOnboardingState.WcpayNotActivated)
        }

    @Test
    fun `when stripe account not connected, then WCPAY_SETUP_NOT_COMPLETED returned`() =
        testBlocking {
            whenever(wcPayStore.loadAccount(site)).thenReturn(
                buildPaymentAccountResult(
                    WCPaymentAccountResult.WCPayAccountStatusEnum.NO_ACCOUNT
                )
            )

            val result = checker.getOnboardingState()

            assertThat(result).isEqualTo(CardReaderOnboardingState.WcpaySetupNotCompleted)
        }

    @Test
    fun `when stripe account under review, then WCPAY_SETUP_NOT_COMPLETED returned`() =
        testBlocking {
            whenever(wcPayStore.loadAccount(site)).thenReturn(
                buildPaymentAccountResult(
                    WCPaymentAccountResult.WCPayAccountStatusEnum.RESTRICTED,
                    hasPendingRequirements = false,
                    hadOverdueRequirements = false
                )
            )

            val result = checker.getOnboardingState()

            assertThat(result).isEqualTo(CardReaderOnboardingState.StripeAccountUnderReview)
        }

    @Test
    fun `when stripe account pending requirements, then STRIPE_ACCOUNT_PENDING_REQUIREMENT returned`() =
        testBlocking {
            whenever(wcPayStore.loadAccount(site)).thenReturn(
                buildPaymentAccountResult(
                    WCPaymentAccountResult.WCPayAccountStatusEnum.RESTRICTED,
                    hasPendingRequirements = true,
                    hadOverdueRequirements = false
                )
            )

            val result = checker.getOnboardingState()

            assertThat(result).isInstanceOf(CardReaderOnboardingState.StripeAccountPendingRequirement::class.java)
        }

    @Test
    fun `when stripe account restricted soon, then STRIPE_ACCOUNT_PENDING_REQUIREMENT returned`() =
        testBlocking {
            whenever(wcPayStore.loadAccount(site)).thenReturn(
                buildPaymentAccountResult(
                    WCPaymentAccountResult.WCPayAccountStatusEnum.RESTRICTED_SOON,
                    hasPendingRequirements = false,
                    hadOverdueRequirements = false
                )
            )

            val result = checker.getOnboardingState()

            assertThat(result).isInstanceOf(CardReaderOnboardingState.StripeAccountPendingRequirement::class.java)
        }

    @Test
    fun `when stripe account has overdue requirements, then STRIPE_ACCOUNT_OVERDUE_REQUIREMENT returned`() =
        testBlocking {
            whenever(wcPayStore.loadAccount(site)).thenReturn(
                buildPaymentAccountResult(
                    WCPaymentAccountResult.WCPayAccountStatusEnum.RESTRICTED,
                    hasPendingRequirements = false,
                    hadOverdueRequirements = true
                )
            )

            val result = checker.getOnboardingState()

            assertThat(result).isEqualTo(CardReaderOnboardingState.StripeAccountOverdueRequirement)
        }

    @Test
    fun `when stripe account has both pending and overdue requirements, then OVERDUE_REQUIREMENT returned`() =
        testBlocking {
            whenever(wcPayStore.loadAccount(site)).thenReturn(
                buildPaymentAccountResult(
                    WCPaymentAccountResult.WCPayAccountStatusEnum.RESTRICTED,
                    hasPendingRequirements = true,
                    hadOverdueRequirements = true
                )
            )

            val result = checker.getOnboardingState()

            assertThat(result).isEqualTo(CardReaderOnboardingState.StripeAccountOverdueRequirement)
        }

    @Test
    fun `when stripe account marked as fraud, then STRIPE_ACCOUNT_REJECTED returned`() =
        testBlocking {
            whenever(wcPayStore.loadAccount(site)).thenReturn(
                buildPaymentAccountResult(
                    WCPaymentAccountResult.WCPayAccountStatusEnum.REJECTED_FRAUD,
                )
            )

            val result = checker.getOnboardingState()

            assertThat(result).isEqualTo(CardReaderOnboardingState.StripeAccountRejected)
        }

    @Test
    fun `when stripe account listed, then STRIPE_ACCOUNT_REJECTED returned`() =
        testBlocking {
            whenever(wcPayStore.loadAccount(site)).thenReturn(
                buildPaymentAccountResult(
                    WCPaymentAccountResult.WCPayAccountStatusEnum.REJECTED_LISTED,
                )
            )

            val result = checker.getOnboardingState()

            assertThat(result).isEqualTo(CardReaderOnboardingState.StripeAccountRejected)
        }

    @Test
    fun `when stripe account violates terms of service, then STRIPE_ACCOUNT_REJECTED returned`() =
        testBlocking {
            whenever(wcPayStore.loadAccount(site)).thenReturn(
                buildPaymentAccountResult(
                    WCPaymentAccountResult.WCPayAccountStatusEnum.REJECTED_TERMS_OF_SERVICE,
                )
            )

            val result = checker.getOnboardingState()

            assertThat(result).isEqualTo(CardReaderOnboardingState.StripeAccountRejected)
        }

    @Test
    fun `when stripe account rejected for other reasons, then STRIPE_ACCOUNT_REJECTED returned`() =
        testBlocking {
            whenever(wcPayStore.loadAccount(site)).thenReturn(
                buildPaymentAccountResult(
                    WCPaymentAccountResult.WCPayAccountStatusEnum.REJECTED_OTHER,
                )
            )

            val result = checker.getOnboardingState()

            assertThat(result).isEqualTo(CardReaderOnboardingState.StripeAccountRejected)
        }

    @Test
    fun `when test mode enabled on site with live account, then WcpayInTestModeWithLiveStripeAccount returned`() =
        testBlocking {
            whenever(wcPayStore.loadAccount(site)).thenReturn(
                buildPaymentAccountResult(liveAccount = true, testModeEnabled = true)
            )

            val result = checker.getOnboardingState()

            assertThat(result).isEqualTo(CardReaderOnboardingState.WcpayInTestModeWithLiveStripeAccount)
        }

    @Test
    fun `when test mode disabled on site with live account, then WcpayInTestModeWithLiveStripeAccount NOT returned`() =
        testBlocking {
            whenever(wcPayStore.loadAccount(site)).thenReturn(
                buildPaymentAccountResult(liveAccount = true, testModeEnabled = false)
            )

            val result = checker.getOnboardingState()

            assertThat(result).isNotEqualTo(CardReaderOnboardingState.WcpayInTestModeWithLiveStripeAccount)
        }

    @Test
    fun `when test mode disabled on site with test account, then WcpayInTestModeWithLiveStripeAccount NOT returned`() =
        testBlocking {
            whenever(wcPayStore.loadAccount(site)).thenReturn(
                buildPaymentAccountResult(liveAccount = false, testModeEnabled = false)
            )

            val result = checker.getOnboardingState()

            assertThat(result).isNotEqualTo(CardReaderOnboardingState.WcpayInTestModeWithLiveStripeAccount)
        }

    @Test
    fun `when test mode enabled on site with test account, then WcpayInTestModeWithLiveStripeAccount NOT returned`() =
        testBlocking {
            whenever(wcPayStore.loadAccount(site)).thenReturn(
                buildPaymentAccountResult(liveAccount = false, testModeEnabled = true)
            )

            val result = checker.getOnboardingState()

            assertThat(result).isNotEqualTo(CardReaderOnboardingState.WcpayInTestModeWithLiveStripeAccount)
        }

    @Test
    fun `when test mode flag not supported, then WcpayInTestModeWithLiveStripeAccount NOT returned`() =
        testBlocking {
            whenever(wcPayStore.loadAccount(site)).thenReturn(
                buildPaymentAccountResult(testModeEnabled = null)
            )

            val result = checker.getOnboardingState()

            assertThat(result).isNotEqualTo(CardReaderOnboardingState.WcpayInTestModeWithLiveStripeAccount)
        }

    @Test
    fun `when onboarding completed, then onboarding completed flag saved`() = testBlocking {
        checker.getOnboardingState()

        verify(appPrefsWrapper).setCardReaderOnboardingCompleted(anyInt(), anyLong(), anyLong())
    }

    @Test
    fun `when onboarding NOT completed, then onboarding completed NOT saved`() = testBlocking {
        whenever(wcPayStore.loadAccount(site)).thenReturn(
            buildPaymentAccountResult(
                WCPaymentAccountResult.WCPayAccountStatusEnum.REJECTED_TERMS_OF_SERVICE,
            )
        )
        checker.getOnboardingState()

        verify(appPrefsWrapper, never()).setCardReaderOnboardingCompleted(anyInt(), anyLong(), anyLong())
    }

    private fun buildPaymentAccountResult(
        status: WCPaymentAccountResult.WCPayAccountStatusEnum = WCPaymentAccountResult.WCPayAccountStatusEnum.COMPLETE,
        hasPendingRequirements: Boolean = false,
        hadOverdueRequirements: Boolean = false,
        liveAccount: Boolean = true,
        testModeEnabled: Boolean? = false,
        countryCode: String = "US",
    ) = WooResult(
        WCPaymentAccountResult(
            status,
            hasPendingRequirements = hasPendingRequirements,
            hasOverdueRequirements = hadOverdueRequirements,
            currentDeadline = null,
            statementDescriptor = "",
            storeCurrencies = WCPaymentAccountResult.WCPayAccountStatusEnum.StoreCurrencies("", listOf()),
            country = countryCode,
            isCardPresentEligible = true,
            isLive = liveAccount,
            testMode = testModeEnabled
        )
    )

    private fun buildWCPayPluginInfo(
        isActive: Boolean = true,
        version: String = SUPPORTED_WCPAY_VERSION
    ) = WCPluginSqlUtils.WCPluginModel(1, 1, isActive, "", "", version)
}
