package com.woocommerce.android.ui.orders.wooshippinglabels.packages.datasource

import com.woocommerce.android.tools.SelectedSite
import com.woocommerce.android.ui.orders.wooshippinglabels.packages.networking.PackageCreationResponse
import com.woocommerce.android.ui.orders.wooshippinglabels.packages.networking.PackageResponse
import com.woocommerce.android.ui.orders.wooshippinglabels.packages.networking.WooShippingLabelPackageRestClient
import com.woocommerce.android.viewmodel.BaseUnitTest
import kotlinx.coroutines.ExperimentalCoroutinesApi
import org.assertj.core.api.Assertions.assertThat
import org.junit.Before
import org.junit.Test
import org.mockito.kotlin.any
import org.mockito.kotlin.argumentCaptor
import org.mockito.kotlin.eq
import org.mockito.kotlin.mock
import org.mockito.kotlin.verify
import org.mockito.kotlin.whenever
import org.wordpress.android.fluxc.model.LocalOrRemoteId
import org.wordpress.android.fluxc.model.SiteModel
import org.wordpress.android.fluxc.network.BaseRequest.GenericErrorType.UNKNOWN
import org.wordpress.android.fluxc.network.rest.wpcom.wc.WooError
import org.wordpress.android.fluxc.network.rest.wpcom.wc.WooErrorType.GENERIC_ERROR
import org.wordpress.android.fluxc.network.rest.wpcom.wc.WooPayload
import org.wordpress.android.fluxc.persistence.dao.WooShippingDao
import org.wordpress.android.fluxc.persistence.entity.WooShippingPackagesEntity
import org.wordpress.android.fluxc.persistence.entity.WooShippingPackagesEntity.CarrierPackageGroup
import org.wordpress.android.fluxc.persistence.entity.WooShippingPackagesEntity.CarrierPackageGroups
import org.wordpress.android.fluxc.persistence.entity.WooShippingPackagesEntity.CarrierType
import org.wordpress.android.fluxc.persistence.entity.WooShippingPackagesEntity.Package

@OptIn(ExperimentalCoroutinesApi::class)
class WooShippingLabelPackageRepositoryTest : BaseUnitTest() {

    private lateinit var repository: WooShippingLabelPackageRepository
    private val selectedSite: SelectedSite = mock()
    private val packageMapper: WooShippingLabelPackageMapper = mock()
    private val packageRestClient: WooShippingLabelPackageRestClient = mock()
    private val wooShippingDao: WooShippingDao = mock()
    private val siteModel: SiteModel = SiteModel().apply { id = 1 }

    @Before
    fun setUp() {
        whenever(selectedSite.get()).thenReturn(siteModel)
        repository = WooShippingLabelPackageRepository(selectedSite, packageMapper, packageRestClient, wooShippingDao)
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

    @Test
    fun `when saveCarrierPackage invoke, then mark package saved in cache and add to savedPackages`() = testBlocking {
        // Given
        val localSiteId = LocalOrRemoteId.LocalId(1)
        val packageId = "p1"
        val parentCarrierId = CarrierType.USPS.id
        val initialPackage = Package(
            id = packageId,
            name = "Carrier Package 1",
            dimensions = "",
            weight = "",
            isLetter = false,
            dimensionUnit = "",
            isUserDefined = false,
            weightUnit = "",
            groupName = "Group 1",
            saved = false
        )
        val initialEntity = WooShippingPackagesEntity(
            localSiteId = localSiteId,
            storeOptions = WooShippingPackagesEntity.StoreOptions("$", "cm", "kg", "US"),
            savedPackages = emptyList(),
            carrierPackageGroups = listOf(
                CarrierPackageGroups(
                    carrierType = CarrierType.USPS,
                    packageGroups = listOf(
                        CarrierPackageGroup(
                            description = "Group 1",
                            packages = listOf(initialPackage)
                        )
                    )
                )
            )
        )
        whenever(wooShippingDao.getShippingPackages(localSiteId)).thenReturn(initialEntity)
        whenever(packageRestClient.postPredefinedPackages(eq(siteModel), any())).thenReturn(
            WooPayload(
                PackageCreationResponse()
            )
        )

        // When
        repository.saveCarrierPackage(savedPackageId = packageId, parentCarrierId = parentCarrierId, site = siteModel)

        // Then
        val captor = argumentCaptor<WooShippingPackagesEntity>()
        verify(wooShippingDao).insertShippingPackages(captor.capture())
        val updated = captor.firstValue

        // Assert saved flag in carrier groups
        val updatedPackage = updated.carrierPackageGroups
            .first { it.carrierType == CarrierType.USPS }
            .packageGroups!!.first().packages!!.first { it.id == packageId }
        assertThat(updatedPackage.saved).isTrue

        // Assert savedPackages contains the package once
        assertThat(updated.savedPackages.map { it.id }).containsExactly(packageId)
    }

    @Test
    fun `when deleteSavedCarrierPackage invoke, then mark package unsaved in cache and remove from savedPackages`() =
        testBlocking {
            // Given
            val localSiteId = LocalOrRemoteId.LocalId(1)
            val packageId = "p1"
            val isUserDefined = false
            val initialPackage = Package(
                id = packageId,
                name = "Carrier Package 1",
                dimensions = "",
                weight = "",
                isLetter = false,
                dimensionUnit = "",
                isUserDefined = isUserDefined,
                weightUnit = "",
                groupName = "Group 1",
                saved = true
            )
            val initialEntity = WooShippingPackagesEntity(
                localSiteId = localSiteId,
                storeOptions = WooShippingPackagesEntity.StoreOptions("$", "cm", "kg", "US"),
                savedPackages = listOf(initialPackage),
                carrierPackageGroups = listOf(
                    CarrierPackageGroups(
                        carrierType = CarrierType.USPS,
                        packageGroups = listOf(
                            CarrierPackageGroup(
                                description = "Group 1",
                                packages = listOf(initialPackage)
                            )
                        )
                    )
                )
            )
            whenever(wooShippingDao.getShippingPackages(localSiteId)).thenReturn(initialEntity)
            whenever(packageRestClient.deleteSavedCarrierPackage(eq(siteModel), eq(isUserDefined), eq(packageId)))
                .thenReturn(WooPayload(PackageCreationResponse()))

            // When
            repository.deleteSavedCarrierPackage(packageId = packageId, isUserDefined = isUserDefined, site = siteModel)

            // Then
            val captor = argumentCaptor<WooShippingPackagesEntity>()
            verify(wooShippingDao).insertShippingPackages(captor.capture())
            val updated = captor.firstValue

            // Assert saved flag in carrier groups is false
            val updatedPackage = updated.carrierPackageGroups
                .first { it.carrierType == CarrierType.USPS }
                .packageGroups!!.first().packages!!.first { it.id == packageId }
            assertThat(updatedPackage.saved).isFalse

            // Assert savedPackages no longer contains the package
            assertThat(updated.savedPackages.any { it.id == packageId }).isFalse
        }
}
