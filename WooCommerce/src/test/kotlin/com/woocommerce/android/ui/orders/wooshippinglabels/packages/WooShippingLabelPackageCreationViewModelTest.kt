package com.woocommerce.android.ui.orders.wooshippinglabels.packages

import androidx.lifecycle.SavedStateHandle
import com.woocommerce.android.R
import com.woocommerce.android.tools.SelectedSite
import com.woocommerce.android.ui.orders.wooshippinglabels.packages.WooShippingLabelPackageCreationViewModel.PackageSelected
import com.woocommerce.android.ui.orders.wooshippinglabels.packages.WooShippingLabelPackageCreationViewModel.PackageType
import com.woocommerce.android.ui.orders.wooshippinglabels.packages.WooShippingLabelPackageCreationViewModel.PredefinedPackagesState
import com.woocommerce.android.ui.orders.wooshippinglabels.packages.WooShippingLabelPackageCreationViewModel.ShowPackageTypeDialog
import com.woocommerce.android.ui.orders.wooshippinglabels.packages.WooShippingLabelPackageCreationViewModel.ViewState
import com.woocommerce.android.ui.orders.wooshippinglabels.packages.datasource.FetchPredefinedPackagesFromStore
import com.woocommerce.android.ui.orders.wooshippinglabels.packages.datasource.WooShippingLabelPackageRepository
import com.woocommerce.android.ui.orders.wooshippinglabels.packages.ui.Carrier
import com.woocommerce.android.ui.orders.wooshippinglabels.packages.ui.CarrierPackageGroup
import com.woocommerce.android.ui.orders.wooshippinglabels.packages.ui.CustomPackageCreationData
import com.woocommerce.android.ui.orders.wooshippinglabels.packages.ui.PackageData
import com.woocommerce.android.ui.orders.wooshippinglabels.packages.ui.StoreOptionsForPackages
import com.woocommerce.android.viewmodel.BaseUnitTest
import com.woocommerce.android.viewmodel.MultiLiveEvent
import com.woocommerce.android.viewmodel.ResourceProvider
import kotlinx.coroutines.ExperimentalCoroutinesApi
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

@OptIn(ExperimentalCoroutinesApi::class)
class WooShippingLabelPackageCreationViewModelTest : BaseUnitTest() {

    private lateinit var sut: WooShippingLabelPackageCreationViewModel
    private val resourceProvider: ResourceProvider = mock()
    private val fetchPredefinedPackages: FetchPredefinedPackagesFromStore = mock()
    private val packageRepository: WooShippingLabelPackageRepository = mock()
    private val selectedSite: SelectedSite = mock {
        on { getOrNull() } doReturn SiteModel().apply { siteId = 123 }
    }

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
            SavedStateHandle(),
            selectedSite,
            resourceProvider,
            fetchPredefinedPackages,
            packageRepository
        )
    }

    @Test
    fun `onAddPackageClick triggers PackageSelected event`() = testBlocking {
        var lastEvent: MultiLiveEvent.Event? = null
        sut.event.observeForever { lastEvent = it }

        val customPackageData = CustomPackageCreationData(
            type = PackageType.ENVELOPE,
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
        sut.onPackageTypeSelected(PackageType.ENVELOPE)

        sut.onAddCustomPackageClick(savePackageAsTemplate = false)

        verify(packageRepository, times(0)).createCustomPackage(any(), any())
        assertThat(lastEvent).isEqualTo(PackageSelected(customPackageData.toPackageData("cm")))
    }

    @Test
    fun `onAddPackageClick triggers createCustomPackage when saveAsTemplate is true`() = testBlocking {
        sut.onAddCustomPackageClick(savePackageAsTemplate = true)

        verify(packageRepository, times(1)).createCustomPackage(any(), any())
    }

    @Test
    fun `onAddPackageClick skips createCustomPackage when saveAsTemplate is false`() = testBlocking {
        sut.onAddCustomPackageClick(savePackageAsTemplate = false)

        verify(packageRepository, times(0)).createCustomPackage(any(), any())
    }

    @Test
    fun `onPackageTypeSpinnerClick triggers ShowPackageTypeDialog event`() = testBlocking {
        var lastEvent: MultiLiveEvent.Event? = null
        sut.event.observeForever { lastEvent = it }
        sut.onPackageTypeSelected(PackageType.ENVELOPE)

        sut.onPackageTypeSpinnerClick()

        assertThat(
            lastEvent
        ).isEqualTo(ShowPackageTypeDialog(PackageType.ENVELOPE))
    }

    @Test
    fun `onPackageTypeSelected updates viewState with new type`() = testBlocking {
        var lastViewState: ViewState? = null
        sut.viewState.observeForever { lastViewState = it }
        val newType = PackageType.ENVELOPE
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
        whenever(fetchPredefinedPackages()).thenReturn(
            PredefinedPackagesState.Data(
                storeOptions = StoreOptionsForPackages.DEFAULT,
                carrierPackages = emptyMap(),
                savedPackages = listOf(package1, package2)
            )
        )

        sut = WooShippingLabelPackageCreationViewModel(
            SavedStateHandle(),
            selectedSite,
            resourceProvider,
            fetchPredefinedPackages,
            packageRepository
        )
        sut.viewState.observeForever { lastViewState = it }
        sut.onSavedPackageSelected(package1, true)

        val selectedPackages = lastViewState?.predefinedPackagesData?.savedPackages?.filter { it.isSelected }
        assertThat(selectedPackages).isNotNull
        assertThat(selectedPackages).size().isEqualTo(1)
        assertThat(selectedPackages?.first()).isEqualTo(package1.copy(isSelected = true))
    }

    @Test
    fun `onCarrierPackageSelected selects only one package at a time`() = testBlocking {
        var lastViewState: ViewState? = null
        val carrier: Carrier = Carrier.DHL
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
        whenever(fetchPredefinedPackages()).thenReturn(
            PredefinedPackagesState.Data(
                storeOptions = StoreOptionsForPackages.DEFAULT,
                carrierPackages = carrierPackages,
                savedPackages = emptyList()
            )
        )

        sut = WooShippingLabelPackageCreationViewModel(
            SavedStateHandle(),
            selectedSite,
            resourceProvider,
            fetchPredefinedPackages,
            packageRepository
        )
        sut.viewState.observeForever { lastViewState = it }
        sut.onCarrierPackageSelected(package1, true)

        val selectedPackages = lastViewState
            ?.predefinedPackagesData
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
        val carrier1: Carrier = Carrier.DHL
        val carrier2: Carrier = Carrier.USPS
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
        whenever(fetchPredefinedPackages()).thenReturn(
            PredefinedPackagesState.Data(
                storeOptions = StoreOptionsForPackages.DEFAULT,
                carrierPackages = carrierPackages,
                savedPackages = emptyList()
            )
        )

        sut = WooShippingLabelPackageCreationViewModel(
            SavedStateHandle(),
            selectedSite,
            resourceProvider,
            fetchPredefinedPackages,
            packageRepository
        )

        sut.viewState.observeForever { lastViewState = it }
        sut.onCarrierPackageSelected(package1, true)
        sut.onCarrierPackageSelected(package2, true)
        advanceUntilIdle()

        val selectedPackages = lastViewState
            ?.predefinedPackagesData
            ?.carrierPackages
            ?.values
            ?.flatten()
            ?.flatMap { it.packages }
            ?.filter { it.isSelected }

        assertThat(selectedPackages).isNotNull
        assertThat(selectedPackages).size().isEqualTo(1)
        assertThat(selectedPackages?.first()).isEqualTo(package2.copy(isSelected = true))
    }
}
