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
import org.wordpress.android.fluxc.model.LocalOrRemoteId
import org.wordpress.android.fluxc.model.SiteModel
import org.wordpress.android.fluxc.network.BaseRequest.GenericErrorType.UNKNOWN
import org.wordpress.android.fluxc.network.rest.wpcom.wc.WooError
import org.wordpress.android.fluxc.network.rest.wpcom.wc.WooErrorType.GENERIC_ERROR
import org.wordpress.android.fluxc.network.rest.wpcom.wc.WooPayload
import org.wordpress.android.fluxc.persistence.entity.WooShippingPackagesEntity

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
        repository = WooShippingLabelPackageRepository(selectedSite, packageMapper, packageRestClient, mock())
    }

    @Test
    fun `when fetchShippingPackages succeed, then return WooResult with result`() = testBlocking {
        val localSiteId = LocalOrRemoteId.LocalId(1)
        val shippingPackages = WooShippingPackagesEntity(
            localSiteId = localSiteId,
            storeOptions = WooShippingPackagesEntity.StoreOptions(
                currencySymbol = "",
                dimensionUnit = "",
                weightUnit = "",
                originCountry = ""
            ),
            savedPackages = listOf(),
            carrierPackageGroups = listOf()
        )
        val packageResponse = mock<PackageResponse>()
        whenever(packageRestClient.fetchShippingLabelPackages(siteModel)).thenReturn(WooPayload(packageResponse))
        whenever(packageMapper(siteModel, packageResponse)).thenReturn(shippingPackages)

        val result = repository.fetchShippingPackages()

        assertThat(result.isError).isFalse
        assertThat(shippingPackages).isEqualTo(result.model)
    }

    @Test
    fun `when fetchShippingPackages returns error, then return WooResult with error`() = testBlocking {
        val error = WooError(type = GENERIC_ERROR, original = UNKNOWN)
        whenever(packageRestClient.fetchShippingLabelPackages(siteModel)).thenReturn(WooPayload(error))

        val result = repository.fetchShippingPackages()

        assertThat(result.isError).isTrue
        assertThat(error).isEqualTo(result.error)
    }
}
