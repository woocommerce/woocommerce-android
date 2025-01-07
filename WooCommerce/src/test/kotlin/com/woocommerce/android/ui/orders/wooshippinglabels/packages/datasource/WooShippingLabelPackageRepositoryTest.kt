package com.woocommerce.android.ui.orders.wooshippinglabels.packages.datasource

import com.woocommerce.android.tools.SelectedSite
import com.woocommerce.android.ui.orders.wooshippinglabels.packages.networking.PackageResponse
import com.woocommerce.android.ui.orders.wooshippinglabels.packages.networking.WooShippingLabelPackageRestClient
import com.woocommerce.android.viewmodel.BaseUnitTest
import kotlinx.coroutines.ExperimentalCoroutinesApi
import org.assertj.core.api.Assertions.assertThat
import org.junit.Before
import org.junit.Test
import org.mockito.kotlin.mock
import org.mockito.kotlin.whenever
import org.wordpress.android.fluxc.model.SiteModel
import org.wordpress.android.fluxc.network.BaseRequest.GenericErrorType.UNKNOWN
import org.wordpress.android.fluxc.network.rest.wpcom.wc.WooError
import org.wordpress.android.fluxc.network.rest.wpcom.wc.WooErrorType.GENERIC_ERROR
import org.wordpress.android.fluxc.network.rest.wpcom.wc.WooPayload

@OptIn(ExperimentalCoroutinesApi::class)
class WooShippingLabelPackageRepositoryTest : BaseUnitTest() {

    private lateinit var repository: WooShippingLabelPackageRepository
    private val selectedSite: SelectedSite = mock()
    private val packageMapper: WooShippingLabelPackageMapper = mock()
    private val packageRestClient: WooShippingLabelPackageRestClient = mock()
    private val siteModel: SiteModel = mock()

    @Before
    fun setUp() {
        whenever(selectedSite.get()).thenReturn(siteModel)
        repository = WooShippingLabelPackageRepository(selectedSite, packageMapper, packageRestClient)
    }

    @Test
    fun `fetchAllStorePackages returns WooResult with result`() = testBlocking {
        val storePackagesDAO = StorePackagesDAO(
            storeOptions = StoreOptionsDAO(
                currencySymbol = "",
                dimensionUnit = "",
                weightUnit = "",
                originCountry = ""
            ),
            savedPackages = listOf(),
            carrierPackages = mapOf()
        )
        val packageResponse = mock<PackageResponse>()
        whenever(packageRestClient.fetchShippingLabelPackages(siteModel)).thenReturn(WooPayload(packageResponse))
        whenever(packageMapper(packageResponse)).thenReturn(storePackagesDAO)

        val result = repository.fetchAllStorePackages()

        assertThat(result.isError).isFalse
        assertThat(storePackagesDAO).isEqualTo(result.model)
    }

    @Test
    fun `fetchAllStorePackages returns WooResult with error`() = testBlocking {
        val error = WooError(
            type = GENERIC_ERROR,
            original = UNKNOWN
        )
        whenever(packageRestClient.fetchShippingLabelPackages(siteModel)).thenReturn(WooPayload(error))

        val result = repository.fetchAllStorePackages()

        assertThat(result.isError).isTrue
        assertThat(error).isEqualTo(result.error)
    }
}
