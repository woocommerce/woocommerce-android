package com.woocommerce.android.ui.orders.wooshippinglabels.rates.domain

import com.woocommerce.android.model.Address
import com.woocommerce.android.ui.orders.wooshippinglabels.models.OriginShippingAddress
import com.woocommerce.android.ui.orders.wooshippinglabels.models.WooShippingCarrier
import com.woocommerce.android.ui.orders.wooshippinglabels.packages.ui.PackageData
import com.woocommerce.android.ui.orders.wooshippinglabels.rates.datasource.WooShippingRateModel
import com.woocommerce.android.ui.orders.wooshippinglabels.rates.datasource.WooShippingRateOptionsModel
import com.woocommerce.android.ui.orders.wooshippinglabels.rates.datasource.WooShippingRatesRepository
import com.woocommerce.android.ui.orders.wooshippinglabels.rates.ui.generateShippingRates
import com.woocommerce.android.viewmodel.BaseUnitTest
import junit.framework.TestCase.assertTrue
import kotlinx.coroutines.ExperimentalCoroutinesApi
import org.junit.Test
import org.mockito.kotlin.any
import org.mockito.kotlin.doReturn
import org.mockito.kotlin.mock
import org.mockito.kotlin.never
import org.mockito.kotlin.verify
import org.mockito.kotlin.whenever
import java.math.BigDecimal

@OptIn(ExperimentalCoroutinesApi::class)
class GetShippingRatesTest : BaseUnitTest() {
    private val mapper: WooShippingRatesDomainMapper = mock()
    private val repository: WooShippingRatesRepository = mock()
    private val sut = GetShippingRates(repository, mapper)

    private val defaultSelectedPackage = PackageData(
        id = "1",
        name = "Default",
        weight = "15kg",
        dimensions = "12x12x12",
        isSelected = true,
        isLetter = true
    )

    private val defaultRates = listOf(
        WooShippingRateOptionsModel(
            mapOf(
                WooShippingRateModel.Option.DEFAULT to WooShippingRateModel(
                    carrier = WooShippingCarrier.DHL,
                    deliveryDays = (1..10).random(),
                    discount = BigDecimal.ZERO,
                    hasFreePickup = true,
                    insurance = null,
                    isTrackingEnabled = true,
                    carrierId = WooShippingCarrier.DHL.ordinal.toString(),
                    option = WooShippingRateModel.Option.DEFAULT,
                    packageId = "1",
                    price = BigDecimal.TEN,
                    rateId = "1",
                    shipmentId = "1",
                    serviceId = "1",
                    serviceName = "Default",
                    isSelected = true,
                    listRate = BigDecimal.TEN,
                    retailRate = BigDecimal.TEN,
                    deliveryDate = null,
                    isDeliveryDateGuaranteed = false
                )
            )
        )
    )

    @Test
    fun `when shipping rates request succeeds then result is a success`() = testBlocking {
        whenever(mapper(any(), any())).doReturn(generateShippingRates())
        whenever(repository.getShippingRates(any(), any(), any(), any(), any()))
            .doReturn(Result.success(defaultRates))

        val result = sut.invoke(
            orderId = 3L,
            selectedPackage = defaultSelectedPackage,
            shipTo = Address.EMPTY,
            shipFrom = OriginShippingAddress.EMPTY,
            weight = 15f,
            currencyCode = "USD"
        )

        assertTrue(result.isSuccess)
    }

    @Test
    fun `when shipping rates request fails then result is a failure`() = testBlocking {
        whenever(repository.getShippingRates(any(), any(), any(), any(), any()))
            .doReturn(Result.failure(Exception("Something fails")))

        val result = sut.invoke(
            orderId = 3L,
            selectedPackage = defaultSelectedPackage,
            shipTo = Address.EMPTY,
            shipFrom = OriginShippingAddress.EMPTY,
            weight = 15f,
            currencyCode = "USD"
        )

        assertTrue(result.isFailure)
        verify(mapper, never()).invoke(any(), any())
    }
}
