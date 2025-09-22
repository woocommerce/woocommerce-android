package com.woocommerce.android.ui.dashboard.data

import com.woocommerce.android.ciab.CIABAffectedFeature
import com.woocommerce.android.ciab.CIABSiteGateKeeper
import com.woocommerce.android.model.DashboardWidget
import com.woocommerce.android.tools.SelectedSite
import com.woocommerce.android.viewmodel.BaseUnitTest
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flowOf
import org.assertj.core.api.Assertions.assertThat
import org.junit.Test
import org.mockito.kotlin.any
import org.mockito.kotlin.doReturn
import org.mockito.kotlin.given
import org.mockito.kotlin.mock
import org.wordpress.android.fluxc.model.SiteModel

@OptIn(ExperimentalCoroutinesApi::class)
class ObserveStockWidgetStatusTest : BaseUnitTest() {
    private val ciabSiteGateKeeper: CIABSiteGateKeeper = mock {
        on { isFeatureUnsupported(any()) } doReturn false
    }
    private val observePublishedProductsCount: ObservePublishedProductsCount = mock()
    private val selectedSite: SelectedSite = mock {
        on { observe() } doReturn flowOf(SiteModel())
    }

    private val sut = ObserveStockWidgetStatus(
        selectedSite,
        observePublishedProductsCount,
        ciabSiteGateKeeper
    )

    @Test
    fun `given CIAB feature unsupported, when observing stock widget status, then status is Hidden`() = testBlocking {
        given(ciabSiteGateKeeper.isFeatureUnsupported(CIABAffectedFeature.ProductsStockDashboardCard))
            .willReturn(true)

        val status = sut().first()

        assertThat(status).isEqualTo(DashboardWidget.Status.Hidden)
    }

    @Test
    fun `given CIAB feature supported and has published products, when observing stock widget status, then status is Available`() =
        testBlocking {
            given(ciabSiteGateKeeper.isFeatureUnsupported(CIABAffectedFeature.ProductsStockDashboardCard))
                .willReturn(false)
            given(observePublishedProductsCount()).willReturn(flowOf(true))

            val status = sut().first()

            assertThat(status).isEqualTo(DashboardWidget.Status.Available)
        }

    @Test
    fun `given CIAB feature supported and has no published products, when observing stock widget status, then status is Unavailable`(): Unit =
        testBlocking {
            given(ciabSiteGateKeeper.isFeatureUnsupported(CIABAffectedFeature.ProductsStockDashboardCard))
                .willReturn(false)
            given(observePublishedProductsCount()).willReturn(flowOf(false))

            val status = sut().first()

            assertThat(status).isEqualTo(
                DashboardWidget.Status.Unavailable(
                    badgeText = com.woocommerce.android.R.string.my_store_widget_unavailable
                )
            )
        }
}
