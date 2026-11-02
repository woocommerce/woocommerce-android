package com.woocommerce.android.ui.orders.wooshippinglabels.servicestatus

import com.woocommerce.android.WooException
import com.woocommerce.android.tools.SelectedSite
import com.woocommerce.android.ui.orders.wooshippinglabels.networking.WooShippingLabelRepository
import com.woocommerce.android.viewmodel.BaseUnitTest
import kotlinx.coroutines.ExperimentalCoroutinesApi
import org.assertj.core.api.Assertions.assertThat
import org.junit.Test
import org.mockito.kotlin.doReturn
import org.mockito.kotlin.mock
import org.mockito.kotlin.whenever
import org.wordpress.android.fluxc.model.SiteModel
import org.wordpress.android.fluxc.network.BaseRequest.GenericErrorType
import org.wordpress.android.fluxc.network.rest.wpcom.wc.WooError
import org.wordpress.android.fluxc.network.rest.wpcom.wc.WooErrorType
import org.wordpress.android.fluxc.network.rest.wpcom.wc.WooResult

@OptIn(ExperimentalCoroutinesApi::class)
class FetchServiceStatusNoticesTest : BaseUnitTest() {
    private val site = SiteModel()
    private val selectedSite: SelectedSite = mock {
        on { getOrNull() } doReturn site
    }
    private val repository: WooShippingLabelRepository = mock()
    private val sut = FetchServiceStatusNotices(shippingRepository = repository, selectedSite = selectedSite)

    @Test
    fun `when service status request succeeds, then return the notices`() = testBlocking {
        val notices = listOf(
            ServiceStatusNotice(
                id = "1",
                severity = ServiceStatusNotice.Severity.ERROR,
                message = "USPS outage",
                isDismissible = false,
                actionUrl = null
            )
        )
        whenever(repository.fetchServiceStatus(site)).doReturn(WooResult(notices))

        val result = sut()

        assertThat(result.getOrNull()).isEqualTo(notices)
    }

    @Test
    fun `when service status request returns a 500, then return a failure`() = testBlocking {
        val error = WooError(
            type = WooErrorType.API_ERROR,
            original = GenericErrorType.SERVER_ERROR,
            message = "Unable to fetch service status.",
            apiErrorCode = "service_status_error"
        )
        whenever(repository.fetchServiceStatus(site)).doReturn(WooResult(error))

        val result = sut()

        assertThat(result.isFailure).isTrue()
        assertThat((result.exceptionOrNull() as WooException).error).isEqualTo(error)
    }

    @Test
    fun `given no site is selected, when fetching service status, then return a failure`() = testBlocking {
        whenever(selectedSite.getOrNull()).doReturn(null)

        assertThat(sut().isFailure).isTrue()
    }
}
