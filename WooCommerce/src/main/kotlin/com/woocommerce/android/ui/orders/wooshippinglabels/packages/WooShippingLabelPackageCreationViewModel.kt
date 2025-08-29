package com.woocommerce.android.ui.orders.wooshippinglabels.packages

import android.os.Parcelable
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.asLiveData
import androidx.lifecycle.viewModelScope
import com.woocommerce.android.R
import com.woocommerce.android.WooException
import com.woocommerce.android.analytics.AnalyticsEvent
import com.woocommerce.android.analytics.AnalyticsTracker.Companion.KEY_ERROR
import com.woocommerce.android.analytics.AnalyticsTracker.Companion.KEY_STATE
import com.woocommerce.android.analytics.AnalyticsTracker.Companion.VALUE_STARTED
import com.woocommerce.android.analytics.AnalyticsTrackerWrapper
import com.woocommerce.android.tools.SelectedSite
import com.woocommerce.android.ui.orders.wooshippinglabels.models.StoreOptionsModel
import com.woocommerce.android.ui.orders.wooshippinglabels.packages.datasource.FetchShippingPackages
import com.woocommerce.android.ui.orders.wooshippinglabels.packages.datasource.WooShippingLabelPackageRepository
import com.woocommerce.android.ui.orders.wooshippinglabels.packages.networking.CustomPackageCreationRequestData
import com.woocommerce.android.ui.orders.wooshippinglabels.packages.ui.Carrier
import com.woocommerce.android.ui.orders.wooshippinglabels.packages.ui.CarrierPackageGroup
import com.woocommerce.android.ui.orders.wooshippinglabels.packages.ui.CustomPackageCreationData
import com.woocommerce.android.ui.orders.wooshippinglabels.packages.ui.PackageData
import com.woocommerce.android.ui.orders.wooshippinglabels.packages.ui.StoreOptionsForPackages
import com.woocommerce.android.viewmodel.MultiLiveEvent.Event
import com.woocommerce.android.viewmodel.ResourceProvider
import com.woocommerce.android.viewmodel.ScopedViewModel
import com.woocommerce.android.viewmodel.getStateFlow
import com.woocommerce.android.viewmodel.navArgs
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
    private val fetchPackages: FetchShippingPackages,
    private val updateSavedCarrierPackages: UpdateSavedCarrierPackages,
    private val packageRepository: WooShippingLabelPackageRepository,
    private val tracker: AnalyticsTrackerWrapper,
) : ScopedViewModel(savedState) {
    private val navArgs: WooShippingLabelPackageCreationFragmentArgs by savedState.navArgs()

    private val _viewState = savedState.getStateFlow(
        scope = viewModelScope,
        initialValue = ViewState(
            pageTabs = pageTabs,
            storeOptions = navArgs.storeOptions,
        )
    )
    val viewState = _viewState.asLiveData()

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
            tracker.track(AnalyticsEvent.WCS_PACKAGE_SELECTION_STEP, mapOf(KEY_STATE to VALUE_STARTED))
            fetchPackages().let { response ->
                _viewState.update { viewState -> viewState.copy(packagesState = response) }
                if (response is PackagesState.Data) {
                    tracker.track(AnalyticsEvent.WCS_PACKAGE_SELECTION_STEP, mapOf(KEY_STATE to "loading_success"))
                } else if (response is PackagesState.Error) {
                    tracker.track(
                        AnalyticsEvent.WCS_PACKAGE_SELECTION_STEP,
                        mapOf(KEY_STATE to "loading_failed", KEY_ERROR to response.error)
                    )
                }
            }
        }
    }

    fun onCarrierPackageSelected(selectedPackage: PackageData, isSelected: Boolean) {
        _viewState.update { viewState ->
            val packages = viewState.packagesData
            packages?.carrierPackages
                ?.map { updateCarrierPackagesSelection(it, selectedPackage, isSelected) }
                ?.let {
                    viewState.copy(packagesState = packages.copy(carrierPackages = it.toMap()))
                } ?: _viewState.value
        }
    }

    fun onSavedPackageSelected(selectedPackage: PackageData, isSelected: Boolean) {
        _viewState.update { viewState ->
            val packages = viewState.packagesData
            packages?.savedPackages
                ?.map { it.copy(isSelected = false) }
                ?.toMutableList()
                ?.safelyUpdate(selectedPackage, selectedPackage.copy(isSelected = isSelected))
                ?.let { viewState.copy(packagesState = packages.copy(savedPackages = it)) }
                ?: _viewState.value
        }
    }

    fun onSavedPackageRemoved(removedPackage: PackageData) {
        // Update UI
        updateSavedPackageUI(packageData = removedPackage, saved = false)

        // Send to backend
        launch {
            updateSavedCarrierPackages(
                savePackage = false,
                packageId = removedPackage.id,
                isUserDefined = removedPackage.isUserDefined,
            ).let {
                if (it.isSuccess) {
                    tracker.track(AnalyticsEvent.WCS_PACKAGE_SELECTION_STEP, mapOf(KEY_STATE to "removing_success"))
                } else {
                    triggerEvent(Event.ShowSnackbar(R.string.woo_shipping_labels_package_removing_error))

                    // If the removal fails, revert the UI change
                    updateSavedPackageUI(packageData = removedPackage, saved = true)
                    tracker.track(
                        AnalyticsEvent.WCS_PACKAGE_SELECTION_STEP,
                        mapOf(
                            KEY_STATE to "removing_failed",
                            KEY_ERROR to (it.exceptionOrNull() as? WooException)?.error?.type?.name.orEmpty()
                        )
                    )
                }
            }
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
        _viewState.value.packagesData?.carrierPackages
            ?.asSequence()
            ?.flatMap { it.value }
            ?.flatMap { it.packages }
            ?.find { it.isSelected }
            ?.let { triggerEvent(PackageSelected(it)) }

        tracker.track(AnalyticsEvent.WCS_PACKAGE_SELECTION_STEP, mapOf(KEY_STATE to "selected"))
    }

    fun onAddSavedPackageClick() {
        _viewState.value.packagesData
            ?.savedPackages
            ?.find { it.isSelected }
            ?.let { triggerEvent(PackageSelected(it)) }

        tracker.track(AnalyticsEvent.WCS_PACKAGE_SELECTION_STEP, mapOf(KEY_STATE to "selected"))
    }

    fun onAddCustomPackageClick(savePackageAsTemplate: Boolean) {
        val customPackage = _viewState.value.customPackageCreationData
        when {
            customPackage.invalidDimensions -> _viewState.value = _viewState.value.copy(
                customPackageCreationData = _viewState.value.customPackageCreationData.copy(
                    invalidDimensionError = true
                )
            )

            savePackageAsTemplate -> handleCustomSelectionAsTemplate(customPackage)
            else -> {
                triggerEvent(
                    PackageSelected(
                        customPackage.toPackageData(dimensionUnit = _viewState.value.storeOptions.dimensionUnit)
                    )
                )
            }
        }

        tracker.track(AnalyticsEvent.WCS_PACKAGE_SELECTION_STEP, mapOf(KEY_STATE to "selected"))
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
            var newPackageData = it.customPackageCreationData.copy(length = length)
            if (it.customPackageCreationData.invalidDimensionError) {
                newPackageData = newPackageData.copyWithUpdatedError()
            }
            it.copy(customPackageCreationData = newPackageData)
        }
    }

    fun onWidthChange(width: String) {
        _viewState.update {
            var newPackageData = it.customPackageCreationData.copy(width = width)
            if (it.customPackageCreationData.invalidDimensionError) {
                newPackageData = newPackageData.copyWithUpdatedError()
            }
            it.copy(customPackageCreationData = newPackageData)
        }
    }

    fun onHeightChange(height: String) {
        _viewState.update {
            var newPackageData = it.customPackageCreationData.copy(height = height)
            if (it.customPackageCreationData.invalidDimensionError) {
                newPackageData = newPackageData.copyWithUpdatedError()
            }
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
                            PackageSelected(
                                customPackage.toPackageData(
                                    dimensionUnit = _viewState.value.storeOptions.dimensionUnit
                                )
                            )
                        )
                        tracker.track(AnalyticsEvent.WCS_PACKAGE_SELECTION_STEP, mapOf(KEY_STATE to "saving_success"))
                    },
                    onFailure = {
                        triggerEvent(ShowLoadingDialog(false))
                        triggerEvent(ShowTemplateCreationErrorDialog)
                        tracker.track(
                            AnalyticsEvent.WCS_PACKAGE_SELECTION_STEP,
                            mapOf(KEY_STATE to "saving_failed", KEY_ERROR to it.message.orEmpty())
                        )
                    }
                ) ?: triggerEvent(
                PackageSelected(
                    customPackage.toPackageData(
                        dimensionUnit = _viewState.value.storeOptions.dimensionUnit
                    )
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
            ?: Result.failure(Throwable(response.error.type.toString()))
    }

    fun onCarrierPackageStarred(packageData: PackageData, isStarred: Boolean) {
        // Update UI
        updateSavedPackageUI(packageData = packageData, saved = isStarred)

        // Send to backend
        launch {
            updateSavedCarrierPackages(
                savePackage = isStarred,
                packageId = packageData.id,
                isUserDefined = packageData.isUserDefined,
                carrierPackages = _viewState.value.packagesData?.carrierPackages ?: emptyMap()
            ).let {
                if (it.isSuccess) {
                    tracker.track(
                        AnalyticsEvent.WCS_PACKAGE_SELECTION_STEP,
                        mapOf(KEY_STATE to if (isStarred) "saving_success" else "removing_success")
                    )
                } else {
                    val errorMessage = if (isStarred) {
                        R.string.woo_shipping_labels_package_saving_error
                    } else {
                        R.string.woo_shipping_labels_package_removing_error
                    }
                    triggerEvent(Event.ShowSnackbar(errorMessage))

                    // If failed, revert the UI change
                    updateSavedPackageUI(packageData = packageData, saved = !isStarred)
                    tracker.track(
                        AnalyticsEvent.WCS_PACKAGE_SELECTION_STEP,
                        mapOf(
                            KEY_STATE to if (isStarred) "saving_failed" else "removing_failed",
                            KEY_ERROR to (it.exceptionOrNull() as? WooException)?.error?.type?.name.orEmpty()
                        )
                    )
                }
            }
        }
    }

    private fun updateSavedPackageUI(packageData: PackageData, saved: Boolean) {
        _viewState.update { viewState ->
            val packages = viewState.packagesData ?: return
            val updatedCarrierPackages = starCarrierPackage(
                carrierPackages = packages.carrierPackages,
                star = saved,
                packageId = packageData.id
            )
            val updatedSavedPackages = if (saved) {
                packages.savedPackages + packageData
            } else {
                packages.savedPackages.filterNot { it.id == packageData.id }
            }
            viewState.copy(
                packagesState = packages.copy(
                    carrierPackages = updatedCarrierPackages,
                    savedPackages = updatedSavedPackages
                )
            )
        }
    }

    /**
     * Updates the starred state of a specific carrier package.
     *
     * This function iterates through the provided carrier packages and updates the `isStarred`
     * property of the package that matches the given `packageId`.
     *
     * @param carrierPackages A map of carrier packages to be updated.
     * @param star The new starred state to be set for the package.
     * @param packageId The ID of the package to be updated.
     * @return A new map with the updated package information.
     */
    private fun starCarrierPackage(
        carrierPackages: Map<Carrier, List<CarrierPackageGroup>>,
        star: Boolean,
        packageId: String
    ) = carrierPackages.mapValues { (_, packageGroupList) ->
        packageGroupList.map { packageGroup ->
            val updatedPackages = packageGroup.packages.map {
                if (it.id == packageId) it.copy(isStarred = star) else it
            }
            packageGroup.copy(packages = updatedPackages)
        }
    }

    @Parcelize
    data class ViewState(
        val pageTabs: List<PageTab> = emptyList(),
        val customPackageCreationData: CustomPackageCreationData = CustomPackageCreationData.EMPTY,
        val packagesState: PackagesState = PackagesState.Waiting,
        val storeOptions: StoreOptionsModel = StoreOptionsModel.EMPTY
    ) : Parcelable {
        val packagesData
            get() = (packagesState as? PackagesState.Data)
    }

    @Parcelize
    sealed class PackagesState : Parcelable {
        data class Error(val error: String = "") : PackagesState()
        data object Waiting : PackagesState()
        data class Data(
            val storeOptions: StoreOptionsForPackages,
            val savedPackages: List<PackageData>,
            val carrierPackages: Map<Carrier, List<CarrierPackageGroup>>
        ) : PackagesState() {
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

    data class PackageSelected(val packageData: PackageData) : Event()
    data class ShowPackageTypeDialog(val currentSelection: PackageType) : Event()
    data class ShowLoadingDialog(val show: Boolean) : Event()
    object ShowTemplateCreationErrorDialog : Event()
}
