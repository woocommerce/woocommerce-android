package com.woocommerce.android.di

import com.woocommerce.android.AppPrefs
import com.woocommerce.android.WooException
import com.woocommerce.android.tools.SelectedSite
import com.woocommerce.android.ui.payments.cardreader.onboarding.PluginType
import com.woocommerce.android.util.CapturePaymentResponseMapper
import kotlinx.coroutines.test.runTest
import org.assertj.core.api.Assertions.assertThat
import org.junit.Test
import org.mockito.kotlin.mock
import org.mockito.kotlin.whenever
import org.wordpress.android.fluxc.model.SiteModel
import org.wordpress.android.fluxc.model.payments.inperson.WCConnectionTokenResult
import org.wordpress.android.fluxc.network.BaseRequest.GenericErrorType
import org.wordpress.android.fluxc.network.rest.wpcom.wc.WooError
import org.wordpress.android.fluxc.network.rest.wpcom.wc.WooErrorType
import org.wordpress.android.fluxc.network.rest.wpcom.wc.WooResult
import org.wordpress.android.fluxc.store.WCInPersonPaymentsStore
import org.wordpress.android.fluxc.store.WCInPersonPaymentsStore.InPersonPaymentsPluginType

class InPersonPaymentsModuleTest {
    private val selectedSite: SelectedSite = mock()
    private val inPersonPaymentsStore: WCInPersonPaymentsStore = mock()
    private val responseMapper: CapturePaymentResponseMapper = mock()
    private val appPrefs: AppPrefs = mock()

    @Test
    fun `given token request fails, when fetching connection token, then Woo error is thrown`() = runTest {
        // GIVEN
        val site: SiteModel = mock {
            on { id }.thenReturn(1)
            on { siteId }.thenReturn(2L)
            on { selfHostedSiteId }.thenReturn(3L)
        }
        val error = WooError(
            type = WooErrorType.AUTHORIZATION_REQUIRED,
            original = GenericErrorType.AUTHORIZATION_REQUIRED,
        )
        whenever(selectedSite.get()).thenReturn(site)
        whenever(appPrefs.getCardReaderPreferredPlugin(1, 2L, 3L)).thenReturn(PluginType.WOOCOMMERCE_PAYMENTS)
        whenever(
            inPersonPaymentsStore.fetchConnectionToken(InPersonPaymentsPluginType.WOOCOMMERCE_PAYMENTS, site)
        ).thenReturn(WooResult<WCConnectionTokenResult>(error))
        val store = InPersonPaymentsModule().provideInPersonPaymentsStore(
            selectedSite = selectedSite,
            inPersonPaymentsStore = inPersonPaymentsStore,
            responseMapper = responseMapper,
            appPrefs = appPrefs,
        )

        // WHEN
        val exception = runCatching { store.fetchConnectionToken() }.exceptionOrNull()

        // THEN
        assertThat(exception).isInstanceOf(WooException::class.java)
        assertThat((exception as WooException).error).isEqualTo(error)
    }
}
