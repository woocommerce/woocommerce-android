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
import org.mockito.kotlin.whenever
import org.wordpress.android.fluxc.model.SiteModel
import org.wordpress.android.fluxc.network.BaseRequest.GenericErrorType
import org.wordpress.android.fluxc.network.rest.wpcom.wc.WooError
import org.wordpress.android.fluxc.network.rest.wpcom.wc.WooErrorType
import org.wordpress.android.fluxc.network.rest.wpcom.wc.WooResult

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
        whenever(
            repository.saveCarrierPackage(
                savedPackageId = packageIdToSave,
                parentCarrierId = expectedCarrier.id,
                site = selectedSite.get()
            )
        ).thenReturn(WooResult(emptyList()))

        // When
        useCase(
            savePackage = true,
            packageId = packageIdToSave,
            isUserDefined = false,
            carrierPackages = carrierPackages,
        )

        // Then
        verifyBlocking(repository, times(1)) {
            saveCarrierPackage(packageIdToSave, expectedCarrier.id, selectedSite.get())
        }
        verifyBlocking(repository, never()) {
            deleteSavedCarrierPackage(any(), any(), any())
        }
    }

    @Test
    fun `When un-starring a package, then call deleteSavedCarrierPackage with correct carrierId`() = runTest {
        // Given
        val packageIdToDelete = "usps_package_123"
        val expectedCarrier = Carrier.USPS
        val carrierPackages = createDummyCarrierPackages(packageIdToDelete, expectedCarrier)
        val isUserDefined = false
        whenever(
            repository.deleteSavedCarrierPackage(
                packageId = packageIdToDelete,
                isUserDefined = isUserDefined,
                site = selectedSite.get()
            )
        ).thenReturn(WooResult(emptyList()))

        // When
        useCase(
            savePackage = false,
            packageId = packageIdToDelete,
            isUserDefined = isUserDefined,
            carrierPackages = carrierPackages,
        )

        // Then
        verifyBlocking(repository, times(1)) {
            deleteSavedCarrierPackage(packageIdToDelete, isUserDefined, selectedSite.get())
        }
        verifyBlocking(repository, never()) {
            saveCarrierPackage(any(), any(), any())
        }
    }

    @Test
    fun `given save fails, when starring a package, then return failure`() = runTest {
        // Given
        val packageIdToSave = "usps_package_123"
        val expectedCarrier = Carrier.USPS
        val carrierPackages = createDummyCarrierPackages(packageIdToSave, expectedCarrier)
        val error = WooError(type = WooErrorType.GENERIC_ERROR, original = GenericErrorType.SERVER_ERROR)
        whenever(repository.saveCarrierPackage(any(), any(), any())).thenReturn(WooResult(error = error))

        // When
        val result = useCase(
            savePackage = true,
            packageId = packageIdToSave,
            isUserDefined = false,
            carrierPackages = carrierPackages,
        )

        // Then
        assert(result.isFailure)
    }

    @Test
    fun `given delete fails, when un-starring a package, then return failure`() = runTest {
        // Given
        val packageIdToDelete = "usps_package_123"
        val isUserDefined = false
        val error = WooError(type = WooErrorType.GENERIC_ERROR, original = GenericErrorType.SERVER_ERROR)
        whenever(repository.deleteSavedCarrierPackage(any(), any(), any())).thenReturn(WooResult(error = error))

        // When
        val result = useCase(
            savePackage = false,
            packageId = packageIdToDelete,
            isUserDefined = isUserDefined,
        )

        // Then
        assert(result.isFailure)
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
