package com.woocommerce.android.ui.dashboard.data

import com.woocommerce.android.model.DashboardWidget
import com.woocommerce.android.tools.SelectedSite
import com.woocommerce.android.viewmodel.BaseUnitTest
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flowOf
import org.assertj.core.api.Assertions.assertThat
import org.junit.Test
import org.mockito.kotlin.doReturn
import org.mockito.kotlin.mock
import org.wordpress.android.fluxc.model.SiteModel

@OptIn(ExperimentalCoroutinesApi::class)
class ObserveInboxWidgetStatusTest : BaseUnitTest() {
    private val selectedSite: SelectedSite = mock {
        on { observe() } doReturn flowOf(SiteModel())
    }

    private val sut = ObserveInboxWidgetStatus(
        selectedSite
    )

    @Test
    fun `when observing inbox widget status, then status is Available`() = testBlocking {
        val status = sut().first()

        assertThat(status).isEqualTo(DashboardWidget.Status.Available)
    }
}
