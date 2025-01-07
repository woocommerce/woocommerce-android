package com.woocommerce.android.ui.orders.wooshippinglabels.rates.domain

import com.woocommerce.android.ui.orders.wooshippinglabels.models.WooShippingCarrier
import com.woocommerce.android.ui.orders.wooshippinglabels.rates.datasource.WooShippingRateModel
import com.woocommerce.android.ui.orders.wooshippinglabels.rates.datasource.WooShippingRateOptionsModel
import com.woocommerce.android.util.CurrencyFormatter
import com.woocommerce.android.viewmodel.BaseUnitTest
import com.woocommerce.android.viewmodel.ResourceProvider
import junit.framework.TestCase.assertEquals
import kotlinx.coroutines.ExperimentalCoroutinesApi
import org.junit.Test
import org.mockito.kotlin.any
import org.mockito.kotlin.anyOrNull
import org.mockito.kotlin.mock
import java.math.BigDecimal

@OptIn(ExperimentalCoroutinesApi::class)
class WooShippingRatesDomainMapperTest : BaseUnitTest() {
    private val currencyFormatter: CurrencyFormatter = mock {
        on(it.formatCurrency(any<BigDecimal>(), any(), any())).thenAnswer { i -> "${i.arguments[1]}${i.arguments[0]}" }
    }
    private val resourceProvider: ResourceProvider = mock {
        on(it.getString(any(), any())).thenAnswer { i -> "formatted ${i.arguments[1]}" }
        on(it.getString(any())).thenAnswer { i -> "formatted ${i.arguments[0]}" }
        on(it.getQuantityString(any(), anyOrNull(), anyOrNull(), anyOrNull()))
            .thenAnswer { i -> "formatted ${i.arguments[0]}" }
    }

    private val sut = WooShippingRatesDomainMapper(
        resourceProvider = resourceProvider,
        currencyFormatter = currencyFormatter
    )

    @Test
    fun `mapping rates generates the expected number of carriers and rates with 3 carriers and 15 rates`() {
        val carriersNum = 3
        val ratesNum = 15

        val rates = generateRates(carriersNum, ratesNum)
        val mappedRates = sut(rates, "USD")

        assertEquals(mappedRates.keys.size, carriersNum)
        assertEquals(mappedRates.values.sumOf { it.size }, ratesNum)
    }

    @Test
    fun `mapping rates generates the expected number of carriers and rates with 1 carriers and 1 rates`() {
        val carriersNum = 1
        val ratesNum = 1

        val rates = generateRates(carriersNum, ratesNum)
        val mappedRates = sut(rates, "USD")

        assertEquals(mappedRates.keys.size, carriersNum)
        assertEquals(mappedRates.values.sumOf { it.size }, ratesNum)
    }

    @Test
    fun `mapping rates generates the expected number of carriers and rates with 5 carriers and 10 rates`() {
        val carriersNum = 5
        val ratesNum = 10

        val rates = generateRates(carriersNum, ratesNum)
        val mappedRates = sut(rates, "USD")

        assertEquals(mappedRates.keys.size, carriersNum)
        assertEquals(mappedRates.values.sumOf { it.size }, ratesNum)
    }

    @Test
    fun `when the shipping rate contain a null insurance then is NOT included in the shipping rate`() {
        val rates = listOf(
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
        val mappedRates = sut(rates, "USD")

        val rate = mappedRates.values.first().first()

        assertEquals(2, rate.defaultRate.shippingRateOptions.size)
    }

    @Test
    fun `when the shipping rate contains insurance then IS included in the shipping rate`() {
        val rates = listOf(
            WooShippingRateOptionsModel(
                mapOf(
                    WooShippingRateModel.Option.DEFAULT to WooShippingRateModel(
                        carrier = WooShippingCarrier.DHL,
                        deliveryDays = (1..10).random(),
                        discount = BigDecimal.ZERO,
                        hasFreePickup = true,
                        insurance = BigDecimal.TEN,
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
        val mappedRates = sut(rates, "USD")

        val rate = mappedRates.values.first().first()

        assertEquals(3, rate.defaultRate.shippingRateOptions.size)
    }

    @Test
    fun `when isTrackingEnabled is false then isTrackingEnabled is NOT included in the shipping rate`() {
        val rates = listOf(
            WooShippingRateOptionsModel(
                mapOf(
                    WooShippingRateModel.Option.DEFAULT to WooShippingRateModel(
                        carrier = WooShippingCarrier.DHL,
                        deliveryDays = (1..10).random(),
                        discount = BigDecimal.ZERO,
                        hasFreePickup = true,
                        insurance = BigDecimal.TEN,
                        isTrackingEnabled = false,
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
        val mappedRates = sut(rates, "USD")

        val rate = mappedRates.values.first().first()

        assertEquals(2, rate.defaultRate.shippingRateOptions.size)
    }

    @Test
    fun `when isTrackingEnabled is false then isTrackingEnabled is included in the shipping rate`() {
        val rates = listOf(
            WooShippingRateOptionsModel(
                mapOf(
                    WooShippingRateModel.Option.DEFAULT to WooShippingRateModel(
                        carrier = WooShippingCarrier.DHL,
                        deliveryDays = (1..10).random(),
                        discount = BigDecimal.ZERO,
                        hasFreePickup = true,
                        insurance = BigDecimal.TEN,
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
        val mappedRates = sut(rates, "USD")

        val rate = mappedRates.values.first().first()

        assertEquals(3, rate.defaultRate.shippingRateOptions.size)
    }

    @Test
    fun `when hasFreePickup is false then isTrackingEnabled is NOT included in the shipping rate`() {
        val rates = listOf(
            WooShippingRateOptionsModel(
                mapOf(
                    WooShippingRateModel.Option.DEFAULT to WooShippingRateModel(
                        carrier = WooShippingCarrier.DHL,
                        deliveryDays = (1..10).random(),
                        discount = BigDecimal.ZERO,
                        hasFreePickup = false,
                        insurance = BigDecimal.TEN,
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
        val mappedRates = sut(rates, "USD")

        val rate = mappedRates.values.first().first()

        assertEquals(2, rate.defaultRate.shippingRateOptions.size)
    }

    @Test
    fun `when hasFreePickup is false then isTrackingEnabled is included in the shipping rate`() {
        val rates = listOf(
            WooShippingRateOptionsModel(
                mapOf(
                    WooShippingRateModel.Option.DEFAULT to WooShippingRateModel(
                        carrier = WooShippingCarrier.DHL,
                        deliveryDays = (1..10).random(),
                        discount = BigDecimal.ZERO,
                        hasFreePickup = true,
                        insurance = BigDecimal.TEN,
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
        val mappedRates = sut(rates, "USD")

        val rate = mappedRates.values.first().first()

        assertEquals(3, rate.defaultRate.shippingRateOptions.size)
    }

    @Suppress("LongMethod")
    private fun generateRates(carriersNum: Int, ratesNum: Int): List<WooShippingRateOptionsModel> {
        require(WooShippingCarrier.entries.size >= carriersNum) {
            "The number of carriers should be less than the number of options"
        }
        require(carriersNum <= ratesNum) {
            "At least one carrier option should be used for the rates"
        }
        val carriers = WooShippingCarrier.entries.take(carriersNum)
        val rates = mutableListOf<WooShippingRateOptionsModel>()
        val lastPosition = ratesNum - 1
        for (i in 0..lastPosition) {
            val ratesMap = mutableMapOf<WooShippingRateModel.Option, WooShippingRateModel>()
            val carrier = if (i < carriersNum) carriers[i] else carriers.random()
            ratesMap[WooShippingRateModel.Option.DEFAULT] = WooShippingRateModel(
                carrier = carrier,
                deliveryDays = (1..10).random(),
                discount = BigDecimal.ZERO,
                hasFreePickup = true,
                insurance = BigDecimal.TEN,
                isTrackingEnabled = true,
                carrierId = carrier.ordinal.toString(),
                option = WooShippingRateModel.Option.DEFAULT,
                packageId = i.toString(),
                price = BigDecimal.TEN,
                rateId = i.toString(),
                shipmentId = i.toString(),
                serviceId = i.toString(),
                serviceName = "Default",
                isSelected = true,
                listRate = BigDecimal.TEN,
                retailRate = BigDecimal.TEN,
                deliveryDate = null,
                isDeliveryDateGuaranteed = false
            )

            ratesMap[WooShippingRateModel.Option.SIGNATURE] = WooShippingRateModel(
                carrier = carrier,
                deliveryDays = (1..10).random(),
                discount = BigDecimal.ZERO,
                hasFreePickup = true,
                insurance = BigDecimal.TEN,
                isTrackingEnabled = true,
                carrierId = carrier.ordinal.toString(),
                option = WooShippingRateModel.Option.SIGNATURE,
                packageId = i.toString(),
                price = BigDecimal.TEN,
                rateId = i.toString(),
                shipmentId = i.toString(),
                serviceId = i.toString(),
                serviceName = "Signature Required",
                isSelected = true,
                listRate = BigDecimal.TEN,
                retailRate = BigDecimal.TEN,
                deliveryDate = null,
                isDeliveryDateGuaranteed = false
            )

            ratesMap[WooShippingRateModel.Option.ADULT_SIGNATURE] = WooShippingRateModel(
                carrier = carrier,
                deliveryDays = (1..10).random(),
                discount = BigDecimal.ZERO,
                hasFreePickup = true,
                insurance = BigDecimal.TEN,
                isTrackingEnabled = true,
                carrierId = carrier.ordinal.toString(),
                option = WooShippingRateModel.Option.ADULT_SIGNATURE,
                packageId = i.toString(),
                price = BigDecimal.TEN,
                rateId = i.toString(),
                shipmentId = i.toString(),
                serviceId = i.toString(),
                serviceName = "Adult Signature Required",
                isSelected = true,
                listRate = BigDecimal.TEN,
                retailRate = BigDecimal.TEN,
                deliveryDate = null,
                isDeliveryDateGuaranteed = false
            )
            rates.add(WooShippingRateOptionsModel(ratesMap))
        }
        return rates
    }
}
