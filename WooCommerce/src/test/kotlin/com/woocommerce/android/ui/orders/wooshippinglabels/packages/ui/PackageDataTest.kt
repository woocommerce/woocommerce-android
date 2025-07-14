package com.woocommerce.android.ui.orders.wooshippinglabels.packages.ui

import org.assertj.core.api.Assertions.assertThat
import kotlin.test.Test

class PackageDataTest {

    @Test
    fun `length, width, and height are correctly set when dimensions are properly formatted`() {
        val packageData = PackageData(
            id = "1",
            name = "Test Package",
            dimensions = "10 x 20 x 30",
            weight = "5",
            isSelected = false,
            isLetter = false,
        )

        assertThat(packageData.length).isEqualTo("10")
        assertThat(packageData.width).isEqualTo("20")
        assertThat(packageData.height).isEqualTo("30")
    }

    @Test
    fun `length, width, and height are empty when dimensions are not properly formatted`() {
        val packageData = PackageData(
            id = "1",
            name = "Test Package",
            dimensions = "10 x 20",
            weight = "5",
            isSelected = false,
            isLetter = false,
        )

        assertThat(packageData.length).isEqualTo("10")
        assertThat(packageData.width).isEqualTo("20")
        assertThat(packageData.height).isEmpty()
    }

    @Test
    fun `length, width, and height are empty when dimensions are null`() {
        val packageData = PackageData(
            id = "1",
            name = "Test Package",
            dimensions = "10",
            weight = "5",
            isSelected = false,
            isLetter = false,
        )

        assertThat(packageData.length).isEqualTo("10")
        assertThat(packageData.width).isEmpty()
        assertThat(packageData.height).isEmpty()
    }

    @Test
    fun `length, width, and height are empty when dimensions are empty`() {
        val packageData = PackageData(
            id = "1",
            name = "Test Package",
            dimensions = "",
            weight = "5",
            isSelected = false,
            isLetter = false,
        )

        assertThat(packageData.length).isEmpty()
        assertThat(packageData.width).isEmpty()
        assertThat(packageData.height).isEmpty()
    }
}
