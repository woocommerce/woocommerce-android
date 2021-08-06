package com.woocommerce.android.model

import org.assertj.core.api.Assertions.assertThat
import org.junit.Before
import org.junit.Test
import org.wordpress.android.fluxc.model.addons.WCProductAddonModel

internal class ProductAddonTest {
    private lateinit var addonUnderTest: ProductAddon

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
            price = "test-price",
            adjustPrice = "test-adjust-price",
            restrictions = "test-restrictions",
            titleFormat = WCProductAddonModel.AddOnTitleFormat.Label,
            restrictionsType = WCProductAddonModel.AddOnRestrictionsType.OnlyLettersNumbers,
            priceType = WCProductAddonModel.AddOnPriceType.FlatFee,
            type = WCProductAddonModel.AddOnType.CustomPrice,
            display = WCProductAddonModel.AddOnDisplay.RadioButton,
            rawOptions = listOf(
                ProductAddonOption(
                    priceType = WCProductAddonModel.AddOnPriceType.FlatFee,
                    label = "test-option-label",
                    price = "test-option-price",
                    image = "test-option-image"
                )
            )
        )
    }

    @Test
    fun `priceSafeOptionList should replace empty price option data with addon data correctly`() {
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

        assertThat(addonUnderTest.rawOptions.isNotEmpty())

        val priceSafeOptionList = addonUnderTest.options

        assertThat(priceSafeOptionList.isNotEmpty())
        assertThat(priceSafeOptionList.size).isEqualTo(1)

        val priceSafeOption = addonUnderTest.options.first()

        assertThat(priceSafeOption).isNotNull
        assertThat(priceSafeOption.priceType).isEqualTo(addonUnderTest.priceType)
        assertThat(priceSafeOption.price).isEqualTo(addonUnderTest.price)
    }

    @Test
    fun `priceSafeOptionList should return existent option list when option data is available`() {
        assertThat(addonUnderTest.rawOptions.isNotEmpty())

        val priceSafeOptionList = addonUnderTest.options

        assertThat(priceSafeOptionList.isNotEmpty())
        assertThat(priceSafeOptionList).isEqualTo(addonUnderTest.rawOptions)
    }
}
