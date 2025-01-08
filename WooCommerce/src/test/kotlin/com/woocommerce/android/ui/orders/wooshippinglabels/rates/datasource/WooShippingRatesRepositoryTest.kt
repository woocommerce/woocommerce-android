package com.woocommerce.android.ui.orders.wooshippinglabels.rates.datasource

import com.woocommerce.android.model.Address
import com.woocommerce.android.model.AmbiguousLocation
import com.woocommerce.android.model.Location
import com.woocommerce.android.tools.SelectedSite
import com.woocommerce.android.ui.orders.wooshippinglabels.models.OriginShippingAddress
import com.woocommerce.android.ui.orders.wooshippinglabels.packages.ui.PackageData
import com.woocommerce.android.ui.orders.wooshippinglabels.rates.networking.WooShippingRatesRestClient
import com.woocommerce.android.viewmodel.BaseUnitTest
import kotlinx.coroutines.ExperimentalCoroutinesApi
import org.mockito.kotlin.any
import org.mockito.kotlin.doReturn
import org.mockito.kotlin.mock
import org.mockito.kotlin.whenever
import org.wordpress.android.fluxc.model.SiteModel
import org.wordpress.android.fluxc.network.BaseRequest
import org.wordpress.android.fluxc.network.rest.wpcom.wc.WooError
import org.wordpress.android.fluxc.network.rest.wpcom.wc.WooErrorType
import org.wordpress.android.fluxc.network.rest.wpcom.wc.WooResult
import kotlin.test.Test

@OptIn(ExperimentalCoroutinesApi::class)
class WooShippingRatesRepositoryTest : BaseUnitTest() {
    private val currentSite = SiteModel()
    private val selectedSite: SelectedSite = mock {
        on { get() } doReturn currentSite
    }
    private val shippingRatesMapper: WooShippingRatesDatasourceMapper = mock()
    private val restClient: WooShippingRatesRestClient = mock()

    private val sut = WooShippingRatesRepository(selectedSite, shippingRatesMapper, restClient)

    private val defaultPackageData = PackageData(
        id = "1",
        name = "Package 1",
        dimensions = "10x10x10",
        weight = "10",
        isSelected = true,
        isLetter = false,
        isPredefined = true,
        dimensionUnit = "cm",
        weightUnit = "kg"
    )

    private val defaultAddress = Address(
        firstName = "",
        lastName = " ",
        company = "Company",
        phone = "",
        address1 = "Address 1",
        address2 = "",
        city = "City",
        postcode = "",
        email = "email",
        country = Location("US", "USA"),
        state = AmbiguousLocation.Defined(Location("CA", "California", "USA"))
    )

    private val defaultOriginAddress = OriginShippingAddress(
        firstName = "John",
        lastName = "Doe",
        company = "Company",
        phone = "1234567890",
        address1 = "123 Main St",
        address2 = "Apt 1",
        city = "City",
        postcode = "12345",
        email = "john.c.breckinridge@altostrat.com",
        country = "US",
        state = "CA",
        id = "1",
        isDefault = true,
        isVerified = true
    )

    @Test
    fun `when the get shipping rates request fails then return an error`() = testBlocking {
        whenever(
            restClient.getShippingRates(any(), any(), any(), any(), any())
        ) doReturn WooResult(
            WooError(
                type = WooErrorType.API_ERROR,
                original = BaseRequest.GenericErrorType.PARSE_ERROR,
                message = "Error"
            )
        )
        val result = sut.getShippingRates(1L, defaultPackageData, defaultAddress, defaultOriginAddress, 2f)

        assert(result.isFailure)
    }

    @Test
    fun `when the get shipping labels request succeed then return shipping rates`() = testBlocking {
        whenever(
            restClient.getShippingRates(any(), any(), any(), any(), any())
        ) doReturn WooResult(emptyMap())

        val result = sut.getShippingRates(1L, defaultPackageData, defaultAddress, defaultOriginAddress, 2f)

        assert(result.isSuccess)
    }
}
