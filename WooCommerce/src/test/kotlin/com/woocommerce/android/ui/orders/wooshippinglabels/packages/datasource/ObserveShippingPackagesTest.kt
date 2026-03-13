package com.woocommerce.android.ui.orders.wooshippinglabels.packages.datasource

import com.woocommerce.android.tools.SelectedSite
import com.woocommerce.android.ui.orders.wooshippinglabels.packages.WooShippingLabelPackageCreationViewModel.PackagesState
import com.woocommerce.android.ui.orders.wooshippinglabels.packages.ui.Carrier
import com.woocommerce.android.ui.orders.wooshippinglabels.packages.ui.CarrierPackageGroup
import com.woocommerce.android.ui.orders.wooshippinglabels.packages.ui.PackageData
import com.woocommerce.android.viewmodel.BaseUnitTest
import junit.framework.TestCase.assertTrue
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.toList
import org.assertj.core.api.Assertions.assertThat
import org.junit.Test
import org.mockito.kotlin.mock
import org.mockito.kotlin.whenever
import org.wordpress.android.fluxc.model.LocalOrRemoteId
import org.wordpress.android.fluxc.model.SiteModel
import org.wordpress.android.fluxc.persistence.entity.WooShippingPackagesEntity

@OptIn(ExperimentalCoroutinesApi::class)
class ObserveShippingPackagesTest : BaseUnitTest() {

    private val packageRepository: WooShippingLabelPackageRepository = mock()
    private val fetchShippingPackages: FetchShippingPackages = mock()
    private val selectedSite: SelectedSite = mock()
    private val observeShippingPackages = ObserveShippingPackages(
        selectedSite,
        packageRepository,
        fetchShippingPackages
    )

    @Test
    fun `when invoke, then return Data with carrier and saved packages`() = testBlocking {
        val shippingPackages = generatePackagesData()
        val site = SiteModel().apply { id = 1 }
        whenever(selectedSite.get()).thenReturn(site)
        whenever(packageRepository.observeShippingPackages(site)).thenReturn(flowOf(shippingPackages))

        val result = observeShippingPackages().first() as PackagesState.Data

        assertThat(result.savedPackages).containsExactly(
            PackageData(
                id = "1",
                name = "Saved Package 1",
                dimensions = "dimensions",
                weight = "weight",
                isSelected = false,
                isLetter = false,
                isStarred = true,
            ),
            PackageData(
                id = "2",
                name = "Saved Package 2",
                dimensions = "dimensions",
                weight = "weight",
                isSelected = false,
                isLetter = false,
                isStarred = true,
            )
        )
        assertThat(result.carrierPackages[Carrier.USPS]).containsExactly(
            CarrierPackageGroup(
                groupName = "Group 1",
                packages = listOf(
                    PackageData(
                        id = "1",
                        name = "Carrier Package 1",
                        dimensions = "dimensions",
                        weight = "weight",
                        isSelected = false,
                        isLetter = false,
                    )
                )
            )
        )
    }

    @Test
    fun `when fetchAllStorePackages returns error and no cache, then return Error`() = testBlocking {
        val error = Exception("error")
        val site = SiteModel().apply { id = 1 }
        whenever(selectedSite.get()).thenReturn(site)
        whenever(packageRepository.observeShippingPackages(site)).thenReturn(flowOf(null))
        whenever(fetchShippingPackages()).thenReturn(Result.failure(error))

        val result = observeShippingPackages().first()

        assertThat(result).isEqualTo(PackagesState.Error(error.message!!))
    }

    @Test
    fun `when starting, then refresh data once`() = testBlocking {
        val site = SiteModel().apply { id = 1 }
        val shippingPackages = generatePackagesData()
        whenever(selectedSite.get()).thenReturn(site)
        whenever(packageRepository.observeShippingPackages(site)).thenReturn(flowOf(shippingPackages))
        whenever(fetchShippingPackages()).thenReturn(Result.success(shippingPackages))

        val result = observeShippingPackages().toList()

        assertTrue(result.size == 1)
    }

    @Test
    fun `when invoke with fedex carrier packages, then return fedex carrier packages`() = testBlocking {
        val shippingPackages = generatePackagesData().copy(
            carrierPackageGroups = listOf(
                WooShippingPackagesEntity.CarrierPackageGroups(
                    carrierType = WooShippingPackagesEntity.CarrierType.FEDEX,
                    packageGroups = listOf(
                        WooShippingPackagesEntity.CarrierPackageGroup(
                            description = "FedEx Express Packages",
                            packages = listOf(
                                WooShippingPackagesEntity.Package(
                                    id = "FedExPak",
                                    name = "Large Pak",
                                    dimensions = "39.37 x 30.48 x 1.9",
                                    weight = "0",
                                    isLetter = true,
                                    dimensionUnit = "cm",
                                    weightUnit = "kg",
                                    saved = false
                                )
                            )
                        )
                    )
                )
            )
        )
        val site = SiteModel().apply { id = 1 }
        whenever(selectedSite.get()).thenReturn(site)
        whenever(packageRepository.observeShippingPackages(site)).thenReturn(flowOf(shippingPackages))

        val result = observeShippingPackages().first() as PackagesState.Data

        assertThat(result.carrierPackages[Carrier.FEDEX]).containsExactly(
            CarrierPackageGroup(
                groupName = "FedEx Express Packages",
                packages = listOf(
                    PackageData(
                        id = "FedExPak",
                        name = "Large Pak",
                        dimensions = "39.37 x 30.48 x 1.9",
                        weight = "0",
                        isSelected = false,
                        isLetter = true,
                    )
                )
            )
        )
    }

    private fun generatePackagesData() = WooShippingPackagesEntity(
        localSiteId = LocalOrRemoteId.LocalId(1),
        storeOptions = WooShippingPackagesEntity.StoreOptions(
            currencySymbol = "$",
            dimensionUnit = "cm",
            weightUnit = "kg",
            originCountry = "US"
        ),
        savedPackages = listOf(
            WooShippingPackagesEntity.Package(
                id = "1",
                name = "Saved Package 1",
                dimensions = "dimensions",
                weight = "weight",
                isLetter = false,
                dimensionUnit = "cm",
                weightUnit = "kg",
                saved = true
            ),
            WooShippingPackagesEntity.Package(
                id = "2",
                name = "Saved Package 2",
                dimensions = "dimensions",
                weight = "weight",
                isLetter = false,
                dimensionUnit = "cm",
                weightUnit = "kg",
                saved = true
            )
        ),
        carrierPackageGroups = listOf(
            WooShippingPackagesEntity.CarrierPackageGroups(
                carrierType = WooShippingPackagesEntity.CarrierType.USPS,
                packageGroups = listOf(
                    WooShippingPackagesEntity.CarrierPackageGroup(
                        description = "Group 1",
                        packages = listOf(
                            WooShippingPackagesEntity.Package(
                                id = "1",
                                name = "Carrier Package 1",
                                dimensions = "dimensions",
                                weight = "weight",
                                isLetter = false,
                                dimensionUnit = "cm",
                                weightUnit = "kg",
                                saved = false
                            )
                        )
                    )
                )
            )
        )
    )
}
