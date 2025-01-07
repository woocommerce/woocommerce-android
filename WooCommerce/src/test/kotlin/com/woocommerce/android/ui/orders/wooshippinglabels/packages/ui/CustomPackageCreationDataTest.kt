package com.woocommerce.android.ui.orders.wooshippinglabels.packages.ui

import com.woocommerce.android.ui.orders.wooshippinglabels.packages.WooShippingLabelPackageCreationViewModel.PackageType
import org.assertj.core.api.Assertions.assertThat
import org.junit.Test

class CustomPackageCreationDataTest {

    @Test
    fun `isValid returns true when all fields are filled and saveAsTemplate is false`() {
        val data = CustomPackageCreationData(
            type = PackageType.BOX,
            length = "10",
            width = "10",
            height = "10",
            saveAsTemplate = false
        )

        assertThat(data.isValid).isTrue
    }

    @Test
    fun `isValid returns false when any dimension field is empty`() {
        val data = CustomPackageCreationData(
            type = PackageType.BOX,
            length = "",
            width = "10",
            height = "10",
            saveAsTemplate = false
        )

        assertThat(data.isValid).isFalse
    }

    @Test
    fun `isValid returns false when saveAsTemplate is true and name or weight is empty`() {
        val data = CustomPackageCreationData(
            type = PackageType.BOX,
            length = "10",
            width = "10",
            height = "10",
            saveAsTemplate = true,
            name = "",
            weight = "1"
        )

        assertThat(data.isValid).isFalse
    }

    @Test
    fun `isValid returns true when saveAsTemplate is true and name and weight are filled`() {
        val data = CustomPackageCreationData(
            type = PackageType.BOX,
            length = "10",
            width = "10",
            height = "10",
            saveAsTemplate = true,
            name = "Package",
            weight = "1"
        )

        assertThat(data.isValid).isTrue
    }

    @Test
    fun `toPackageData returns PackageData with correct values`() {
        val data = CustomPackageCreationData(
            type = PackageType.BOX,
            length = "10",
            width = "10",
            height = "10",
            saveAsTemplate = false
        )

        val packageData = data.toPackageData("cm")

        assertThat(packageData.name).isEqualTo("")
        assertThat(packageData.dimensions).isEqualTo("10 x 10 x 10")
        assertThat(packageData.isSelected).isTrue
        assertThat(packageData.isLetter).isFalse
    }

    @Test
    fun `toPackageData returns PackageData with correct values for envelope type`() {
        val data = CustomPackageCreationData(
            type = PackageType.ENVELOPE,
            length = "10",
            width = "10",
            height = "10",
            saveAsTemplate = false
        )

        val packageData = data.toPackageData("cm")

        assertThat(packageData.name).isEqualTo("")
        assertThat(packageData.dimensions).isEqualTo("10 x 10 x 10")
        assertThat(packageData.isSelected).isTrue
        assertThat(packageData.isLetter).isTrue
    }
}
