package com.woocommerce.android.model

import org.assertj.core.api.Assertions.assertThat
import org.junit.Before
import org.junit.Test
import org.wordpress.android.fluxc.model.addons.WCProductAddonModel

internal class ProductAddonTest {
    private lateinit var addonUnderTest: ProductAddon

    private val addonPriceFake = "test-price"
    private val rawOptionsFake = listOf(
        ProductAddonOption(
            priceType = WCProductAddonModel.AddOnPriceType.FlatFee,
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
            max = "test-max",
            min = "test-min",
            position = "test-position",
            adjustPrice = "test-adjust-price",
            restrictions = "test-restrictions",
            titleFormat = WCProductAddonModel.AddOnTitleFormat.Label,
            restrictionsType = WCProductAddonModel.AddOnRestrictionsType.OnlyLettersNumbers,
            priceType = WCProductAddonModel.AddOnPriceType.FlatFee,
            type = WCProductAddonModel.AddOnType.CustomPrice,
            display = WCProductAddonModel.AddOnDisplay.RadioButton,
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
