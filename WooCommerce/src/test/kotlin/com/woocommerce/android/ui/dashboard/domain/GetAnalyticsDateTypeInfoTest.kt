package com.woocommerce.android.ui.dashboard.domain

import com.woocommerce.android.tools.SelectedSite
import com.woocommerce.android.viewmodel.BaseUnitTest
import kotlinx.coroutines.ExperimentalCoroutinesApi
import org.assertj.core.api.Assertions.assertThat
import org.junit.Test
import org.mockito.kotlin.mock
import org.mockito.kotlin.whenever
import org.wordpress.android.fluxc.model.SiteModel
import org.wordpress.android.fluxc.network.rest.wpcom.wc.WooError
import org.wordpress.android.fluxc.network.rest.wpcom.wc.WooErrorType
import org.wordpress.android.fluxc.network.rest.wpcom.wc.WooResult
import org.wordpress.android.fluxc.store.WooCommerceStore

@ExperimentalCoroutinesApi
class GetAnalyticsDateTypeInfoTest : BaseUnitTest() {
    private val siteModel: SiteModel = mock {
        on { adminUrl }.thenReturn("https://example.com/wp-admin/")
    }
    private val selectedSite: SelectedSite = mock {
        on { get() }.thenReturn(siteModel)
    }
    private val wooCommerceStore: WooCommerceStore = mock()

    private val sut = GetAnalyticsDateTypeInfo(
        selectedSite = selectedSite,
        wooCommerceStore = wooCommerceStore,
    )

    @Test
    fun `when date type is date_created, then label is date created`() = testBlocking {
        whenever(wooCommerceStore.fetchDateTypeSetting(siteModel))
            .thenReturn(WooResult("date_created"))

        val result = sut()

        assertThat(result.isSuccess).isTrue()
        assertThat(result.getOrNull()?.dateTypeLabel).isEqualTo("date created")
    }

    @Test
    fun `when date type is date_completed, then label is date completed`() = testBlocking {
        whenever(wooCommerceStore.fetchDateTypeSetting(siteModel))
            .thenReturn(WooResult("date_completed"))

        val result = sut()

        assertThat(result.isSuccess).isTrue()
        assertThat(result.getOrNull()?.dateTypeLabel).isEqualTo("date completed")
    }

    @Test
    fun `when date type is date_paid, then label is date paid`() = testBlocking {
        whenever(wooCommerceStore.fetchDateTypeSetting(siteModel))
            .thenReturn(WooResult("date_paid"))

        val result = sut()

        assertThat(result.isSuccess).isTrue()
        assertThat(result.getOrNull()?.dateTypeLabel).isEqualTo("date paid")
    }

    @Test
    fun `when API fails, then falls back to date paid`() = testBlocking {
        whenever(wooCommerceStore.fetchDateTypeSetting(siteModel))
            .thenReturn(
                WooResult(
                    WooError(
                        WooErrorType.GENERIC_ERROR,
                        org.wordpress.android.fluxc.network.BaseRequest.GenericErrorType.UNKNOWN
                    )
                )
            )

        val result = sut()

        assertThat(result.isSuccess).isTrue()
        assertThat(result.getOrNull()?.dateTypeLabel).isEqualTo("date paid")
    }

    @Test
    fun `when invoked, then analytics settings url is correctly constructed`() = testBlocking {
        whenever(wooCommerceStore.fetchDateTypeSetting(siteModel))
            .thenReturn(WooResult("date_paid"))

        val result = sut()

        assertThat(result.getOrNull()?.analyticsSettingsUrl)
            .contains("page=wc-admin")
            .contains("path=%2Fanalytics%2Fsettings")
    }
}
