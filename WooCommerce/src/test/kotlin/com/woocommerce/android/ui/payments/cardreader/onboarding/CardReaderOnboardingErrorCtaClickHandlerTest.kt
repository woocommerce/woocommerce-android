package com.woocommerce.android.ui.payments.cardreader.onboarding

import com.woocommerce.android.tools.SelectedSite
import com.woocommerce.android.ui.common.PluginRepository
import com.woocommerce.android.ui.payments.tracking.CardReaderTracker
import com.woocommerce.android.viewmodel.BaseUnitTest
import com.woocommerce.android.viewmodel.ResourceProvider
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.flowOf
import org.assertj.core.api.Assertions.assertThat
import org.junit.Test
import org.mockito.kotlin.mock
import org.mockito.kotlin.verify
import org.mockito.kotlin.whenever
import org.wordpress.android.fluxc.model.SiteModel

@OptIn(ExperimentalCoroutinesApi::class)
class CardReaderOnboardingErrorCtaClickHandlerTest : BaseUnitTest() {
    private val siteModel: SiteModel = mock()
    private val selectedSite: SelectedSite = mock {
        on { get() }.thenReturn(siteModel)
    }
    private val pluginRepository: PluginRepository = mock()
    private val cardReaderTracker: CardReaderTracker = mock()
    private val resourceProvider: ResourceProvider = mock()

    private val handler = CardReaderOnboardingErrorCtaClickHandler(
        selectedSite,
        pluginRepository,
        cardReaderTracker,
        resourceProvider,
    )

    @Test
    fun `when invoked with WC_PAY_NOT_INSTALLED, then event tracked with reason`() =
        testBlocking {
            // GIVEN
            whenever(
                pluginRepository.installPlugin(
                    site = siteModel,
                    slug = "woocommerce-payments",
                    name = "woocommerce-payments/woocommerce-payments",
                )
            ).thenReturn(
                flowOf(
                    PluginRepository.PluginStatus.PluginInstalled(
                        slug = "slug"
                    )
                )
            )

            // WHEN
            handler(CardReaderOnboardingCTAErrorType.WC_PAY_NOT_INSTALLED)

            // THEN
            verify(cardReaderTracker).trackOnboardingCtaTapped(
                OnboardingCtaReasonTapped.PLUGIN_INSTALL_TAPPED
            )
        }

    @Test
    fun `when invoked with WC_PAY_NOT_ACTIVATED, then event tracked with reason`() =
        testBlocking {
            // GIVEN
            whenever(
                pluginRepository.installPlugin(
                    site = siteModel,
                    slug = "woocommerce-payments",
                    name = "woocommerce-payments/woocommerce-payments",
                )
            ).thenReturn(
                flowOf(
                    PluginRepository.PluginStatus.PluginInstalled(
                        slug = "slug"
                    )
                )
            )

            // WHEN
            handler(CardReaderOnboardingCTAErrorType.WC_PAY_NOT_ACTIVATED)

            // THEN
            verify(cardReaderTracker).trackOnboardingCtaTapped(
                OnboardingCtaReasonTapped.PLUGIN_ACTIVATE_TAPPED
            )
        }

    @Test
    fun `given error plugin installation ,when invoked with WC_PAY_NOT_INSTALLED, then event tracked with reason`() =
        testBlocking {
            // GIVEN
            whenever(
                pluginRepository.installPlugin(
                    site = siteModel,
                    slug = "woocommerce-payments",
                    name = "woocommerce-payments/woocommerce-payments",
                )
            ).thenReturn(
                flowOf(
                    PluginRepository.PluginStatus.PluginInstallFailed(
                        errorDescription = "errorDescription",
                        errorType = "errorType",
                        errorCode = null,
                    )
                )
            )

            // WHEN
            handler(CardReaderOnboardingCTAErrorType.WC_PAY_NOT_INSTALLED)

            // THEN
            verify(cardReaderTracker).trackOnboardingCtaFailed(
                reason = OnboardingCtaReasonTapped.PLUGIN_INSTALL_TAPPED,
                description = "errorDescription"
            )
        }

    @Test
    fun `given error plugin installation ,when invoked with WC_PAY_NOT_ACTIVATED, then event tracked with reason`() =
        testBlocking {
            // GIVEN
            whenever(
                pluginRepository.installPlugin(
                    site = siteModel,
                    slug = "woocommerce-payments",
                    name = "woocommerce-payments/woocommerce-payments",
                )
            ).thenReturn(
                flowOf(
                    PluginRepository.PluginStatus.PluginActivationFailed(
                        errorDescription = "errorDescription",
                        errorType = "errorType",
                        errorCode = null,
                    )
                )
            )

            // WHEN
            handler(CardReaderOnboardingCTAErrorType.WC_PAY_NOT_ACTIVATED)

            // THEN
            verify(cardReaderTracker).trackOnboardingCtaFailed(
                reason = OnboardingCtaReasonTapped.PLUGIN_ACTIVATE_TAPPED,
                description = "errorDescription"
            )
        }

    @Test
    fun `given installPlugin failed with PluginInstallFailed, when invoked with WC_PAY_NOT_INSTALLED, then error with description returned`() =
        testBlocking {
            // GIVEN
            val errorDescription = "errorDescription"
            val errorType = "errorType"
            whenever(
                pluginRepository.installPlugin(
                    site = siteModel,
                    slug = "woocommerce-payments",
                    name = "woocommerce-payments/woocommerce-payments",
                )
            ).thenReturn(
                flowOf(
                    PluginRepository.PluginStatus.PluginInstallFailed(
                        errorDescription = errorDescription,
                        errorType = errorType,
                        errorCode = null,
                    )
                )
            )

            // WHEN
            val result = handler(CardReaderOnboardingCTAErrorType.WC_PAY_NOT_INSTALLED)

            // THEN
            assertThat(result).isEqualTo(
                CardReaderOnboardingErrorCtaClickHandler.Reaction.ShowErrorAndRefresh(
                    message = errorDescription
                )
            )
        }

    @Test
    fun `given installPlugin failed with PluginInstallFailed, when invoked with WC_PAY_NOT_ACTIVATED, then error with description returned`() =
        testBlocking {
            // GIVEN
            val errorDescription = "errorDescription"
            val errorType = "errorType"
            whenever(
                pluginRepository.installPlugin(
                    site = siteModel,
                    slug = "woocommerce-payments",
                    name = "woocommerce-payments/woocommerce-payments",
                )
            ).thenReturn(
                flowOf(
                    PluginRepository.PluginStatus.PluginInstallFailed(
                        errorDescription = errorDescription,
                        errorType = errorType,
                        errorCode = null,
                    )
                )
            )

            // WHEN
            val result = handler(CardReaderOnboardingCTAErrorType.WC_PAY_NOT_ACTIVATED)

            // THEN
            assertThat(result).isEqualTo(
                CardReaderOnboardingErrorCtaClickHandler.Reaction.ShowErrorAndRefresh(
                    message = errorDescription
                )
            )
        }

    @Test
    fun `given installPlugin failed with PluginActivationFailed, when invoked with WC_PAY_NOT_INSTALLED, then error with description returned`() =
        testBlocking {
            // GIVEN
            val errorDescription = "errorDescription"
            val errorType = "errorType"
            whenever(
                pluginRepository.installPlugin(
                    site = siteModel,
                    slug = "woocommerce-payments",
                    name = "woocommerce-payments/woocommerce-payments",
                )
            ).thenReturn(
                flowOf(
                    PluginRepository.PluginStatus.PluginActivationFailed(
                        errorDescription = errorDescription,
                        errorType = errorType,
                        errorCode = null,
                    )
                )
            )

            // WHEN
            val result = handler(CardReaderOnboardingCTAErrorType.WC_PAY_NOT_INSTALLED)

            // THEN
            assertThat(result).isEqualTo(
                CardReaderOnboardingErrorCtaClickHandler.Reaction.ShowErrorAndRefresh(
                    message = errorDescription
                )
            )
        }

    @Test
    fun `given installPlugin failed with PluginActivationFailed, when invoked with WC_PAY_NOT_ACTIVATED, then error with description returned`() =
        testBlocking {
            // GIVEN
            val errorDescription = "errorDescription"
            val errorType = "errorType"
            whenever(
                pluginRepository.installPlugin(
                    site = siteModel,
                    slug = "woocommerce-payments",
                    name = "woocommerce-payments/woocommerce-payments",
                )
            ).thenReturn(
                flowOf(
                    PluginRepository.PluginStatus.PluginActivationFailed(
                        errorDescription = errorDescription,
                        errorType = errorType,
                        errorCode = null,
                    )
                )
            )

            // WHEN
            val result = handler(CardReaderOnboardingCTAErrorType.WC_PAY_NOT_ACTIVATED)

            // THEN
            assertThat(result).isEqualTo(
                CardReaderOnboardingErrorCtaClickHandler.Reaction.ShowErrorAndRefresh(
                    message = errorDescription
                )
            )
        }

    @Test
    fun `given installPlugin failed with PluginActivated, when invoked with WC_PAY_NOT_INSTALLED, then Refresh returned`() =
        testBlocking {
            // GIVEN
            whenever(
                pluginRepository.installPlugin(
                    site = siteModel,
                    slug = "woocommerce-payments",
                    name = "woocommerce-payments/woocommerce-payments",
                )
            ).thenReturn(
                flowOf(
                    PluginRepository.PluginStatus.PluginActivated(
                        name = "name"
                    )
                )
            )

            // WHEN
            val result = handler(CardReaderOnboardingCTAErrorType.WC_PAY_NOT_INSTALLED)

            // THEN
            assertThat(result).isEqualTo(
                CardReaderOnboardingErrorCtaClickHandler.Reaction.Refresh
            )
        }

    @Test
    fun `given installPlugin failed with PluginInstalled, when invoked with WC_PAY_NOT_INSTALLED, then Refresh returned`() =
        testBlocking {
            // GIVEN
            whenever(
                pluginRepository.installPlugin(
                    site = siteModel,
                    slug = "woocommerce-payments",
                    name = "woocommerce-payments/woocommerce-payments",
                )
            ).thenReturn(
                flowOf(
                    PluginRepository.PluginStatus.PluginInstalled(
                        slug = "slug"
                    )
                )
            )

            // WHEN
            val result = handler(CardReaderOnboardingCTAErrorType.WC_PAY_NOT_INSTALLED)

            // THEN
            assertThat(result).isEqualTo(
                CardReaderOnboardingErrorCtaClickHandler.Reaction.Refresh
            )
        }

    @Test
    fun `given installPlugin failed with PluginInstalled, when invoked with WC_PAY_NOT_ACTIVATED, then Refresh returned`() =
        testBlocking {
            // GIVEN
            whenever(
                pluginRepository.installPlugin(
                    site = siteModel,
                    slug = "woocommerce-payments",
                    name = "woocommerce-payments/woocommerce-payments",
                )
            ).thenReturn(
                flowOf(
                    PluginRepository.PluginStatus.PluginInstalled(
                        slug = "slug"
                    )
                )
            )

            // WHEN
            val result = handler(CardReaderOnboardingCTAErrorType.WC_PAY_NOT_ACTIVATED)

            // THEN
            assertThat(result).isEqualTo(
                CardReaderOnboardingErrorCtaClickHandler.Reaction.Refresh
            )
        }

    @Test
    fun `given wpcom site, when invoked with WC_PAY_NOT_SETUP, then OpenWpComWebView returned`() =
        testBlocking {
            // GIVEN
            whenever(siteModel.isWPCom).thenReturn(true)
            val adminUrl = "mywebsite.com"
            whenever(siteModel.adminUrl).thenReturn(adminUrl)

            // WHEN
            val result = handler(CardReaderOnboardingCTAErrorType.WC_PAY_NOT_SETUP)

            // THEN
            assertThat(result).isEqualTo(
                CardReaderOnboardingErrorCtaClickHandler.Reaction.OpenWpComWebView(
                    url = "$adminUrl/admin.php?page=wc-admin&path=%2Fpayments%2Foverview"
                )
            )
        }

    @Test
    fun `given wpcomatomic site, when invoked with WC_PAY_NOT_SETUP, then OpenWpComWebView returned`() =
        testBlocking {
            // GIVEN
            whenever(siteModel.isWPComAtomic).thenReturn(true)
            val adminUrl = "mywebsite.com"
            whenever(siteModel.adminUrl).thenReturn(adminUrl)

            // WHEN
            val result = handler(CardReaderOnboardingCTAErrorType.WC_PAY_NOT_SETUP)

            // THEN
            assertThat(result).isEqualTo(
                CardReaderOnboardingErrorCtaClickHandler.Reaction.OpenWpComWebView(
                    url = "$adminUrl/admin.php?page=wc-admin&path=%2Fpayments%2Foverview"
                )
            )
        }

    @Test
    fun `given non wpcom site, when invoked with WC_PAY_NOT_SETUP, then OpenGenericWebView returned`() =
        testBlocking {
            // GIVEN
            whenever(siteModel.isWPCom).thenReturn(false)
            whenever(siteModel.isWPComAtomic).thenReturn(false)
            val adminUrl = "mywebsite.com"
            whenever(siteModel.adminUrl).thenReturn(adminUrl)

            // WHEN
            val result = handler(CardReaderOnboardingCTAErrorType.WC_PAY_NOT_SETUP)

            // THEN
            assertThat(result).isEqualTo(
                CardReaderOnboardingErrorCtaClickHandler.Reaction.OpenGenericWebView(
                    url = "$adminUrl/admin.php?page=wc-admin&path=%2Fpayments%2Foverview"
                )
            )
        }

    @Test
    fun `when invoked with WC_PAY_NOT_SETUP, then event tracked with reason`() =
        testBlocking {
            // GIVEN
            val adminUrl = "mywebsite.com"
            whenever(siteModel.adminUrl).thenReturn(adminUrl)

            // WHEN
            handler(CardReaderOnboardingCTAErrorType.WC_PAY_NOT_SETUP)

            // THEN
            verify(cardReaderTracker).trackOnboardingCtaTapped(
                OnboardingCtaReasonTapped.PLUGIN_SETUP_TAPPED
            )
        }

    @Test
    fun `given wpcom site, when invoked with STRIPE_ACCOUNT_OVERDUE_REQUIREMENTS, then OpenWpComWebView returned`() =
        testBlocking {
            // GIVEN
            whenever(siteModel.isWPCom).thenReturn(true)
            val adminUrl = "mywebsite.com"
            whenever(siteModel.adminUrl).thenReturn(adminUrl)

            // WHEN
            val result = handler(CardReaderOnboardingCTAErrorType.STRIPE_ACCOUNT_OVERDUE_REQUIREMENTS)

            // THEN
            assertThat(result).isEqualTo(
                CardReaderOnboardingErrorCtaClickHandler.Reaction.OpenWpComWebView(
                    url = "$adminUrl/admin.php?page=wc-admin&path=%2Fpayments%2Foverview"
                )
            )
        }

    @Test
    fun `given wpcom atomic site, when invoked with STRIPE_ACCOUNT_OVERDUE_REQUIREMENTS, then OpenWpComWebView returned`() =
        testBlocking {
            // GIVEN
            whenever(siteModel.isWPComAtomic).thenReturn(true)
            val adminUrl = "mywebsite.com"
            whenever(siteModel.adminUrl).thenReturn(adminUrl)

            // WHEN
            val result = handler(CardReaderOnboardingCTAErrorType.STRIPE_ACCOUNT_OVERDUE_REQUIREMENTS)

            // THEN
            assertThat(result).isEqualTo(
                CardReaderOnboardingErrorCtaClickHandler.Reaction.OpenWpComWebView(
                    url = "$adminUrl/admin.php?page=wc-admin&path=%2Fpayments%2Foverview"
                )
            )
        }

    @Test
    fun `given non wpcom site, when invoked with STRIPE_ACCOUNT_OVERDUE_REQUIREMENTS, then OpenGenericWebView returned`() =
        testBlocking {
            // GIVEN
            whenever(siteModel.isWPCom).thenReturn(false)
            whenever(siteModel.isWPComAtomic).thenReturn(false)
            val adminUrl = "mywebsite.com"
            whenever(siteModel.adminUrl).thenReturn(adminUrl)

            // WHEN
            val result = handler(CardReaderOnboardingCTAErrorType.STRIPE_ACCOUNT_OVERDUE_REQUIREMENTS)

            // THEN
            assertThat(result).isEqualTo(
                CardReaderOnboardingErrorCtaClickHandler.Reaction.OpenGenericWebView(
                    url = "$adminUrl/admin.php?page=wc-admin&path=%2Fpayments%2Foverview"
                )
            )
        }

    @Test
    fun `when invoked with STRIPE_ACCOUNT_OVERDUE_REQUIREMENTS, then event tracked with reason`() =
        testBlocking {
            // GIVEN
            val adminUrl = "mywebsite.com"
            whenever(siteModel.adminUrl).thenReturn(adminUrl)

            // WHEN
            handler(CardReaderOnboardingCTAErrorType.STRIPE_ACCOUNT_OVERDUE_REQUIREMENTS)

            // THEN
            verify(cardReaderTracker).trackOnboardingCtaTapped(
                OnboardingCtaReasonTapped.STRIPE_ACCOUNT_SETUP_TAPPED
            )
        }
}
