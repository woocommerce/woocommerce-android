package com.woocommerce.android.ui.orders.wooshippinglabels.rates.datasource

import com.woocommerce.android.ui.orders.wooshippinglabels.rates.networking.ShippingRateDTO
import com.woocommerce.android.ui.orders.wooshippinglabels.rates.networking.WooShippingRatesDTO
import com.woocommerce.android.viewmodel.BaseUnitTest
import kotlinx.coroutines.ExperimentalCoroutinesApi
import org.junit.Test
import java.math.BigDecimal
import kotlin.random.Random

@OptIn(ExperimentalCoroutinesApi::class)
class WooShippingRatesDatasourceMapperTest : BaseUnitTest() {
    val sut = WooShippingRatesDatasourceMapper()
    private val packagesIds = listOf("package1", "package2", "package3")
    private val optionIds = listOf("default", "signature_required", "adult_signature_required")
    private val carriers =
        listOf("usps", "fedex", "ups", "dhlexpress", "dhlecommerce", "dhlecommerceasia", "canapost", "null")

    @Test
    fun `mapper can parse rates with 1 package 2 carriers 5 rates`() {
        val rates = generateRates(1, 2, 5)
        val result = sut(rates)

        val carriers = result.associateBy { it.defaultRate.carrier }

        assert(carriers.size == 2)
        assert(result.size == 5)
    }

    @Test
    fun `mapper can parse rates with 2 package 8 carriers 18 rates`() {
        val rates = generateRates(2, 8, 18)
        val result = sut(rates)

        val carriers = result.associateBy { it.defaultRate.carrier }

        assert(carriers.size == 5)
        assert(result.size == 18)
    }

    @Test
    fun `mapper can parse rates with 3 package 4 carriers 15 rates`() {
        val rates = generateRates(3, 4, 15)
        val result = sut(rates)

        val carriers = result.associateBy { it.defaultRate.carrier }

        assert(carriers.size == 4)
        assert(result.size == 15)
    }

    @Suppress("NestedBlockDepth")
    private fun generateRates(
        packagesNum: Int,
        carriersNum: Int,
        ratesNum: Int
    ): Map<String, Map<String, WooShippingRatesDTO>>? {
        val random = Random(6789)
        val currentPackages = packagesIds.take(packagesNum)
        val currentCarriers = carriers.take(carriersNum)

        require(packagesIds.size >= packagesNum) {
            "The number of carriers should be less than the number of options"
        }
        require(carriers.size >= carriersNum) {
            "The number of carriers should be less than the number of options"
        }
        require(carriersNum <= ratesNum) {
            "At least one carrier option should be used for the rates"
        }

        val lastPosition = ratesNum - 1
        val result = mutableMapOf<String, Map<String, WooShippingRatesDTO>>()
        currentPackages.forEach { packageId ->
            val ratesMap = mutableMapOf<String, WooShippingRatesDTO>()
            optionIds.forEach { optionId ->
                val rates = mutableListOf<ShippingRateDTO>()
                for (i in 0..lastPosition) {
                    val carrierPos = if (i < currentCarriers.size) i else random.nextInt(0, currentCarriers.size)
                    val carrier = currentCarriers[carrierPos]
                    val rateDto = ShippingRateDTO(
                        rateId = i.toString(),
                        shipmentId = i.toString(),
                        serviceId = i.toString(),
                        carrierId = carrier,
                        title = "rate $i",
                        deliveryDays = 10,
                        rate = BigDecimal.TEN,
                        retailRate = BigDecimal.TEN,
                        tracking = random.nextBoolean(),
                        freePickup = random.nextBoolean(),
                        insurance = "10.00",
                        deliveryDate = null,
                        deliveryDateGuaranteed = true,
                        isSelected = true
                    )
                    rates.add(rateDto)
                }
                ratesMap[optionId] = WooShippingRatesDTO(rates)
            }
            result[packageId] = ratesMap
        }
        return result
    }
}
