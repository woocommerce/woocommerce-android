package com.woocommerce.android.ui.orders.wooshippinglabels.packages.datasource

import com.woocommerce.android.tools.SelectedSite
import com.woocommerce.android.ui.orders.wooshippinglabels.packages.WooShippingLabelPackageCreationViewModel.PackagesState
import com.woocommerce.android.ui.orders.wooshippinglabels.packages.ui.Carrier
import com.woocommerce.android.ui.orders.wooshippinglabels.packages.ui.CarrierPackageGroup
import com.woocommerce.android.ui.orders.wooshippinglabels.packages.ui.PackageData
import com.woocommerce.android.viewmodel.BaseUnitTest
import kotlinx.coroutines.ExperimentalCoroutinesApi
import org.assertj.core.api.Assertions.assertThat
import org.junit.Test
import org.mockito.kotlin.mock
import org.mockito.kotlin.whenever
import org.wordpress.android.fluxc.model.SiteModel
import org.wordpress.android.fluxc.network.BaseRequest
import org.wordpress.android.fluxc.network.rest.wpcom.wc.WooError
import org.wordpress.android.fluxc.network.rest.wpcom.wc.WooErrorType
import org.wordpress.android.fluxc.network.rest.wpcom.wc.WooResult

@OptIn(ExperimentalCoroutinesApi::class)
class ObserveShippingPackagesTest : BaseUnitTest() {

    private val packageRepository: WooShippingLabelPackageRepository = mock()
    private val selectedSite: SelectedSite = mock()
    private val observeShippingPackages = ObserveShippingPackages(selectedSite, packageRepository)

    @Test
    fun `invoke should return Data with carrier and saved packages`() = testBlocking {
        val storePackages = generatePackagesData()
        val site = SiteModel().apply { id = 1 }
        whenever(selectedSite.get()).thenReturn(site)
        whenever(packageRepository.fetchShippingPackages(site)).thenReturn(WooResult(storePackages))

        val result = observeShippingPackages() as PackagesState.Data

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
    fun `invoke should return Error when fetchAllStorePackages returns error`() = testBlocking {
        val error = WooError(WooErrorType.GENERIC_ERROR, BaseRequest.GenericErrorType.UNKNOWN)
        val site = SiteModel().apply { id = 1 }
        whenever(selectedSite.get()).thenReturn(site)
        whenever(packageRepository.fetchShippingPackages(site)).thenReturn(WooResult(error))

        val result = observeShippingPackages()

        assertThat(result).isEqualTo(PackagesState.Error(WooErrorType.GENERIC_ERROR.name))
    }

    private fun generatePackagesData() = StorePackagesDAO(
        storeOptions = StoreOptionsDAO(
            currencySymbol = "$",
            dimensionUnit = "cm",
            weightUnit = "kg",
            originCountry = "US"
        ),
        savedPackages = listOf(
            PackageDAO(
                id = "1",
                name = "Saved Package 1",
                dimensions = "dimensions",
                weight = "weight",
                isLetter = false,
                dimensionUnit = "cm",
                weightUnit = "kg",
                saved = true
            ),
            PackageDAO(
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
        carrierPackages = mapOf(
            CarrierType.USPS to CarrierDAO(
                packageGroup = listOf(
                    CarrierPackageGroupDAO(
                        description = "Group 1",
                        packages = listOf(
                            PackageDAO(
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
