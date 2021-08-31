package com.woocommerce.android.model

import com.woocommerce.android.model.ProductAddon.*
import org.assertj.core.api.Assertions.assertThat
import org.junit.Before
import org.junit.Test

internal class ProductAddonTest {
    private lateinit var addonUnderTest: ProductAddon

    private val addonPriceFake = "test-price"
    private val rawOptionsFake = listOf(
        ProductAddonOption(
            priceType = PriceType.FlatFee,
            label = "test-option-label",
            price = "test-option-price",
            image = "test-option-image"
        )
    )

    @Before
    fun setUp() {
        addonUnderTest = ProductAddon(
            name = "test-name",
            required = true,
            description = "test-description",
            descriptionEnabled = true,
            max = 0,
            min = 0,
            position = 0,
            adjustPrice = false,
            titleFormat = TitleFormat.Label,
            restrictionsType = Restrictions.OnlyLettersNumbers,
            priceType = PriceType.FlatFee,
            type = Type.CustomPrice,
            display = Display.RadioButton,
            price = addonPriceFake,
            rawOptions = rawOptionsFake
        )
    }

    @Test
    fun `options should replace empty price raw option data with addon data correctly`() {
        addonUnderTest = addonUnderTest.copy(
            rawOptions = listOf(
                ProductAddonOption(
                    priceType = null,
                    label = "",
                    price = "",
                    image = ""
                )
            )
        )

        val priceSafeOptionList = addonUnderTest.options

        assertThat(priceSafeOptionList.isNotEmpty())
        assertThat(priceSafeOptionList.size).isEqualTo(1)

        val priceSafeOption = addonUnderTest.options.first()

        assertThat(priceSafeOption).isNotNull
        assertThat(priceSafeOption.priceType).isEqualTo(addonUnderTest.priceType)
        assertThat(priceSafeOption.price).isEqualTo(addonPriceFake)
    }

    @Test
    fun `options should return existent raw option list when option data is available`() {
        val priceSafeOptionList = addonUnderTest.options

        assertThat(priceSafeOptionList).isNotEmpty
        assertThat(priceSafeOptionList).isEqualTo(rawOptionsFake)
    }
}
