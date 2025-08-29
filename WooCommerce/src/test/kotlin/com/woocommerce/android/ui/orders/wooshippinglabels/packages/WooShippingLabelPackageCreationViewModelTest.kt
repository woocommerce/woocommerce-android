package com.woocommerce.android.ui.orders.wooshippinglabels.packages

import com.woocommerce.android.R
import com.woocommerce.android.WooException
import com.woocommerce.android.analytics.AnalyticsEvent
import com.woocommerce.android.analytics.AnalyticsTracker.Companion.KEY_ERROR
import com.woocommerce.android.analytics.AnalyticsTracker.Companion.KEY_STATE
import com.woocommerce.android.analytics.AnalyticsTracker.Companion.VALUE_STARTED
import com.woocommerce.android.analytics.AnalyticsTrackerWrapper
import com.woocommerce.android.tools.SelectedSite
import com.woocommerce.android.ui.orders.wooshippinglabels.models.StoreOptionsModel
import com.woocommerce.android.ui.orders.wooshippinglabels.packages.WooShippingLabelPackageCreationViewModel.PackageSelected
import com.woocommerce.android.ui.orders.wooshippinglabels.packages.WooShippingLabelPackageCreationViewModel.PackageType.ENVELOPE
import com.woocommerce.android.ui.orders.wooshippinglabels.packages.WooShippingLabelPackageCreationViewModel.PackagesState.Data
import com.woocommerce.android.ui.orders.wooshippinglabels.packages.WooShippingLabelPackageCreationViewModel.ShowPackageTypeDialog
import com.woocommerce.android.ui.orders.wooshippinglabels.packages.WooShippingLabelPackageCreationViewModel.ViewState
import com.woocommerce.android.ui.orders.wooshippinglabels.packages.datasource.ObserveShippingPackages
import com.woocommerce.android.ui.orders.wooshippinglabels.packages.datasource.WooShippingLabelPackageRepository
import com.woocommerce.android.ui.orders.wooshippinglabels.packages.ui.Carrier
import com.woocommerce.android.ui.orders.wooshippinglabels.packages.ui.Carrier.DHL
import com.woocommerce.android.ui.orders.wooshippinglabels.packages.ui.Carrier.USPS
import com.woocommerce.android.ui.orders.wooshippinglabels.packages.ui.CarrierPackageGroup
import com.woocommerce.android.ui.orders.wooshippinglabels.packages.ui.CustomPackageCreationData
import com.woocommerce.android.ui.orders.wooshippinglabels.packages.ui.PackageData
import com.woocommerce.android.ui.orders.wooshippinglabels.packages.ui.StoreOptionsForPackages
import com.woocommerce.android.viewmodel.BaseUnitTest
import com.woocommerce.android.viewmodel.MultiLiveEvent.Event
import com.woocommerce.android.viewmodel.ResourceProvider
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.advanceUntilIdle
import org.assertj.core.api.Assertions.assertThat
import org.junit.Before
import org.junit.Test
import org.mockito.kotlin.any
import org.mockito.kotlin.doReturn
import org.mockito.kotlin.mock
import org.mockito.kotlin.times
import org.mockito.kotlin.verify
import org.mockito.kotlin.whenever
import org.wordpress.android.fluxc.model.SiteModel
import org.wordpress.android.fluxc.network.BaseRequest
import org.wordpress.android.fluxc.network.BaseRequest.GenericErrorType
import org.wordpress.android.fluxc.network.rest.wpcom.wc.WooError
import org.wordpress.android.fluxc.network.rest.wpcom.wc.WooErrorType
import org.wordpress.android.fluxc.network.rest.wpcom.wc.WooResult
import org.wordpress.android.fluxc.persistence.entity.WooShippingPackagesEntity

@OptIn(ExperimentalCoroutinesApi::class)
class WooShippingLabelPackageCreationViewModelTest : BaseUnitTest() {

    private lateinit var sut: WooShippingLabelPackageCreationViewModel
    private val resourceProvider: ResourceProvider = mock()
    private val observeShippingPackages: ObserveShippingPackages = mock()
    private val packageRepository: WooShippingLabelPackageRepository = mock()
    private val selectedSite: SelectedSite = mock {
        on { getOrNull() } doReturn SiteModel().apply { siteId = 123 }
    }
    private val updateSavedCarrierPackages: UpdateSavedCarrierPackages = mock()

    private val tracker: AnalyticsTrackerWrapper = mock()

    val storeDimensionUnit = "cm"
    private val savedState = WooShippingLabelPackageCreationFragmentArgs(
        StoreOptionsModel.EMPTY.copy(dimensionUnit = storeDimensionUnit)
    ).toSavedStateHandle()

    @Before
    fun setUp() {
        whenever(
            resourceProvider.getString(R.string.woo_shipping_labels_package_creation_tab_custom)
        ).thenReturn("Custom")
        whenever(
            resourceProvider.getString(R.string.woo_shipping_labels_package_creation_tab_carrier)
        ).thenReturn("Carrier")
        whenever(
            resourceProvider.getString(R.string.woo_shipping_labels_package_creation_tab_saved)
        ).thenReturn("Saved")

        sut = WooShippingLabelPackageCreationViewModel(
            savedState,
            selectedSite,
            resourceProvider,
            observeShippingPackages,
            mock(),
            updateSavedCarrierPackages,
            packageRepository,
            tracker
        )
    }

    @Test
    fun `when started, track started event`() = testBlocking {
        verify(tracker).track(AnalyticsEvent.WCS_PACKAGE_SELECTION_STEP, mapOf(KEY_STATE to VALUE_STARTED))
    }

    @Test
    fun `onAddPackageClick triggers PackageSelected event`() = testBlocking {
        var lastEvent: Event? = null
        sut.event.observeForever { lastEvent = it }

        val customPackageData = CustomPackageCreationData(
            type = ENVELOPE,
            length = "10",
            width = "10",
            height = "10",
            weight = "20",
            name = "Test Package",
            saveAsTemplate = true
        )

        sut.onLengthChange("10")
        sut.onWidthChange("10")
        sut.onHeightChange("10")
        sut.onWeightChange("20")
        sut.onPackageNameChange("Test Package")
        sut.onSavePackageChanged(true)
        sut.onPackageTypeSelected(ENVELOPE)

        sut.onAddCustomPackageClick(savePackageAsTemplate = false)

        verify(packageRepository, times(0)).createCustomPackage(any(), any())
        assertThat(lastEvent).isEqualTo(PackageSelected(customPackageData.toPackageData(storeDimensionUnit)))
    }

    @Test
    fun `onAddCustomPackageClick triggers selected track`() = testBlocking {
        sut.onAddCustomPackageClick(savePackageAsTemplate = false)

        verify(tracker).track(AnalyticsEvent.WCS_PACKAGE_SELECTION_STEP, mapOf(KEY_STATE to "selected"))
    }

    @Test
    fun `onAddCarrierPackageClick triggers selected track`() = testBlocking {
        sut.onAddCarrierPackageClick()

        verify(tracker).track(AnalyticsEvent.WCS_PACKAGE_SELECTION_STEP, mapOf(KEY_STATE to "selected"))
    }

    @Test
    fun `onAddSavedPackageClick triggers selected track`() = testBlocking {
        sut.onAddSavedPackageClick()

        verify(tracker).track(AnalyticsEvent.WCS_PACKAGE_SELECTION_STEP, mapOf(KEY_STATE to "selected"))
    }

    @Test
    fun `onSavedPackageRemoved triggers removing_success track`() = testBlocking {
        val packageToRemove = PackageData(
            id = "1",
            name = "Package 1 - Carrier 1",
            dimensions = "10 x 10 x 10",
            weight = "10",
            isSelected = false,
            isLetter = false,
            isStarred = true
        )
        sut.onSavedPackageRemoved(packageToRemove)

        verify(tracker).track(AnalyticsEvent.WCS_PACKAGE_SELECTION_STEP, mapOf(KEY_STATE to "removing_success"))
    }

    @Test
    fun `onAddPackageClick triggers createCustomPackage when saveAsTemplate is true`() = testBlocking {
        sut.onLengthChange("1")
        sut.onWidthChange("1")
        sut.onHeightChange("1")
        sut.onAddCustomPackageClick(savePackageAsTemplate = true)

        verify(packageRepository, times(1)).createCustomPackage(any(), any())
    }

    @Test
    fun `when saveAsTemplate is true and saving succeed, track WCS_PACKAGE_SELECTION_STEP event with saving_success`() =
        testBlocking {
            whenever(packageRepository.createCustomPackage(any(), any())).thenReturn(
                WooResult(
                    listOf(
                        WooShippingPackagesEntity.Package(
                            id = "1",
                            name = "Saved Package 1",
                            dimensions = "dimensions",
                            weight = "weight",
                            isLetter = false,
                            dimensionUnit = "cm",
                            weightUnit = "kg",
                            saved = true
                        )
                    )
                )
            )

            sut.onLengthChange("1")
            sut.onWidthChange("1")
            sut.onHeightChange("1")
            sut.onAddCustomPackageClick(savePackageAsTemplate = true)

            verify(tracker).track(AnalyticsEvent.WCS_PACKAGE_SELECTION_STEP, mapOf(KEY_STATE to "saving_success"))
        }

    @Test
    fun `when saveAsTemplate is true and saving fails, track WCS_PACKAGE_SELECTION_STEP event with saving_failed`() =
        testBlocking {
            val error = WooError(WooErrorType.API_ERROR, BaseRequest.GenericErrorType.NETWORK_ERROR)
            whenever(packageRepository.createCustomPackage(any(), any())).thenReturn(WooResult(error))

            sut.onLengthChange("1")
            sut.onWidthChange("1")
            sut.onHeightChange("1")
            sut.onAddCustomPackageClick(savePackageAsTemplate = true)

            verify(tracker).track(
                AnalyticsEvent.WCS_PACKAGE_SELECTION_STEP,
                mapOf(KEY_STATE to "saving_failed", KEY_ERROR to error.type.toString())
            )
        }

    @Test
    fun `onAddPackageClick skips createCustomPackage when saveAsTemplate is false`() = testBlocking {
        sut.onAddCustomPackageClick(savePackageAsTemplate = false)

        verify(packageRepository, times(0)).createCustomPackage(any(), any())
    }

    @Test
    fun `onPackageTypeSpinnerClick triggers ShowPackageTypeDialog event`() = testBlocking {
        var lastEvent: Event? = null
        sut.event.observeForever { lastEvent = it }
        sut.onPackageTypeSelected(ENVELOPE)

        sut.onPackageTypeSpinnerClick()

        assertThat(
            lastEvent
        ).isEqualTo(ShowPackageTypeDialog(ENVELOPE))
    }

    @Test
    fun `onPackageTypeSelected updates viewState with new type`() = testBlocking {
        var lastViewState: ViewState? = null
        sut.viewState.observeForever { lastViewState = it }
        val newType = ENVELOPE
        sut.onPackageTypeSelected(newType)

        assertThat(lastViewState?.customPackageCreationData?.type).isEqualTo(newType)
    }

    @Test
    fun `onLengthChange updates viewState with new length`() = testBlocking {
        var lastViewState: ViewState? = null
        sut.viewState.observeForever { lastViewState = it }
        val newLength = "20"
        sut.onLengthChange(newLength)

        assertThat(lastViewState?.customPackageCreationData?.length).isEqualTo(newLength)
    }

    @Test
    fun `onWidthChange updates viewState with new width`() = testBlocking {
        var lastViewState: ViewState? = null
        sut.viewState.observeForever { lastViewState = it }
        val newWidth = "20"
        sut.onWidthChange(newWidth)

        assertThat(lastViewState?.customPackageCreationData?.width).isEqualTo(newWidth)
    }

    @Test
    fun `onHeightChange updates viewState with new height`() = testBlocking {
        var lastViewState: ViewState? = null
        sut.viewState.observeForever { lastViewState = it }
        val newHeight = "20"
        sut.onHeightChange(newHeight)

        assertThat(lastViewState?.customPackageCreationData?.height).isEqualTo(newHeight)
    }

    @Test
    fun `onSavePackageChanged updates viewState with new saveAsTemplate value`() = testBlocking {
        var lastViewState: ViewState? = null
        sut.viewState.observeForever { lastViewState = it }
        val newSaveAsTemplate = true
        sut.onSavePackageChanged(newSaveAsTemplate)

        assertThat(lastViewState?.customPackageCreationData?.saveAsTemplate).isEqualTo(newSaveAsTemplate)
    }

    @Test
    fun `onSavedPackageSelected selects only one package at a time`() = testBlocking {
        var lastViewState: ViewState? = null
        val package1 = PackageData(
            id = "1",
            name = "Package 1",
            dimensions = "10 x 10 x 10",
            weight = "10",
            isSelected = false,
            isLetter = false,
        )
        val package2 = PackageData(
            id = "2",
            name = "Package 2",
            dimensions = "20 x 20 x 20",
            weight = "20",
            isSelected = false,
            isLetter = true,
        )
        whenever(observeShippingPackages()).thenReturn(
            flowOf(
                Data(
                    storeOptions = StoreOptionsForPackages.DEFAULT,
                    carrierPackages = emptyMap(),
                    savedPackages = listOf(package1, package2)
                )
            )
        )

        sut = WooShippingLabelPackageCreationViewModel(
            savedState,
            selectedSite,
            resourceProvider,
            observeShippingPackages,
            mock(),
            updateSavedCarrierPackages,
            packageRepository,
            tracker
        )
        sut.viewState.observeForever { lastViewState = it }
        sut.onSavedPackageSelected(package1, true)

        val selectedPackages = lastViewState?.packagesData?.savedPackages?.filter { it.isSelected }
        assertThat(selectedPackages).isNotNull
        assertThat(selectedPackages).size().isEqualTo(1)
        assertThat(selectedPackages?.first()).isEqualTo(package1.copy(isSelected = true))
    }

    @Test
    fun `onCarrierPackageSelected selects only one package at a time`() = testBlocking {
        var lastViewState: ViewState? = null
        val carrier: Carrier = DHL
        val package1 = PackageData(
            id = "1",
            name = "Package 1",
            dimensions = "10 x 10 x 10",
            weight = "10",
            isSelected = false,
            isLetter = false,
        )
        val package2 = PackageData(
            id = "2",
            name = "Package 2",
            dimensions = "20 x 20 x 20",
            weight = "20",
            isSelected = false,
            isLetter = true,
        )
        val carrierPackages = mapOf(
            carrier to listOf(
                CarrierPackageGroup(
                    groupName = "Group 1",
                    packages = listOf(package1, package2)
                )
            )
        )
        whenever(observeShippingPackages()).thenReturn(
            flowOf(
                Data(
                    storeOptions = StoreOptionsForPackages.DEFAULT,
                    carrierPackages = carrierPackages,
                    savedPackages = emptyList()
                )
            )
        )

        sut = WooShippingLabelPackageCreationViewModel(
            savedState,
            selectedSite,
            resourceProvider,
            observeShippingPackages,
            mock(),
            updateSavedCarrierPackages,
            packageRepository,
            tracker
        )
        sut.viewState.observeForever { lastViewState = it }
        sut.onCarrierPackageSelected(package1, true)

        val selectedPackages = lastViewState
            ?.packagesData
            ?.carrierPackages
            ?.values
            ?.flatten()
            ?.flatMap { it.packages }
            ?.filter { it.isSelected }

        assertThat(selectedPackages).isNotNull
        assertThat(selectedPackages).size().isEqualTo(1)
        assertThat(selectedPackages?.first()).isEqualTo(package1.copy(isSelected = true))
    }

    @Test
    @Suppress("LongMethod")
    fun `onCarrierPackageSelected selects only one package at a time with multiple carriers`() = testBlocking {
        var lastViewState: ViewState? = null
        val carrier1: Carrier = DHL
        val carrier2: Carrier = USPS
        val package1 = PackageData(
            id = "1",
            name = "Package 1 - Carrier 1",
            dimensions = "10 x 10 x 10",
            weight = "10",
            isSelected = false,
            isLetter = false,
        )
        val package2 = PackageData(
            id = "2",
            name = "Package 2 - Carrier 1",
            dimensions = "20 x 20 x 20",
            weight = "20",
            isSelected = false,
            isLetter = true,
        )
        val package3 = PackageData(
            id = "3",
            name = "Package 1 - Carrier 2",
            dimensions = "30 x 30 x 30",
            weight = "30",
            isSelected = false,
            isLetter = false,
        )
        val package4 = PackageData(
            id = "4",
            name = "Package 2 - Carrier 2",
            dimensions = "40 x 40 x 40",
            weight = "40",
            isSelected = false,
            isLetter = true,
        )
        val carrierPackages = mapOf(
            carrier1 to listOf(
                CarrierPackageGroup(
                    groupName = "Group 1",
                    packages = listOf(package1, package2)
                )
            ),
            carrier2 to listOf(
                CarrierPackageGroup(
                    groupName = "Group 2",
                    packages = listOf(package3, package4)
                )
            )
        )
        whenever(observeShippingPackages()).thenReturn(
            flowOf(
                Data(
                    storeOptions = StoreOptionsForPackages.DEFAULT,
                    carrierPackages = carrierPackages,
                    savedPackages = emptyList()
                )
            )
        )

        sut = WooShippingLabelPackageCreationViewModel(
            savedState,
            selectedSite,
            resourceProvider,
            observeShippingPackages,
            mock(),
            updateSavedCarrierPackages,
            packageRepository,
            tracker
        )

        sut.viewState.observeForever { lastViewState = it }
        sut.onCarrierPackageSelected(package1, true)
        sut.onCarrierPackageSelected(package2, true)
        advanceUntilIdle()

        val selectedPackages = lastViewState
            ?.packagesData
            ?.carrierPackages
            ?.values
            ?.flatten()
            ?.flatMap { it.packages }
            ?.filter { it.isSelected }

        assertThat(selectedPackages).isNotNull
        assertThat(selectedPackages).size().isEqualTo(1)
        assertThat(selectedPackages?.first()).isEqualTo(package2.copy(isSelected = true))
    }

    @Test
    fun `When starring a carrier package, update UI status and invoke updateSavedCarrierPackages use case`() =
        testBlocking {
            val carrier: Carrier = USPS
            val packageToStar = PackageData(
                id = "1",
                name = "Package 1 - Carrier 1",
                dimensions = "10 x 10 x 10",
                weight = "10",
                isSelected = false,
                isLetter = false,
                isStarred = false // Initially not starred
            )
            val initialCarrierPackages = mapOf(
                carrier to listOf(
                    CarrierPackageGroup(
                        groupName = "Group A",
                        packages = listOf(packageToStar)
                    )
                )
            )
            whenever(observeShippingPackages()).thenReturn(
                flowOf(
                    Data(
                        storeOptions = StoreOptionsForPackages.DEFAULT,
                        carrierPackages = initialCarrierPackages,
                        savedPackages = emptyList()
                    )
                )
            )

            sut = WooShippingLabelPackageCreationViewModel(
                savedState,
                selectedSite,
                resourceProvider,
                observeShippingPackages,
                mock(),
                updateSavedCarrierPackages,
                packageRepository,
                tracker
            )
            advanceUntilIdle()

            var lastViewState: ViewState? = null
            sut.viewState.observeForever { lastViewState = it }
            // When
            sut.onCarrierPackageStarred(packageToStar, true)
            advanceUntilIdle()

            // Then
            verify(updateSavedCarrierPackages, times(1)).invoke(
                true,
                packageToStar.id,
                false, // isUserDefined is false for carrier packages
                lastViewState?.packagesData?.carrierPackages!!
            )

            // Verify viewState is updated
            val updatedPackage = lastViewState
                .packagesData
                ?.carrierPackages
                ?.get(carrier)
                ?.flatMap { it.packages }
                ?.find { it.id == packageToStar.id }

            assertThat(updatedPackage).isNotNull
            assertThat(updatedPackage?.isStarred).isTrue
        }

    @Test
    fun `when starring a package, then update saved packages`() = testBlocking {
        val carrier: Carrier = USPS
        val packageToStar = PackageData(
            id = "1",
            name = "Package 1 - Carrier 1",
            dimensions = "10 x 10 x 10",
            weight = "10",
            isSelected = false,
            isLetter = false,
            isStarred = false // Initially not starred
        )
        val initialCarrierPackages = mapOf(
            carrier to listOf(CarrierPackageGroup(groupName = "Group A", packages = listOf(packageToStar)))
        )
        val initialSavedPackages = emptyList<PackageData>()

        whenever(observeShippingPackages()).thenReturn(
            flowOf(
                Data(
                    storeOptions = StoreOptionsForPackages.DEFAULT,
                    carrierPackages = initialCarrierPackages,
                    savedPackages = initialSavedPackages
                )
            )
        )

        sut = WooShippingLabelPackageCreationViewModel(
            savedState,
            selectedSite,
            resourceProvider,
            observeShippingPackages,
            mock(),
            updateSavedCarrierPackages,
            packageRepository,
            tracker
        )
        advanceUntilIdle()

        var lastViewState: ViewState? = null
        sut.viewState.observeForever { lastViewState = it }

        // Star a package
        sut.onCarrierPackageStarred(packageData = packageToStar, isStarred = true)
        advanceUntilIdle()

        // Verify it's added to saved packages
        val savedPackages = lastViewState?.packagesData?.savedPackages
        assertThat(savedPackages).hasSize(1)
        assertThat(savedPackages).contains(packageToStar)
    }

    @Test
    fun `when unstarring a package, then update saved packages`() = testBlocking {
        val carrier: Carrier = USPS
        val packageToUnstar = PackageData(
            id = "1",
            name = "Package 1 - Carrier 1",
            dimensions = "10 x 10 x 10",
            weight = "10",
            isSelected = false,
            isLetter = false,
            isStarred = true // Initially starred
        )
        val initialCarrierPackages = mapOf(
            carrier to listOf(CarrierPackageGroup(groupName = "Group A", packages = listOf(packageToUnstar)))
        )
        val initialSavedPackages = listOf(packageToUnstar)

        whenever(observeShippingPackages()).thenReturn(
            flowOf(
                Data(
                    storeOptions = StoreOptionsForPackages.DEFAULT,
                    carrierPackages = initialCarrierPackages,
                    savedPackages = initialSavedPackages
                )
            )
        )

        sut = WooShippingLabelPackageCreationViewModel(
            savedState,
            selectedSite,
            resourceProvider,
            observeShippingPackages,
            mock(),
            updateSavedCarrierPackages,
            packageRepository,
            tracker
        )
        advanceUntilIdle()

        var lastViewState: ViewState? = null
        sut.viewState.observeForever { lastViewState = it }

        // Unstar a package
        sut.onCarrierPackageStarred(packageToUnstar, false)
        advanceUntilIdle()

        // Verify it's removed from saved packages
        val savedPackages = lastViewState?.packagesData?.savedPackages
        assertThat(savedPackages).isEmpty()
    }

    @Test
    fun `when unstarring a package fails, then track event with removing_failed property`() = testBlocking {
        val carrier: Carrier = USPS
        val packageToUnstar = PackageData(
            id = "1",
            name = "Package 1 - Carrier 1",
            dimensions = "10 x 10 x 10",
            weight = "10",
            isSelected = false,
            isLetter = false,
            isStarred = true // Initially starred
        )
        val initialCarrierPackages = mapOf(
            carrier to listOf(CarrierPackageGroup(groupName = "Group A", packages = listOf(packageToUnstar)))
        )
        val initialSavedPackages = listOf(packageToUnstar)

        whenever(observeShippingPackages()).thenReturn(
            flowOf(
                Data(
                    storeOptions = StoreOptionsForPackages.DEFAULT,
                    carrierPackages = initialCarrierPackages,
                    savedPackages = initialSavedPackages
                )
            )
        )
        val wooError = WooError(type = WooErrorType.API_ERROR, original = GenericErrorType.UNKNOWN)
        val wooException = WooException(wooError)
        whenever(updateSavedCarrierPackages(any(), any(), any(), any())).thenReturn(Result.failure(wooException))

        sut = WooShippingLabelPackageCreationViewModel(
            savedState,
            selectedSite,
            resourceProvider,
            observeShippingPackages,
            mock(),
            updateSavedCarrierPackages,
            packageRepository,
            tracker
        )
        advanceUntilIdle()

        // Unstar a package
        sut.onCarrierPackageStarred(packageToUnstar, false)
        advanceUntilIdle()

        // Verify WCS_PACKAGE_SELECTION_STEP event is tracked with "removing_failed" property
        val expectedProperty = mapOf(KEY_STATE to "removing_failed", KEY_ERROR to WooErrorType.API_ERROR.name)
        verify(tracker).track(AnalyticsEvent.WCS_PACKAGE_SELECTION_STEP, expectedProperty)
    }

    @Test
    fun `when removing a package from saved, then update carrier packages`() = testBlocking {
        val carrier: Carrier = USPS
        val packageToRemove = PackageData(
            id = "1",
            name = "Package 1 - Carrier 1",
            dimensions = "10 x 10 x 10",
            weight = "10",
            isSelected = false,
            isLetter = false,
            isStarred = true
        )
        val initialCarrierPackages = mapOf(
            carrier to listOf(CarrierPackageGroup(groupName = "Group A", packages = listOf(packageToRemove)))
        )
        val initialSavedPackages = listOf(packageToRemove)

        whenever(observeShippingPackages()).thenReturn(
            flowOf(
                Data(
                    storeOptions = StoreOptionsForPackages.DEFAULT,
                    carrierPackages = initialCarrierPackages,
                    savedPackages = initialSavedPackages
                )
            )
        )

        sut = WooShippingLabelPackageCreationViewModel(
            savedState,
            selectedSite,
            resourceProvider,
            observeShippingPackages,
            mock(),
            updateSavedCarrierPackages,
            packageRepository,
            tracker
        )
        advanceUntilIdle()

        var lastViewState: ViewState? = null
        sut.viewState.observeForever { lastViewState = it }

        // Act
        sut.onSavedPackageRemoved(packageToRemove)
        advanceUntilIdle()

        // Assert
        // Verify it's removed from saved packages
        val savedPackages = lastViewState?.packagesData?.savedPackages
        assertThat(savedPackages).isEmpty()

        // Verify the carrier package is unstarred
        val carrierPackage = lastViewState?.packagesData?.carrierPackages?.get(USPS)?.first()?.packages?.first()
        assertThat(carrierPackage?.isStarred).isFalse
    }

    @Test
    fun `when dimensions are invalid and add custom package clicked, then show error`() = testBlocking {
        var lastViewState: ViewState? = null
        sut.viewState.observeForever { lastViewState = it }

        sut.onLengthChange("0")
        sut.onWidthChange("0")
        sut.onHeightChange("0")
        sut.onAddCustomPackageClick(savePackageAsTemplate = false)

        assertThat(lastViewState?.customPackageCreationData?.invalidDimensionError).isTrue
    }
}
