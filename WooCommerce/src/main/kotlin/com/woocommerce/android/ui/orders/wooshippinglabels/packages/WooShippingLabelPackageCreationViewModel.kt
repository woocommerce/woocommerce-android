package com.woocommerce.android.ui.orders.wooshippinglabels.packages

import android.os.Parcelable
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.asLiveData
import androidx.lifecycle.viewModelScope
import com.woocommerce.android.R
import com.woocommerce.android.tools.SelectedSite
import com.woocommerce.android.ui.orders.wooshippinglabels.packages.datasource.FetchPredefinedPackagesFromStore
import com.woocommerce.android.ui.orders.wooshippinglabels.packages.datasource.WooShippingLabelPackageRepository
import com.woocommerce.android.ui.orders.wooshippinglabels.packages.networking.CustomPackageCreationRequestData
import com.woocommerce.android.ui.orders.wooshippinglabels.packages.ui.Carrier
import com.woocommerce.android.ui.orders.wooshippinglabels.packages.ui.CarrierPackageGroup
import com.woocommerce.android.ui.orders.wooshippinglabels.packages.ui.CustomPackageCreationData
import com.woocommerce.android.ui.orders.wooshippinglabels.packages.ui.PackageData
import com.woocommerce.android.ui.orders.wooshippinglabels.packages.ui.StoreOptionsForPackages
import com.woocommerce.android.viewmodel.MultiLiveEvent
import com.woocommerce.android.viewmodel.ResourceProvider
import com.woocommerce.android.viewmodel.ScopedViewModel
import com.woocommerce.android.viewmodel.getStateFlow
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.parcelize.Parcelize
import org.wordpress.android.fluxc.model.SiteModel
import javax.inject.Inject

@HiltViewModel
class WooShippingLabelPackageCreationViewModel @Inject constructor(
    savedState: SavedStateHandle,
    private val selectedSite: SelectedSite,
    private val resourceProvider: ResourceProvider,
    private val fetchPredefinedPackages: FetchPredefinedPackagesFromStore,
    private val packageRepository: WooShippingLabelPackageRepository
) : ScopedViewModel(savedState) {

    private val _viewState = savedState.getStateFlow(
        scope = viewModelScope,
        initialValue = ViewState(pageTabs)
    )
    val viewState = _viewState.asLiveData()

    private val storeOptions: StoreOptionsForPackages
        get() = _viewState.value.predefinedPackagesData
            ?.storeOptions
            ?: StoreOptionsForPackages.DEFAULT

    private val pageTabs
        get() = listOf(
            PageTab(
                type = PageType.CUSTOM,
                title = resourceProvider.getString(R.string.woo_shipping_labels_package_creation_tab_custom)
            ),
            PageTab(
                type = PageType.CARRIER,
                title = resourceProvider.getString(R.string.woo_shipping_labels_package_creation_tab_carrier)
            ),
            PageTab(
                type = PageType.SAVED,
                title = resourceProvider.getString(R.string.woo_shipping_labels_package_creation_tab_saved)
            )
        )

    init {
        loadData()
    }

    private fun loadData(): Job {
        return launch {
            fetchPredefinedPackages().let { response ->
                _viewState.update { viewState -> viewState.copy(predefinedPackagesState = response) }
            }
        }
    }

    fun onCarrierPackageSelected(selectedPackage: PackageData, isSelected: Boolean) {
        _viewState.update { viewState ->
            val predefinedPackages = viewState.predefinedPackagesData
            predefinedPackages?.carrierPackages
                ?.map { updateCarrierPackagesSelection(it, selectedPackage, isSelected) }
                ?.let {
                    viewState.copy(predefinedPackagesState = predefinedPackages.copy(carrierPackages = it.toMap()))
                } ?: _viewState.value
        }
    }

    fun onSavedPackageSelected(selectedPackage: PackageData, isSelected: Boolean) {
        _viewState.update { viewState ->
            val predefinedPackages = viewState.predefinedPackagesData
            predefinedPackages?.savedPackages
                ?.map { it.copy(isSelected = false) }
                ?.toMutableList()
                ?.safelyUpdate(selectedPackage, selectedPackage.copy(isSelected = isSelected))
                ?.let { viewState.copy(predefinedPackagesState = predefinedPackages.copy(savedPackages = it)) }
                ?: _viewState.value
        }
    }

    fun onRetryClick() {
        triggerEvent(ShowLoadingDialog(true))
        launch {
            loadData().join()
            triggerEvent(ShowLoadingDialog(false))
        }
    }

    fun onAddCarrierPackageClick() {
        _viewState.value.predefinedPackagesData?.carrierPackages
            ?.asSequence()
            ?.flatMap { it.value }
            ?.flatMap { it.packages }
            ?.find { it.isSelected }
            ?.let { triggerEvent(PackageSelected(it)) }
    }

    fun onAddSavedPackageClick() {
        _viewState.value.predefinedPackagesData
            ?.savedPackages
            ?.find { it.isSelected }
            ?.let { triggerEvent(PackageSelected(it)) }
    }

    fun onAddCustomPackageClick(savePackageAsTemplate: Boolean) {
        val customPackage = _viewState.value.customPackageCreationData
        if (savePackageAsTemplate) {
            handleCustomSelectionAsTemplate(customPackage)
        } else {
            triggerEvent(PackageSelected(customPackage.toPackageData(dimensionUnit = storeOptions.dimensionUnit)))
        }
    }

    fun onPackageTypeSpinnerClick() {
        triggerEvent(ShowPackageTypeDialog(_viewState.value.customPackageCreationData.type))
    }

    fun onPackageTypeSelected(type: PackageType) {
        _viewState.update {
            val newPackageData = it.customPackageCreationData.copy(type = type)
            it.copy(customPackageCreationData = newPackageData)
        }
    }

    fun onLengthChange(length: String) {
        _viewState.update {
            val newPackageData = it.customPackageCreationData.copy(length = length)
            it.copy(customPackageCreationData = newPackageData)
        }
    }

    fun onWidthChange(width: String) {
        _viewState.update {
            val newPackageData = it.customPackageCreationData.copy(width = width)
            it.copy(customPackageCreationData = newPackageData)
        }
    }

    fun onHeightChange(height: String) {
        _viewState.update {
            val newPackageData = it.customPackageCreationData.copy(height = height)
            it.copy(customPackageCreationData = newPackageData)
        }
    }

    fun onPackageNameChange(name: String) {
        _viewState.update {
            val newPackageData = it.customPackageCreationData.copy(name = name)
            it.copy(customPackageCreationData = newPackageData)
        }
    }

    fun onWeightChange(weight: String) {
        _viewState.update {
            val newPackageData = it.customPackageCreationData.copy(weight = weight)
            it.copy(customPackageCreationData = newPackageData)
        }
    }

    fun onSavePackageChanged(checked: Boolean) {
        _viewState.update {
            val newPackageData = it.customPackageCreationData.copy(saveAsTemplate = checked)
            it.copy(customPackageCreationData = newPackageData)
        }
    }

    private fun updateCarrierPackagesSelection(
        carrierPackages: Map.Entry<Carrier, List<CarrierPackageGroup>>,
        packageData: PackageData,
        isSelected: Boolean
    ) = carrierPackages.value.map { packageGroup ->
        packageGroup.packages
            .map { it.copy(isSelected = false) }
            .toMutableList()
            .safelyUpdate(packageData, packageData.copy(isSelected = isSelected))
            .let { packageGroup.copy(packages = it) }
    }.let { carrierPackages.key to it }

    private fun MutableList<PackageData>.safelyUpdate(
        originalPackage: PackageData,
        updatedPackage: PackageData
    ) = apply {
        indexOf(originalPackage)
            .takeIf { it != -1 }
            ?.let { set(it, updatedPackage) }
    }

    private fun handleCustomSelectionAsTemplate(
        customPackage: CustomPackageCreationData
    ) {
        triggerEvent(ShowLoadingDialog(true))
        launch {
            selectedSite.getOrNull()
                ?.let { sendCustomPackageToStore(it, customPackage) }
                ?.fold(
                    onSuccess = {
                        triggerEvent(ShowLoadingDialog(false))
                        triggerEvent(
                            PackageSelected(customPackage.toPackageData(dimensionUnit = storeOptions.dimensionUnit))
                        )
                    },
                    onFailure = {
                        triggerEvent(ShowLoadingDialog(false))
                        triggerEvent(ShowTemplateCreationErrorDialog)
                    }
                ) ?: triggerEvent(
                PackageSelected(
                    customPackage.toPackageData(dimensionUnit = storeOptions.dimensionUnit)
                )
            )
        }
    }

    private suspend fun sendCustomPackageToStore(
        site: SiteModel,
        packageData: CustomPackageCreationData
    ): Result<PackageData> {
        val response = packageRepository.createCustomPackage(
            site = site,
            requestData = CustomPackageCreationRequestData(
                name = packageData.name,
                isLetter = packageData.type == PackageType.ENVELOPE,
                innerDimensions = packageData.dimensions,
                boxWeight = packageData.weight?.toDoubleOrNull() ?: 0.0,
                isUserDefined = true,
                maxWeight = 0.0
            ).let { listOf(it) }
        )

        return response.takeIf { it.isError.not() }
            ?.model?.firstOrNull()
            ?.let { PackageData.fromPackageDAO(it) }
            ?.let { Result.success(it) }
            ?: Result.failure(Throwable("Failed to save package"))
    }

    @Parcelize
    data class ViewState(
        val pageTabs: List<PageTab> = emptyList(),
        val customPackageCreationData: CustomPackageCreationData = CustomPackageCreationData.EMPTY,
        val predefinedPackagesState: PredefinedPackagesState = PredefinedPackagesState.Waiting,
    ) : Parcelable {
        val predefinedPackagesData
            get() = (predefinedPackagesState as? PredefinedPackagesState.Data)
    }

    @Parcelize
    sealed class PredefinedPackagesState : Parcelable {
        data object Error : PredefinedPackagesState()
        data object Waiting : PredefinedPackagesState()
        data class Data(
            val storeOptions: StoreOptionsForPackages,
            val savedPackages: List<PackageData>,
            val carrierPackages: Map<Carrier, List<CarrierPackageGroup>>
        ) : PredefinedPackagesState() {
            val hasCarrierSelection: Boolean
                get() = carrierPackages.values.flatten().find { group ->
                    group.packages.find { it.isSelected } != null
                } != null

            val hasSavedSelection: Boolean
                get() = savedPackages.find { it.isSelected } != null
        }
    }

    @Parcelize
    data class PageTab(
        val title: String,
        val type: PageType
    ) : Parcelable

    enum class PageType {
        CUSTOM,
        CARRIER,
        SAVED
    }

    enum class PackageType(val resourceId: Int) {
        BOX(R.string.woo_shipping_labels_package_creation_box_type),
        ENVELOPE(R.string.woo_shipping_labels_package_creation_envelope_type)
    }

    data class PackageSelected(val packageData: PackageData) : MultiLiveEvent.Event()
    data class ShowPackageTypeDialog(val currentSelection: PackageType) : MultiLiveEvent.Event()
    data class ShowLoadingDialog(val show: Boolean) : MultiLiveEvent.Event()
    object ShowTemplateCreationErrorDialog : MultiLiveEvent.Event()
}
