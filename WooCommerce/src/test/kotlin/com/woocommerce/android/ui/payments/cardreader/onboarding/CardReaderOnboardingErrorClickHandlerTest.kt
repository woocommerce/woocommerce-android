package com.woocommerce.android.ui.payments.cardreader.onboarding

import com.woocommerce.android.tools.SelectedSite
import com.woocommerce.android.ui.common.PluginRepository
import com.woocommerce.android.ui.payments.cardreader.CardReaderTracker
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
class CardReaderOnboardingErrorClickHandlerTest : BaseUnitTest() {
    private val siteModel: SiteModel = mock()
    private val selectedSite: SelectedSite = mock {
        on { get() }.thenReturn(siteModel)
    }
    private val pluginRepository: PluginRepository = mock()
    private val cardReaderTracker: CardReaderTracker = mock()
    private val resourceProvider: ResourceProvider = mock()

    private val handler = CardReaderOnboardingErrorClickHandler(
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
                OnboardingCtaTapped.PLUGIN_INSTALL_TAPPED
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
                reason = OnboardingCtaTapped.PLUGIN_INSTALL_TAPPED,
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
                CardReaderOnboardingErrorClickHandler.Reaction.ShowErrorAndRefresh(
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
                CardReaderOnboardingErrorClickHandler.Reaction.ShowErrorAndRefresh(
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
                CardReaderOnboardingErrorClickHandler.Reaction.Refresh
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
                CardReaderOnboardingErrorClickHandler.Reaction.Refresh
            )
        }
}
