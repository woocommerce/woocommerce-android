package com.woocommerce.android.ui.orders.wooshippinglabels.packages

import com.woocommerce.android.tools.SelectedSite
import com.woocommerce.android.ui.orders.wooshippinglabels.packages.datasource.WooShippingLabelPackageRepository
import com.woocommerce.android.ui.orders.wooshippinglabels.packages.ui.Carrier
import com.woocommerce.android.ui.orders.wooshippinglabels.packages.ui.Carrier.DHL
import com.woocommerce.android.ui.orders.wooshippinglabels.packages.ui.CarrierPackageGroup
import com.woocommerce.android.ui.orders.wooshippinglabels.packages.ui.PackageData
import com.woocommerce.android.viewmodel.BaseUnitTest
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.runTest
import org.junit.Test
import org.mockito.Mockito.mock
import org.mockito.Mockito.never
import org.mockito.Mockito.times
import org.mockito.kotlin.any
import org.mockito.kotlin.doReturn
import org.mockito.kotlin.verifyBlocking
import org.wordpress.android.fluxc.model.SiteModel

@OptIn(ExperimentalCoroutinesApi::class)
class UpdateSavedCarrierPackagesTest : BaseUnitTest() {
    private val repository: WooShippingLabelPackageRepository = mock()
    private val selectedSite: SelectedSite = org.mockito.kotlin.mock {
        on { get() } doReturn SiteModel().apply {
            url = "https://example.com"
        }
    }
    private val useCase = UpdateSavedCarrierPackages(repository, selectedSite)

    @Test
    fun `When starring a package, then call saveCarrierPackage with correct carrierId`() = runTest {
        // Given
        val packageIdToSave = "usps_package_123"
        val expectedCarrier = Carrier.USPS
        val carrierPackages = createDummyCarrierPackages(packageIdToSave, expectedCarrier)

        // When
        useCase(
            savePackage = true,
            packageId = packageIdToSave,
            carrierPackages = carrierPackages,
        )

        // Then
        verifyBlocking(repository, times(1)) {
            saveCarrierPackage(packageIdToSave, expectedCarrier.id, selectedSite.get())
        }
        verifyBlocking(repository, never()) {
            deleteSavedCarrierPackage(any(), any())
        }
    }

    @Test
    fun `When un-starring a package, then call deleteSavedCarrierPackage with correct carrierId`() = runTest {
        // Given
        val packageIdToDelete = "usps_package_123"
        val expectedCarrier = Carrier.USPS
        val carrierPackages = createDummyCarrierPackages(packageIdToDelete, expectedCarrier)

        // When
        useCase(
            savePackage = false,
            packageId = packageIdToDelete,
            carrierPackages = carrierPackages,
        )

        // Then
        verifyBlocking(repository, times(1)) {
            deleteSavedCarrierPackage(packageIdToDelete, selectedSite.get())
        }
        verifyBlocking(repository, never()) {
            saveCarrierPackage(any(), any(), any())
        }
    }

    private fun createDummyCarrierPackages(
        targetPackageId: String,
        targetCarrier: Carrier
    ): Map<Carrier, List<CarrierPackageGroup>> {
        val packageData = PackageData(
            id = targetPackageId,
            name = "Test Package",
            dimensions = "1x1x1",
            weight = "1kg",
            isSelected = false,
            isLetter = false,
        )
        return mapOf(
            targetCarrier to listOf(
                CarrierPackageGroup(
                    groupName = "Test Group",
                    packages = listOf(packageData)
                )
            ),
            DHL to listOf(
                CarrierPackageGroup(
                    groupName = "DHL Express",
                    packages = listOf(
                        PackageData(
                            id = "dhl_pkg",
                            name = "DHL Pkg",
                            dimensions = "",
                            weight = "",
                            isSelected = false,
                            isLetter = false,
                        )
                    )
                )
            )
        )
    }
}
